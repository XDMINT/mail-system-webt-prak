package de.thm.mni.backend.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@SpringBootTest
@Transactional
class CurrentUserServiceTests @Autowired constructor(
    private val currentUserService: CurrentUserService,
) {

    @Test
    fun `maps a seeded Keycloak identity to its local profile`() {
        val user = currentUserService.getOrProvision(
            jwt(
                subject = "11111111-1111-4111-8111-111111111111",
                email = "aallanson@example.com",
                firstName = "Ameline",
                lastName = "Allanson",
            )
        )

        assertNotNull(user.id)
        assertEquals("11111111-1111-4111-8111-111111111111", user.identityProviderSubject)
        assertEquals("aallanson@example.com", user.email)
        assertFalse(user.externalContact)
    }

    @Test
    fun `provisions a local profile for a new Keycloak identity`() {
        val subject = UUID.randomUUID().toString()
        val email = "oidc-${System.nanoTime()}@example.com"

        val user = currentUserService.getOrProvision(
            jwt(
                subject = subject,
                email = email,
                firstName = "OIDC",
                lastName = "User",
            )
        )

        assertNotNull(user.id)
        assertEquals(subject, user.identityProviderSubject)
        assertEquals(email, user.email)
        assertEquals("OIDC", user.firstName)
        assertEquals("User", user.lastName)
    }

    private fun jwt(subject: String, email: String, firstName: String, lastName: String): Jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .claim("email", email)
            .claim("given_name", firstName)
            .claim("family_name", lastName)
            .build()
}
