package de.thm.mni.backend.mail

import de.thm.mni.backend.common.dto.PageResponse
import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.error.MailDeliveryException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailRequest
import de.thm.mni.backend.mail.dto.MailUpdateRequest
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.mail.dto.MailListItemDTO
import de.thm.mni.backend.mail.dto.toMailCreate
import de.thm.mni.backend.mail.dto.toMailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.user.CurrentUserService
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import de.thm.mni.backend.openapi.DefaultApiResponses
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID


@RestController
@RequestMapping("/api/mails", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Mail", description = "Manage support mails, drafts, ticket replies and SMTP delivery.")
@DefaultApiResponses
class MailController(private val mailService: MailService,
                     private val currentUserService: CurrentUserService,
                     private val mailRecordService: MailRecordService,
                     private val mailMapper: MailMapper
    ) {

    @Operation(operationId = "getDraftMails", summary = "List drafts", description = "Returns draft and failed mails authored by the current user.")
    @ApiResponse(responseCode = "200", description = "Drafts returned")
    @GetMapping("/drafts")
    fun getCreatedMails(@AuthenticationPrincipal jwt: Jwt): List<MailDTO> {
        val user = currentUserService.getOrProvision(jwt)
        val userMails =  mailService.getAllCreatedUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    @Operation(operationId = "getSentMails", summary = "List sent mails", description = "Returns successfully sent mails authored by the current user.")
    @ApiResponse(responseCode = "200", description = "Sent mails returned")
    @GetMapping("/sent")
    fun getSentMails(@AuthenticationPrincipal jwt: Jwt): List<MailDTO> {
        val user = currentUserService.getOrProvision(jwt)
        val userMails =  mailService.getAllSentUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }

    }

    @Operation(operationId = "createMailDraft", summary = "Create a draft", description = "Creates a mail draft with optional attachments.")
    @ApiResponse(responseCode = "201", description = "Draft created")
    @ApiResponse(responseCode = "400", description = "Invalid mail or recipient data")
    @ApiResponse(responseCode = "404", description = "Recipient not found")
    @ApiResponse(responseCode = "413", description = "Attachment size limit exceeded")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createMail(@Valid @RequestPart("data") data: MailRequest,
                   @RequestPart("attachments", required = false) attachments: List<MultipartFile>?,
                   @AuthenticationPrincipal jwt: Jwt): MailDTO
    {
        val user = currentUserService.getOrProvision(jwt)

        val createdMail = mailService.createMail(data.toMailCreate(), user, attachments.orEmpty())
        return mailMapper.toDTO(user, createdMail)
    }

    @Operation(operationId = "getIncomingMails", summary = "List incoming mails", description = "Returns a page of imported support mails visible to the current user.")
    @ApiResponse(responseCode = "200", description = "Incoming mail page returned")
    @GetMapping("/incoming")
    fun getIncomingMailsForUser(
        @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Zero-based page number.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size between 1 and 100.", example = "25")
        @RequestParam(defaultValue = "25") size: Int,
    ): PageResponse<MailListItemDTO> {
        val userId = currentUserService.getOrProvision(jwt).id!!

        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val userMails = mailRecordService.getIncomingMailsForUser(userId, PageRequest.of(safePage, safeSize))

        return PageResponse(
            content = userMails.content.map { mail -> mailMapper.toListItemDTO(mail) },
            page = userMails.number,
            size = userMails.size,
            totalElements = userMails.totalElements,
            totalPages = userMails.totalPages,
            hasNext = userMails.hasNext()
        )
    }

    @Operation(operationId = "getMail", summary = "Get a mail", description = "Returns a mail when it is visible to the current user.")
    @ApiResponse(responseCode = "200", description = "Mail returned")
    @ApiResponse(responseCode = "404", description = "Mail not found or not accessible")
    @GetMapping("/{mailId}")
    fun getMailById(@PathVariable mailId: UUID, @AuthenticationPrincipal jwt: Jwt): MailDTO {
        val user = currentUserService.getOrProvision(jwt)
        val mail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)

        // Check if the user is either the sender or a recipient of the mail
        if (mail.source != MailSource.EXTERN && records.none { it.user!!.id == user.id } && mail.sender!!.id != user.id) {
            throw ResourceNotFoundException("Mail not found")
        }
        return mailMapper.toDTO(user, mail)
    }

    @Operation(
        operationId = "createMailReplyDraft",
        summary = "Create a reply draft for an incoming support mail",
        description = "Creates a draft addressed to the external sender and assigns or reuses the mail's ticket number.",
    )
    @ApiResponse(responseCode = "201", description = "Reply draft created")
    @ApiResponse(responseCode = "400", description = "The mail is not an incoming external mail")
    @ApiResponse(responseCode = "404", description = "The mail is unavailable to the current user")
    @PostMapping("/{mailId}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    fun createReplyDraft(
        @PathVariable mailId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): MailDTO {
        val user = currentUserService.getOrProvision(jwt)
        val incomingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        ensureMailAccess(incomingMail, user.id!!)
        val replyDraft = mailService.createReplyDraft(incomingMail, user)
        return mailMapper.toDTO(user, replyDraft)
    }

    @Operation(operationId = "updateMailDraft", summary = "Update a draft", description = "Updates a draft or failed mail, retaining selected existing attachments and adding optional new files.")
    @ApiResponse(responseCode = "200", description = "Draft updated")
    @ApiResponse(responseCode = "400", description = "Invalid data or mail cannot be modified")
    @ApiResponse(responseCode = "404", description = "Mail or recipient not found")
    @ApiResponse(responseCode = "413", description = "Attachment size limit exceeded")
    @PutMapping("/{mailId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateMail(@PathVariable mailId: UUID,
                   @Valid @RequestPart("data") mail: MailUpdateRequest,
                   @RequestPart("attachments", required = false) attachments: List<MultipartFile>?,
                   @AuthenticationPrincipal jwt: Jwt): MailDTO
    {
        val user = currentUserService.getOrProvision(jwt)
        val userId = user.id!!
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")

        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }

        if (existingMail.status == MailStatus.SENT) {
            throw ResourceCannotBeModifiedException("Cannot update a sent mail")
        }

        val updatedMail = mailService.updateMail(mailId, mail.toMailUpdate(), attachments.orEmpty())
        return mailMapper.toDTO(user, updatedMail)
    }

    @Operation(operationId = "deleteMailDraft", summary = "Delete a mail", description = "Deletes a mail authored by the current user and removes its attachments after commit.")
    @ApiResponse(responseCode = "204", description = "Mail deleted")
    @ApiResponse(responseCode = "404", description = "Mail not found or not accessible")
    @DeleteMapping("/{mailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMail(@PathVariable mailId: UUID, @AuthenticationPrincipal jwt: Jwt) {
        val userId = currentUserService.getOrProvision(jwt).id!!
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
        mailService.deleteMail(existingMail)
    }

    @Operation(operationId = "sendMail", summary = "Send an existing mail", description = "Sends a draft or retries a failed mail through the configured SMTP server.")
    @ApiResponse(responseCode = "200", description = "Mail sent")
    @ApiResponse(responseCode = "400", description = "Mail cannot be sent in its current state")
    @ApiResponse(responseCode = "404", description = "Mail not found or not accessible")
    @ApiResponse(responseCode = "502", description = "SMTP delivery failed")
    @PostMapping("/send/{mailId}")
    fun sendMail(@PathVariable mailId: UUID, @AuthenticationPrincipal jwt: Jwt): MailDTO {
        val user = currentUserService.getOrProvision(jwt)
        val userId = user.id!!
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
        val sentMail = mailService.sendMail(existingMail)
        ensureDeliverySucceeded(sentMail)
        return mailMapper.toDTO(user, sentMail)
    }

    @Operation(operationId = "createAndSendMail", summary = "Create and send a mail", description = "Creates a new mail with optional attachments and immediately attempts SMTP delivery.")
    @ApiResponse(responseCode = "201", description = "Mail created and sent")
    @ApiResponse(responseCode = "400", description = "Invalid mail or recipient data")
    @ApiResponse(responseCode = "404", description = "Recipient not found")
    @ApiResponse(responseCode = "413", description = "Attachment size limit exceeded")
    @ApiResponse(responseCode = "502", description = "SMTP delivery failed")
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAndSendMail(@Valid @RequestPart("data") data: MailRequest,
                          @RequestPart("attachments", required = false) attachments: List<MultipartFile>?,
                          @AuthenticationPrincipal jwt: Jwt): MailDTO {
        val user = currentUserService.getOrProvision(jwt)

        val createdMail = mailService.createAndSendMail(data.toMailCreate(), user, attachments.orEmpty())
        ensureDeliverySucceeded(createdMail)
        return mailMapper.toDTO(user, createdMail)
    }

    private fun ensureMailAccess(mail: Mail, userId: UUID) {
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        if (mail.source != MailSource.EXTERN && records.none { it.user?.id == userId } && mail.sender?.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
    }

    private fun ensureDeliverySucceeded(mail: Mail) {
        if (mail.status == MailStatus.ERROR) {
            throw MailDeliveryException("The mail could not be delivered by the configured SMTP server")
        }
    }

}
