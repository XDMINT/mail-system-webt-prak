package de.thm.mni.backend.mail

import de.thm.mni.backend.common.dto.PageResponse
import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.openapi.BadRequestApiError
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.openapi.NotFoundApiError
import de.thm.mni.backend.mail.dto.MailRequest
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.mail.dto.MailListItemDTO
import de.thm.mni.backend.mail.dto.toMailCreate
import de.thm.mni.backend.mail.dto.toMailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID


@RestController
@RequestMapping("/api/mails", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
    name = "Mails",
    description = "Create drafts, send mails, list folders, retrieve incoming mail, and manage mail content."
)
@SecurityRequirement(name = "bearerAuth")
@DefaultApiErrors
class MailController(private val mailService: MailService,
                     private val userService: UserService,
                     private val mailRecordService: MailRecordService,
                     private val mailMapper: MailMapper
    ) {

    @GetMapping("/drafts")
    @Operation(
        operationId = "listDraftMails",
        summary = "List draft mails",
        description = "Returns all draft mails created by the authenticated user."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Draft mails returned successfully."
    )
    @NotFoundApiError
    fun getCreatedMails(@Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): List<MailDTO> {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")
        val userMails =  mailService.getAllCreatedUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    @GetMapping("/sent")
    @Operation(
        operationId = "listSentMails",
        summary = "List sent mails",
        description = "Returns all sent mails created by the authenticated user."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Sent mails returned successfully."
    )
    @NotFoundApiError
    fun getSentMails(@Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): List<MailDTO> {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")
        val userMails =  mailService.getAllSentUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }

    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        operationId = "createDraftMail",
        summary = "Create a draft mail",
        description = "Creates a draft for the authenticated user. Recipient ids are supplied in the JSON `data` part; files are supplied as `attachments` parts."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Draft mail created successfully."
    )
    @BadRequestApiError
    @NotFoundApiError
    fun createMail(@Valid @RequestPart("data") data: MailRequest,
                   @RequestPart("attachments") attachments: List<MultipartFile>,
                   @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): MailDTO
    {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")

        val createdMail = mailService.createMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }

    @GetMapping("/incoming")
    @Operation(
        operationId = "listIncomingMails",
        summary = "List incoming mails",
        description = "Returns a paged list of mails visible to the authenticated user as recipient. Page numbers below 0 are treated as 0; sizes are clamped to 1..100."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Incoming mails returned successfully."
    )
    @NotFoundApiError
    fun getIncomingMailsForUser(
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails,
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Requested page size. The server clamps it to 1..100.", example = "25")
        @RequestParam(defaultValue = "25") size: Int,
    ): PageResponse<MailListItemDTO> {
        val userId = UUID.fromString(user.username)
        userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")

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

    @GetMapping("/{mailId}")
    @Operation(
        operationId = "getMailById",
        summary = "Get a mail",
        description = "Returns a full mail if the authenticated user is the sender or a recipient. BCC recipients are only visible to the sender and matching BCC recipient."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Mail returned successfully."
    )
    @NotFoundApiError
    fun getMailById(
        @Parameter(description = "Mail id.", example = "6f1f9368-d279-4a6e-993f-f0618767eeb8")
        @PathVariable mailId: UUID,
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails
    ): MailDTO {
        val user = userService.getUserById(UUID.fromString(user.username)) ?: throw ResourceNotFoundException("User not found")
        val mail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)

        // Check if the user is either the sender or a recipient of the mail
        if (records.none { it.user!!.id == user.id } && mail.sender!!.id != user.id) {
            throw ResourceNotFoundException("Mail not found")
        }
        return mailMapper.toDTO(user, mail)
    }

    @PutMapping("/{mailId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        operationId = "updateDraftMail",
        summary = "Update a draft mail",
        description = "Replaces a draft mail's content, recipients, and attachments. Only the sender can update the mail, and sent mails cannot be modified."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Draft mail updated successfully."
    )
    @BadRequestApiError
    @NotFoundApiError
    fun updateMail(@Parameter(description = "Mail id.", example = "6f1f9368-d279-4a6e-993f-f0618767eeb8")
                   @PathVariable mailId: UUID,
                   @Valid @RequestPart("data") mail: MailRequest,
                   @RequestPart("attachments") attachments: List<MultipartFile>,
                   @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): MailDTO
    {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")

        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }

        if (existingMail.status == MailStatus.SENT) {
            throw ResourceCannotBeModifiedException("Cannot update a sent mail")
        }

        val updatedMail = mailService.updateMail(mailId, mail.toMailUpdate(), attachments)
        return mailMapper.toDTO(user, updatedMail)
    }

    @DeleteMapping("/{mailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        operationId = "deleteDraftMail",
        summary = "Delete a mail",
        description = "Deletes a mail owned by the authenticated user and removes its stored attachments."
    )
    @ApiResponse(responseCode = "204", description = "Mail deleted successfully.")
    @NotFoundApiError
    fun deleteMail(@Parameter(description = "Mail id.", example = "6f1f9368-d279-4a6e-993f-f0618767eeb8")
                   @PathVariable mailId: UUID,
                   @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails) {
        val userId = UUID.fromString(user.username)
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
        mailService.deleteMail(existingMail)
    }

    @PostMapping("/send/{mailId}")
    @Operation(
        operationId = "sendDraftMail",
        summary = "Send an existing draft",
        description = "Sends an existing draft owned by the authenticated user through the configured SMTP server. The mail status becomes `SENT` on success or `ERROR` if SMTP delivery fails."
    )
    @ApiResponse(responseCode = "200", description = "Send attempt finished.")
    @NotFoundApiError
    fun sendMail(@Parameter(description = "Mail id.", example = "6f1f9368-d279-4a6e-993f-f0618767eeb8")
                 @PathVariable mailId: UUID,
                 @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails) {
        val userId = UUID.fromString(user.username)
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
        mailService.sendMail(existingMail)
    }

    @PostMapping("/send", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        operationId = "createAndSendMail",
        summary = "Create and send a mail",
        description = "Creates a mail from multipart form data and immediately sends it through the configured SMTP server."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Mail created and send attempt finished."
    )
    @BadRequestApiError
    @NotFoundApiError
    fun createAndSendMail(@Valid @RequestPart("data") data: MailRequest,
                          @RequestPart("attachments") attachments: List<MultipartFile>,
                          @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): MailDTO {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")

        val createdMail = mailService.createAndSendMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }

}
