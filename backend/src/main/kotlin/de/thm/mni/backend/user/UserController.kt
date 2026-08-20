package de.thm.mni.backend.user


import de.thm.mni.backend.error.ResourceAlreadyExistsException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.openapi.BadRequestApiError
import de.thm.mni.backend.openapi.ConflictApiError
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.openapi.NotFoundApiError
import de.thm.mni.backend.user.dto.EnsureUserRequest
import de.thm.mni.backend.user.dto.EnsureUserResponse
import de.thm.mni.backend.user.dto.UserDTO
import de.thm.mni.backend.user.dto.UserUpdate
import de.thm.mni.backend.user.dto.toDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
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
@Tag(
    name = "Users",
    description = "Read user profiles, update the authenticated user, and create external contact users."
)
@SecurityRequirement(name = "bearerAuth")
@DefaultApiErrors
class UserController(private val userService: UserService, private val passwordEncoder: PasswordEncoder) {


    @GetMapping
    @Operation(
        operationId = "listUsers",
        summary = "List users",
        description = "Returns all users known to the application. This list is used to select recipients while composing mails."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Users returned successfully."
    )
    fun getAllUsers(): List<UserDTO> {
        return userService.getAllUsers().map { it -> it.toDTO() }
    }

    @GetMapping("/{id}")
    @Operation(
        operationId = "getUserById",
        summary = "Get a user",
        description = "Returns the user profile for the supplied id."
    )
    @ApiResponse(
        responseCode = "200",
        description = "User returned successfully. If the id is unknown, the current implementation returns an empty response body."
    )
    fun getUserById(@PathVariable id: UUID): UserDTO? {
        return userService.getUserById(id)?.toDTO()
    }

    @PostMapping("/ensure", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "ensureExternalUser",
        summary = "Ensure an external contact",
        description = "Looks up a user by normalized email. If none exists, creates an external contact user with generated credentials."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Existing or newly created contact returned successfully."
    )
    @BadRequestApiError
    fun ensureUser(@Valid @RequestBody data: EnsureUserRequest): EnsureUserResponse {
        val normalizedEmail = data.email.trim().lowercase()
        val user = userService.getUserByEmail(normalizedEmail) ?: userService.createUser(
            User(
                firstName = data.firstName?.takeIf { it.isNotBlank() } ?: "External",
                lastName = data.lastName?.takeIf { it.isNotBlank() } ?: "User",
                email = normalizedEmail,
                password = passwordEncoder.encode(UUID.randomUUID().toString()).toString(),
                externalContact = true
            )
        )

        return EnsureUserResponse(
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
        )
    }

    @PutMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "updateUser",
        summary = "Update the authenticated user",
        description = "Updates the profile of the authenticated user. Users can only update their own profile."
    )
    @ApiResponse(
        responseCode = "200",
        description = "User updated successfully."
    )
    @BadRequestApiError
    @NotFoundApiError
    @ConflictApiError
    fun updateUser(@PathVariable id: UUID,
                   @Valid @RequestBody userData: UserUpdate,
                   @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): UserDTO? {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")

        if(existingUser.id.toString() != user.username) {
            throw ResourceNotFoundException("User not found")
        }

        val userWithExistingEmail = userService.getUserByEmail(userData.email)

        if ( userWithExistingEmail != null && userWithExistingEmail.id != existingUser.id ) {
            throw ResourceAlreadyExistsException("Email is already in use by another user")
        }

       val updatedUser = User(
            firstName = userData.firstName,
            lastName = userData.lastName,
            email = userData.email,
            password = existingUser.password,
            externalContact = existingUser.externalContact
        )
        updatedUser.id = existingUser.id

        return userService.updateUser(id, updatedUser).toDTO()
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        operationId = "deleteUser",
        summary = "Delete the authenticated user",
        description = "Deletes the authenticated user's own account."
    )
    @ApiResponse(responseCode = "204", description = "User deleted successfully.")
    @NotFoundApiError
    fun deleteUser(@PathVariable id: UUID, @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails) {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")
        if(existingUser.id.toString() != user.username) {
            throw ResourceNotFoundException("User not found")
        }
        userService.deleteUser(id)
    }

}
