package de.thm.mni.backend.user

import de.thm.mni.backend.error.ResourceAlreadyExistsException
import de.thm.mni.backend.error.ResourceNotFoundException
import jakarta.transaction.Transactional
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

@Service
class CurrentUserService(private val userRepository: UserRepository) {

    @Transactional
    fun getOrProvision(jwt: Jwt): User {
        userRepository.findByIdentityProviderSubject(jwt.subject)?.let { user ->
            updateProfileFromClaims(user, jwt)
            return userRepository.save(user)
        }

        val email = jwt.getClaimAsString("email")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: throw ResourceNotFoundException("The identity provider did not supply an email address")

        val user = userRepository.findUserByEmail(email) ?: User(
            firstName = claimOrFallback(jwt, "given_name", "Keycloak"),
            lastName = claimOrFallback(jwt, "family_name", "User"),
            email = email,
        )

        if (user.identityProviderSubject != null && user.identityProviderSubject != jwt.subject) {
            throw ResourceAlreadyExistsException("The email address is already linked to another identity")
        }

        user.identityProviderSubject = jwt.subject
        user.externalContact = false
        updateProfileFromClaims(user, jwt)
        return userRepository.save(user)
    }

    private fun updateProfileFromClaims(user: User, jwt: Jwt) {
        jwt.getClaimAsString("email")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?.let { user.email = it }
        jwt.getClaimAsString("given_name")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { user.firstName = it }
        jwt.getClaimAsString("family_name")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { user.lastName = it }
    }

    private fun claimOrFallback(jwt: Jwt, claim: String, fallback: String): String =
        jwt.getClaimAsString(claim)?.trim()?.takeIf { it.isNotBlank() } ?: fallback
}
