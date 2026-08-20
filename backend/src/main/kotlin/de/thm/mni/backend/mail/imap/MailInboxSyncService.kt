package de.thm.mni.backend.mail.imap

import de.thm.mni.backend.attachment.AttachmentPolicy
import de.thm.mni.backend.mail.MailService
import de.thm.mni.backend.mail.dto.ImportedAttachment
import jakarta.mail.BodyPart
import jakarta.mail.Folder
import jakarta.mail.Flags
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeUtility
import jakarta.mail.search.FlagTerm
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Properties
import java.util.UUID

@Service
class MailInboxSyncService(
    private val mailService: MailService,
    private val attachmentPolicy: AttachmentPolicy,
    @Value("\${mail.imap.host:}") private val host: String,
    @Value("\${mail.imap.port:993}") private val port: Int,
    @Value("\${mail.imap.username:}") private val username: String,
    @Value("\${mail.imap.password:}") private val password: String,
    @Value("\${mail.imap.folder:INBOX}") private val folderName: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${mail.imap.poll-interval-ms:5000}")
    @Suppress("TooGenericExceptionCaught") // A failed poll must not terminate future scheduled synchronizations.
    fun synchronizeInbox() {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            return
        }

        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", port.toString())
            put("mail.imaps.ssl.enable", "true")
        }

        var store: Store? = null
        var folder: Folder? = null

        try {
            val session = Session.getInstance(props)
            store = session.getStore("imaps")
            store.connect(host, port, username, password)

            folder = store.getFolder(folderName)
            folder.open(Folder.READ_WRITE)

            val messages = folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))

            val result = importUnseenMessages(messages)

            logger.info(
                "IMAP sync completed for folder {} with foundUnseen={}, imported={}, skipped={}, failed={}",
                folderName,
                messages.size,
                result.imported,
                result.skipped,
                result.failed
            )
        } catch (ex: Exception) {
            logger.warn("IMAP inbox synchronization failed", ex)
        } finally {
            try {
                folder?.close(false)
            } catch (_: Exception) {
            }
            try {
                store?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractMessageId(message: Message): String {
        val rawId = message.getHeader("Message-ID")?.firstOrNull()?.trim()
        if (!rawId.isNullOrBlank()) {
            return rawId.trim('<', '>')
        }

        val fingerprint = buildString {
            append(message.subject ?: "")
            append('|')
            append(message.from?.joinToString(",") { it.toString() } ?: "")
            append('|')
            append(message.sentDate?.time ?: 0L)
            append('|')
            append(message.receivedDate?.time ?: 0L)
            append('|')
            append(message.messageNumber)
        }
        return UUID.nameUUIDFromBytes(fingerprint.toByteArray()).toString()
    }

    private fun parseMessage(message: Part): ParsedMessage {
        val content = ParsedContent()
        var fromEmail = "unknown@unknown.local"
        var subject = "(no subject)"

        if (message is Message) {
            subject = message.subject ?: subject
            fromEmail = message.from?.firstOrNull()?.let { address ->
                (address as? InternetAddress)?.address ?: address.toString()
            } ?: fromEmail
        }

        collectContent(message, content)

        return ParsedMessage(
            subject = subject,
            content = content.toBodyText(),
            fromEmail = fromEmail,
            attachments = content.attachments,
        )
    }

    private fun collectContent(part: Part, content: ParsedContent) {
        if (part is BodyPart && isAttachment(part)) {
            val attachment = parseAttachment(part)
            attachmentPolicy.validateTotalSize(
                content.attachments.sumOf { it.bytes.size.toLong() } + attachment.bytes.size
            )
            content.attachments.add(attachment)
            return
        }

        when {
            part.isMimeType("text/plain") -> {
                content.plainTextParts.add(part.content.toString())
            }
            part.isMimeType("text/html") -> {
                content.htmlTextParts.add(htmlToPlainText(part.content.toString()))
            }
            part.isMimeType("multipart/*") -> {
                val multipart = part.content as Multipart
                for (index in 0 until multipart.count) {
                    collectContent(multipart.getBodyPart(index), content)
                }
            }
            else -> {
                val raw = part.content
                if (raw is String) {
                    content.plainTextParts.add(raw)
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught") // One malformed message must not prevent importing the remaining inbox.
    internal fun importUnseenMessages(messages: Array<Message>): SyncResult {
        var importedCount = 0
        var skippedCount = 0
        var failedCount = 0

        messages.forEach { message ->
            try {
                val externalMessageId = extractMessageId(message)
                if (mailService.getMailByExternalMessageId(externalMessageId) != null) {
                    message.setFlag(Flags.Flag.SEEN, true)
                    skippedCount++
                    return@forEach
                }

                val parsed = parseMessage(message)
                val receivedAt = message.sentDate?.toLocalDateTime() ?: message.receivedDate?.toLocalDateTime()
                mailService.createImportedMail(
                    senderEmail = parsed.fromEmail,
                    subject = parsed.subject,
                    content = parsed.content,
                    attachments = parsed.attachments,
                    externalMessageId = externalMessageId,
                    receivedAt = receivedAt,
                )
                message.setFlag(Flags.Flag.SEEN, true)
                importedCount++
            } catch (ex: Exception) {
                failedCount++
                logger.warn("Failed to import email message #{}", message.messageNumber, ex)
            }
        }

        return SyncResult(importedCount, skippedCount, failedCount)
    }

    private fun isAttachment(part: BodyPart): Boolean {
        return Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) || part.fileName != null
    }

    private fun htmlToPlainText(html: String): String {
        val withBreaks = html
            .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|li|tr|h[1-6]|blockquote)>"), "\n")
            .replace(Regex("(?i)</td>"), "\t")
            .replace(Regex("(?s)<[^>]+>"), " ")

        return normalizeBodyText(HtmlUtils.htmlUnescape(withBreaks).replace('\u00A0', ' '))
    }

    private fun normalizeBodyText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .map { line -> line.trim().replace(Regex("[ \\t]{2,}"), " ") }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun parseAttachment(part: BodyPart): ImportedAttachment {
        if (part.size >= 0) attachmentPolicy.validateFileSize(part.size.toLong())
        val fileName = attachmentPolicy.normalizeFileName(part.fileName?.let { MimeUtility.decodeText(it) })
        val contentType = attachmentPolicy.normalizeContentType(part.contentType)
        val bytes = part.inputStream.use(attachmentPolicy::readLimited)

        return ImportedAttachment(
            fileName = fileName,
            contentType = contentType,
            bytes = bytes,
        )
    }

    private fun Date?.toLocalDateTime(): LocalDateTime? =
        this?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

    private data class ParsedMessage(
        val subject: String,
        val content: String,
        val fromEmail: String,
        val attachments: List<ImportedAttachment>,
    )

    internal data class SyncResult(
        val imported: Int,
        val skipped: Int,
        val failed: Int,
    )

    private data class ParsedContent(
        val plainTextParts: MutableList<String> = mutableListOf(),
        val htmlTextParts: MutableList<String> = mutableListOf(),
        val attachments: MutableList<ImportedAttachment> = mutableListOf(),
    ) {
        fun toBodyText(): String {
            val parts = if (plainTextParts.any { it.isNotBlank() }) {
                plainTextParts
            } else {
                htmlTextParts
            }

            return parts.joinToString("\n\n").trim()
        }
    }
}

