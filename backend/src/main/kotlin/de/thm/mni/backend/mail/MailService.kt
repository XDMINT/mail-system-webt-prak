package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailCreate
import de.thm.mni.backend.mail.dto.ImportedAttachment
import de.thm.mni.backend.mail.dto.MailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.mail_record.dto.CreateMailRecord
import de.thm.mni.backend.smtp.SMTPService
import de.thm.mni.backend.storage.FileStorageService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserService
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID


@Service
class MailService(
    private val mailRepository: MailRepository,
    private val userService: UserService,
    private val smtpService: SMTPService,
    private val fileStorageService: FileStorageService,
    private val mailRecordService: MailRecordService,
    private val passwordEncoder: PasswordEncoder,
){
    fun getMailById(id: UUID): Mail? {
        return mailRepository.findById(id).orElse(null)
    }

    fun getMailByExternalMessageId(externalMessageId: String): Mail? {
        return mailRepository.findByExternalMessageId(externalMessageId)
    }

    fun getAllCreatedUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user).toList().filter { mail -> mail.status == MailStatus.DRAFT }
    }
    fun getAllSentUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user).toList().filter { mail -> mail.status == MailStatus.SENT }
    }

    @Transactional
    fun deleteMail(mail: Mail) {
        mail.attachments.map { file -> fileStorageService.deleteFile(file.path) }
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        records.forEach { record -> mailRecordService.deleteMailRecord(record.id!!) }
        mailRepository.delete(mail)
    }

    @Transactional
    fun sendMail(mail: Mail): Mail {
        applyTrackingCodeIfNeeded(mail)
        val success = smtpService.sendEmail(mail)
        if(success) {
            mail.status = MailStatus.SENT
        }else{
            mail.status = MailStatus.ERROR
        }
        return mailRepository.save(mail)
    }

    @Transactional
    fun createMail(mail: MailCreate, sender: User,  attachments: List<MultipartFile>): Mail {
        val storedAttachments = attachments.mapNotNull { file -> fileStorageService.saveFile(file) }.toMutableList()

        val mailEntity = Mail(
            sender = sender,
            subject = mail.subject,
            content = mail.content,
            attachments = mutableListOf()
        )

        this.connectAttachmentsToMail(mailEntity, storedAttachments)
        val createdMail = mailRepository.save(mailEntity)

        this.createMailRecordsFromIds(createdMail, mail.toIds, mail.ccIds, mail.bccIds, mail.replyToIds)
        return createdMail
    }

    @Transactional
    fun createImportedMail(
        senderEmail: String,
        subject: String,
        content: String,
        attachments: List<ImportedAttachment>,
        externalMessageId: String,
        receivedAt: java.time.LocalDateTime?
    ): Mail {
        mailRepository.findByExternalMessageId(externalMessageId)?.let { return it }

        val externalSender = ensureExternalSenderUser(senderEmail)
        val mailEntity = Mail(
            sender = externalSender,
            subject = subject,
            content = content,
            attachments = mutableListOf()
        )
        mailEntity.status = MailStatus.SENT
        mailEntity.source = MailSource.EXTERN
        mailEntity.externalSenderEmail = senderEmail
        mailEntity.externalMessageId = externalMessageId
        mailEntity.sentAt = receivedAt
        applyTrackingCodeToImportedMail(mailEntity)

        val storedAttachments = attachments.mapNotNull { file ->
            fileStorageService.saveFile(file.fileName, file.contentType, file.bytes)
        }.toMutableList()

        connectAttachmentsToMail(mailEntity, storedAttachments)
        val createdMail = mailRepository.save(mailEntity)

        userService.getInternalUsers()
            .forEach { receiver ->
                mailRecordService.createMailRecord(CreateMailRecord(
                    mail = createdMail,
                    receiver = receiver,
                    mailType = MailType.TO
                ))
            }

        return createdMail
    }

    @Transactional
    fun createAndSendMail(mail: MailCreate, sender: User, attachments: List<MultipartFile>): Mail {
        val createdMail = this.createMail(mail, sender, attachments)
        return this.sendMail(createdMail)
    }


    @Transactional
    fun updateMail(id: UUID, mail: MailUpdate, attachments: List<MultipartFile>): Mail {
        val existingMail = this.getMailById(id)!!

        existingMail.subject = mail.subject
        existingMail.content = mail.content

        existingMail.attachments.map { file -> fileStorageService.deleteFile(file.path) }
        existingMail.attachments.clear()

        val storedAttachments = attachments.mapNotNull { file -> fileStorageService.saveFile(file) }.toMutableList()
        this.connectAttachmentsToMail(existingMail, storedAttachments)

        val updatedMail = mailRepository.save(existingMail)

        val records = mailRecordService.getMailRecordByMailId(updatedMail.id!!)
        records.forEach { record ->
            mailRecordService.deleteMailRecord(record.id!!)
        }
        this.createMailRecordsFromIds(updatedMail, mail.toIds, mail.ccIds, mail.bccIds, mail.replyToIds)

        return updatedMail
    }

    private fun connectAttachmentsToMail(mail: Mail, attachments:  MutableList<AttachmentDTO>) {
        attachments.forEach { att ->
            val attachment = Attachment()
            attachment.fileName = att.fileName
            attachment.mimeType = att.mimeType
            attachment.size = att.size
            attachment.path = att.path
            mail.addAttachment(attachment)
        }
    }

    private fun ensureExternalSenderUser(email: String): User {
        return userService.getUserByEmail(email) ?: userService.createUser(
            User(
                firstName = "External",
                lastName = "Sender",
                email = email,
                password = passwordEncoder.encode(UUID.randomUUID().toString()).toString(),
                externalContact = true
            )
        )
    }

    private fun applyTrackingCodeToImportedMail(mail: Mail) {
        val trackingCode = extractTrackingCode(mail.subject) ?: generateTrackingCode()
        mail.trackingCode = trackingCode

        if (!hasTrackingPrefix(mail.subject)) {
            mail.subject = "[$trackingCode] ${mail.subject}"
        }
    }

    private fun applyTrackingCodeIfNeeded(mail: Mail) {
        if (mail.trackingCode == null) {
            extractTrackingCode(mail.subject)?.let { existing ->
                mail.trackingCode = existing
            }
        }

        val records = mail.id?.let { mailRecordService.getMailRecordByMailId(it) }.orEmpty()
        val isReply = records.any { it.type == MailType.REPLY_TO }
        if (!isReply) {
            return
        }

        val trackingCode = mail.trackingCode ?: generateTrackingCode().also { mail.trackingCode = it }
        if (!hasTrackingPrefix(mail.subject)) {
            mail.subject = "[$trackingCode] ${mail.subject}"
        }
    }

    private fun hasTrackingPrefix(subject: String): Boolean {
        return TRACKING_PREFIX_REGEX.containsMatchIn(subject)
    }

    private fun extractTrackingCode(subject: String): String? {
        return TRACKING_PREFIX_REGEX.find(subject)?.groupValues?.getOrNull(1)
    }

    private fun generateTrackingCode(): String {
        return "TICKET-${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    private companion object {
        private val TRACKING_PREFIX_REGEX = Regex("^\\[(TICKET-[A-Z0-9]{8})\\]\\s*")
    }

    private fun createMailRecordsFromIds(
        mail: Mail,
        toIds: List<UUID>,
        ccIds: List<UUID>,
        bccIds: List<UUID>,
        replyToIds: List<UUID>)
    {
        toIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.TO
        ))}

        ccIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.CC
        ))}

        bccIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.BCC
        ))}

        replyToIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.REPLY_TO
        ))}
    }

}
