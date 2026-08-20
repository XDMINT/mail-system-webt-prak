package de.thm.mni.backend.user


import de.thm.mni.backend.error.ResourceAlreadyExistsException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.user.dto.EnsureUserRequest
import de.thm.mni.backend.user.dto.EnsureUserResponse
import de.thm.mni.backend.user.dto.UserDTO
import de.thm.mni.backend.user.dto.UserUpdate
import de.thm.mni.backend.user.dto.toDTO
import jakarta.validation.Valid
import de.thm.mni.backend.openapi.DefaultApiResponses
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/users", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "User", description = "Access internal profiles and resolve external mail recipients.")
@DefaultApiResponses
class UserController(
    private val userService: UserService,
    private val currentUserService: CurrentUserService,
) {


    @Operation(operationId = "getUsers", summary = "List users", description = "Returns all known internal users and external mail contacts.")
    @ApiResponse(responseCode = "200", description = "Users returned")
    @GetMapping
    fun getAllUsers(): List<UserDTO> {
        return userService.getAllUsers().map { it -> it.toDTO() }
    }

    @Operation(operationId = "getCurrentUser", summary = "Get current user", description = "Returns the local profile linked to the authenticated Keycloak identity.")
    @ApiResponse(responseCode = "200", description = "Current user returned")
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal jwt: Jwt): UserDTO =
        currentUserService.getOrProvision(jwt).toDTO()

    @Operation(operationId = "getUser", summary = "Get a user", description = "Returns a user or external contact by identifier.")
    @ApiResponse(responseCode = "200", description = "User returned")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: UUID): UserDTO =
        (userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")).toDTO()

    @Operation(operationId = "ensureExternalContact", summary = "Resolve an external contact", description = "Returns the contact for an email address and creates it when it does not yet exist.")
    @ApiResponse(responseCode = "200", description = "External contact resolved")
    @ApiResponse(responseCode = "400", description = "Invalid contact data")
    @PostMapping("/ensure", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun ensureUser(@Valid @RequestBody data: EnsureUserRequest): EnsureUserResponse {
        val normalizedEmail = data.email.trim().lowercase()
        val user = userService.getUserByEmail(normalizedEmail) ?: userService.createUser(
            User(
                firstName = data.firstName?.trim()?.takeIf { it.isNotBlank() } ?: "External",
                lastName = data.lastName?.trim()?.takeIf { it.isNotBlank() } ?: "User",
                email = normalizedEmail,
                externalContact = true
            )
        )

        return EnsureUserResponse(
            id = requireNotNull(user.id) { "Cannot return a contact without an identifier" },
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
        )
    }

    @Operation(operationId = "updateCurrentUser", summary = "Update current user", description = "Updates the authenticated user's own local profile.")
    @ApiResponse(responseCode = "200", description = "User updated")
    @ApiResponse(responseCode = "400", description = "Invalid profile data")
    @ApiResponse(responseCode = "404", description = "User not found or not accessible")
    @ApiResponse(responseCode = "409", description = "Email address is already in use")
    @PutMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateUser(@PathVariable id: UUID,
                   @Valid @RequestBody userData: UserUpdate,
                   @AuthenticationPrincipal jwt: Jwt): UserDTO {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")
        val currentUser = currentUserService.getOrProvision(jwt)

        if(existingUser.id != currentUser.id) {
            throw ResourceNotFoundException("User not found")
        }

        val normalizedEmail = userData.email.trim().lowercase()
        val userWithExistingEmail = userService.getUserByEmail(normalizedEmail)

        if ( userWithExistingEmail != null && userWithExistingEmail.id != existingUser.id ) {
            throw ResourceAlreadyExistsException("Email is already in use by another user")
        }

       val updatedUser = User(
            firstName = userData.firstName.trim(),
            lastName = userData.lastName.trim(),
            email = normalizedEmail,
            externalContact = existingUser.externalContact,
            identityProviderSubject = existingUser.identityProviderSubject,
        )
        updatedUser.id = existingUser.id

        return userService.updateUser(id, updatedUser).toDTO()
    }


    @Operation(operationId = "deleteCurrentUser", summary = "Delete current user", description = "Deletes the authenticated user's own local profile.")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found or not accessible")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: UUID, @AuthenticationPrincipal jwt: Jwt) {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")
        val currentUser = currentUserService.getOrProvision(jwt)
        if(existingUser.id != currentUser.id) {
            throw ResourceNotFoundException("User not found")
        }
        userService.deleteUser(id)
    }

}
