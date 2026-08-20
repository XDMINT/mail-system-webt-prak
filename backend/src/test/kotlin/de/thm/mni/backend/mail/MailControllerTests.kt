package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.enums.MailStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
@Transactional
class MailControllerTests @Autowired constructor(
    private val mailController: MailController,
    private val mailService: MailService,
) {
    @Test
    fun `authenticated support user can create a reply draft for the shared seed mail`() {
        val incomingMail = mailService.getMailByExternalMessageId("demo-support-request@example.org")!!

        val reply = mailController.createReplyDraft(incomingMail.id!!, demoUserJwt())

        assertEquals(MailStatus.DRAFT, reply.status)
        assertEquals(incomingMail.id, reply.inReplyToMailId)
        assertEquals("erika.external@example.org", reply.to.single().email)
        assertNotNull(reply.trackingCode)
        assertEquals("[${reply.trackingCode}] Re: Question about my semester registration", reply.subject)
    }

    private fun demoUserJwt(): Jwt = Jwt.withTokenValue("test-token")
        .header("alg", "RS256")
        .subject("11111111-1111-4111-8111-111111111111")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .claim("email", "aallanson@example.com")
        .claim("given_name", "Ameline")
        .claim("family_name", "Allanson")
        .build()
}
