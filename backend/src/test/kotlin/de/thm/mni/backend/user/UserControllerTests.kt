package de.thm.mni.backend.user

import de.thm.mni.backend.user.dto.EnsureUserRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class UserControllerTests @Autowired constructor(
    private val userController: UserController,
) {

    @Test
    fun `ensureUser creates missing user and returns existing user on repeated call`() {
        val email = "ensured-${System.nanoTime()}@example.com"

        val created = userController.ensureUser(EnsureUserRequest(email = email))
        val existing = userController.ensureUser(EnsureUserRequest(email = email))

        assertNotNull(created.id)
        assertEquals(created.id, existing.id)
        assertEquals("External", created.firstName)
        assertEquals("User", created.lastName)
        assertEquals(email, created.email)
    }
}