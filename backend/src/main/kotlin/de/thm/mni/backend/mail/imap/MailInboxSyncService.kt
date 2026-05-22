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
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Properties
import java.util.UUID

@Service
class MailInboxSyncService(
    private val mailService: MailService,
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
            folder.open(Folder.READ_WRITE)

            val unseenMessages = folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            unseenMessages.forEach { message ->
                try {
                    val externalMessageId = extractMessageId(message)
                    if (mailService.getMailByExternalMessageId(externalMessageId) != null) {
                        message.setFlag(Flags.Flag.SEEN, true)
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
                } catch (ex: Exception) {
                    logger.warn("Failed to import email message #{}", message.messageNumber, ex)
                }
            }
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
        val attachments = mutableListOf<ImportedAttachment>()
        val content = StringBuilder()
        var fromEmail = "unknown@unknown.local"
        var subject = "(no subject)"

        if (message is Message) {
            subject = message.subject ?: subject
            fromEmail = message.from?.firstOrNull()?.let { address ->
                (address as? InternetAddress)?.address ?: address.toString()
            } ?: fromEmail
        }

        when {
            message.isMimeType("text/plain") -> {
                content.append(message.content.toString())
            }
            message.isMimeType("text/html") -> {
                content.append(message.content.toString())
            }
            message.isMimeType("multipart/*") -> {
                val multipart = message.content as Multipart
                for (index in 0 until multipart.count) {
                    val part = multipart.getBodyPart(index)
                    if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) || part.fileName != null) {
                        attachments.add(parseAttachment(part))
                    } else if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                        content.append(part.content.toString())
                    } else if (part.content is Multipart) {
                        val nested = parseMessage(part)
                        content.append(nested.content)
                        attachments.addAll(nested.attachments)
                        if (fromEmail == "unknown@unknown.local") {
                            fromEmail = nested.fromEmail
                        }
                    }
                }
            }
            else -> content.append(message.content?.toString() ?: "")
        }

        return ParsedMessage(
            subject = subject,
            content = content.toString().trim(),
            fromEmail = fromEmail,
            attachments = attachments,
        )
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
}

