package de.thm.mni.backend.mail.imap

import de.thm.mni.backend.mail.MailService
import de.thm.mni.backend.mail.dto.ImportedAttachment
import jakarta.mail.BodyPart
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.Flags
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeUtility
import jakarta.mail.search.FlagTerm
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Properties
import java.util.UUID

@Service
class MailInboxSyncService(
    private val mailService: MailService,
    private val imapSyncStateRepository: ImapSyncStateRepository,
    @Value("\${mail.imap.host:}") private val host: String,
    @Value("\${mail.imap.port:993}") private val port: Int,
    @Value("\${mail.imap.username:}") private val username: String,
    @Value("\${mail.imap.password:}") private val password: String,
    @Value("\${mail.imap.folder:INBOX}") private val folderName: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${mail.imap.poll-interval-ms:300000}")
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
            folder.open(Folder.READ_ONLY)

            val syncState = imapSyncStateRepository.findByFolderName(folderName) ?: ImapSyncState(folderName)
            val isInitialImport = !syncState.initialImportCompleted
            val messages = if (isInitialImport) {
                folder.messages
            } else {
                folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            }

            var importedCount = 0
            var skippedCount = 0
            var failedCount = 0

            messages.forEach { message ->
                try {
                    val externalMessageId = extractMessageId(message)
                    if (mailService.getMailByExternalMessageId(externalMessageId) != null) {
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
                    importedCount++
                } catch (ex: Exception) {
                    failedCount++
                    logger.warn("Failed to import email message #{}", message.messageNumber, ex)
                }
            }

            syncState.lastSyncAt = LocalDateTime.now()
            if (isInitialImport && failedCount == 0) {
                syncState.initialImportCompleted = true
            }
            imapSyncStateRepository.save(syncState)

            logger.info(
                "IMAP sync completed for folder {} with mode={}, found={}, imported={}, skipped={}, failed={}",
                folderName,
                if (isInitialImport) "initial" else "incremental",
                messages.size,
                importedCount,
                skippedCount,
                failedCount
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
            content.attachments.add(parseAttachment(part))
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
        val fileName = part.fileName?.let { MimeUtility.decodeText(it) } ?: "attachment"
        val contentType = part.contentType.substringBefore(';')
        val bytes = when (val raw = part.content) {
            is ByteArray -> raw
            is String -> raw.toByteArray()
            else -> {
                val buffer = ByteArrayOutputStream()
                part.inputStream.use { input -> input.copyTo(buffer) }
                buffer.toByteArray()
            }
        }

        return ImportedAttachment(
            fileName = fileName,
            contentType = contentType,
            bytes = bytes,
        )
    }

    private fun Date?.toLocalDateTime(): LocalDateTime? = this?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

    private data class ParsedMessage(
        val subject: String,
        val content: String,
        val fromEmail: String,
        val attachments: List<ImportedAttachment>,
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

