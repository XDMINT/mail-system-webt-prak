package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailCreate
import de.thm.mni.backend.mail.dto.ImportedAttachment
import de.thm.mni.backend.mail.dto.MailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.mail_record.dto.CreateMailRecord
import de.thm.mni.backend.smtp.OutboundMailGateway
import de.thm.mni.backend.storage.FileStorageService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.util.UUID


@Service
class MailService(
    private val mailRepository: MailRepository,
    private val userService: UserService,
    private val outboundMailGateway: OutboundMailGateway,
    private val fileStorageService: FileStorageService,
    private val mailRecordService: MailRecordService,
){
    fun getMailById(id: UUID): Mail? {
        return mailRepository.findById(id).orElse(null)
    }

    fun getMailByExternalMessageId(externalMessageId: String): Mail? {
        return mailRepository.findByExternalMessageId(externalMessageId)
    }

    fun getAllCreatedUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user).toList().filter { mail -> mail.status != MailStatus.SENT }
    }
    fun getAllSentUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user).toList().filter { mail -> mail.status == MailStatus.SENT }
    }

    @Transactional
    fun deleteMail(mail: Mail) {
        deleteFilesAfterCommit(mail.attachments.map { file -> file.path })
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        records.forEach { record -> mailRecordService.deleteMailRecord(record.id!!) }
        mailRepository.delete(mail)
    }

    @Transactional
    fun sendMail(mail: Mail): Mail {
        if (mail.status !in setOf(MailStatus.DRAFT, MailStatus.ERROR)) {
            throw ResourceCannotBeModifiedException("Only draft or failed mails can be sent")
        }

        val success = outboundMailGateway.send(mail)
        if(success) {
            mail.status = MailStatus.SENT
        }else{
            mail.status = MailStatus.ERROR
        }
        return mailRepository.save(mail)
    }

    @Transactional
    fun createMail(mail: MailCreate, sender: User,  attachments: List<MultipartFile>): Mail {
        validateRecipientRoles(mail.toIds, mail.ccIds, mail.bccIds)
        val storedAttachments = storeUploadedFiles(attachments)

        val mailEntity = Mail(
            sender = sender,
            subject = mail.subject,
            content = mail.content,
            attachments = mutableListOf()
        )

        this.connectAttachmentsToMail(mailEntity, storedAttachments)
        val createdMail = mailRepository.save(mailEntity)

        this.createMailRecordsFromIds(createdMail, mail.toIds, mail.ccIds, mail.bccIds)
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
        mailEntity.trackingCode = extractTrackingCode(subject)

        val storedAttachments = storeImportedFiles(attachments)

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
    fun createReplyDraft(incomingMail: Mail, sender: User): Mail {
        if (incomingMail.source != MailSource.EXTERN) {
            throw ResourceCannotBeModifiedException("Only incoming external mails can be answered")
        }

        val recipient = incomingMail.sender
            ?: incomingMail.externalSenderEmail?.let(::ensureExternalSenderUser)
            ?: throw ResourceCannotBeModifiedException("Incoming mail has no external sender")

        val trackingCode = incomingMail.trackingCode
            ?: extractTrackingCode(incomingMail.subject)
            ?: generateTrackingCode()
        incomingMail.trackingCode = trackingCode
        mailRepository.save(incomingMail)

        val reply = Mail(
            sender = sender,
            subject = createReplySubject(incomingMail.subject, trackingCode),
            content = "",
            attachments = mutableListOf(),
        )
        reply.trackingCode = trackingCode
        reply.inReplyToMail = incomingMail

        val createdReply = mailRepository.save(reply)
        mailRecordService.createMailRecord(
            CreateMailRecord(
                mail = createdReply,
                receiver = recipient,
                mailType = MailType.TO,
            )
        )
        return createdReply
    }

    @Transactional
    fun createAndSendMail(mail: MailCreate, sender: User, attachments: List<MultipartFile>): Mail {
        val createdMail = this.createMail(mail, sender, attachments)
        return this.sendMail(createdMail)
    }


    @Transactional
    fun updateMail(id: UUID, mail: MailUpdate, attachments: List<MultipartFile>): Mail {
        val existingMail = this.getMailById(id)!!
        validateRecipientRoles(mail.toIds, mail.ccIds, mail.bccIds)

        existingMail.subject = mail.subject
        existingMail.content = mail.content

        val replacedAttachmentPaths = existingMail.attachments.map { file -> file.path }
        val storedAttachments = storeUploadedFiles(attachments)
        existingMail.attachments.clear()
        this.connectAttachmentsToMail(existingMail, storedAttachments)

        val updatedMail = mailRepository.save(existingMail)

        val records = mailRecordService.getMailRecordByMailId(updatedMail.id!!)
        records.forEach { record ->
            mailRecordService.deleteMailRecord(record.id!!)
        }
        this.createMailRecordsFromIds(updatedMail, mail.toIds, mail.ccIds, mail.bccIds)
        deleteFilesAfterCommit(replacedAttachmentPaths)

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
                externalContact = true
            )
        )
    }

    private fun createReplySubject(subject: String, trackingCode: String): String {
        val subjectWithoutTrackingCode = TRACKING_PREFIX_REGEX.replace(subject, "").trim()
        val replySubject = if (subjectWithoutTrackingCode.startsWith("Re:", ignoreCase = true)) {
            subjectWithoutTrackingCode
        } else {
            "Re: $subjectWithoutTrackingCode"
        }
        val prefix = "[$trackingCode] "
        return prefix + replySubject.take(MAX_SUBJECT_LENGTH - prefix.length)
    }

    private fun extractTrackingCode(subject: String): String? {
        return TRACKING_PREFIX_REGEX.find(subject)?.groupValues?.getOrNull(1)
    }

    private fun generateTrackingCode(): String {
        return "TICKET-${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    private fun storeUploadedFiles(files: List<MultipartFile>): MutableList<AttachmentDTO> {
        val storedFiles = mutableListOf<AttachmentDTO>()
        files.forEach { file ->
            fileStorageService.saveFile(file)?.let { storedFile ->
                storedFiles.add(storedFile)
                deleteFileOnRollback(storedFile.path)
            }
        }
        return storedFiles
    }

    private fun storeImportedFiles(files: List<ImportedAttachment>): MutableList<AttachmentDTO> {
        val storedFiles = mutableListOf<AttachmentDTO>()
        files.forEach { file ->
            val storedFile = fileStorageService.saveFile(file.fileName, file.contentType, file.bytes)
            storedFiles.add(storedFile)
            deleteFileOnRollback(storedFile.path)
        }
        return storedFiles
    }

    private fun deleteFileOnRollback(path: String) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteFileQuietly(path)
                }
            }
        })
    }

    private fun deleteFilesAfterCommit(paths: List<String>) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            paths.forEach(::deleteFileQuietly)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                paths.forEach(::deleteFileQuietly)
            }
        })
    }

    private fun deleteFileQuietly(path: String) {
        runCatching { fileStorageService.deleteFile(path) }
    }

    private fun validateRecipientRoles(toIds: List<UUID>, ccIds: List<UUID>, bccIds: List<UUID>) {
        val recipientIds = toIds + ccIds + bccIds
        if (recipientIds.size != recipientIds.distinct().size) {
            throw ResourceCannotBeModifiedException("A recipient can only occur once across To, CC and BCC")
        }
    }

    private companion object {
        private val TRACKING_PREFIX_REGEX = Regex("^\\[(TICKET-[A-Z0-9]{8})\\]\\s*")
        private const val MAX_SUBJECT_LENGTH = 255
    }

    private fun createMailRecordsFromIds(
        mail: Mail,
        toIds: List<UUID>,
        ccIds: List<UUID>,
        bccIds: List<UUID>)
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

    }

}
