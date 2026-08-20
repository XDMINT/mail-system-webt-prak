package de.thm.mni.backend

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@OpenAPIDefinition(
    info = Info(
        title = "Mail System API",
        version = "1.0.0",
        description = "REST API for the THM Web Technologies mail system. The API supports JWT authentication, user management, drafting and sending mails, incoming mail retrieval, and protected attachment downloads.",
        contact = Contact(
            name = "Mail System Team",
            email = "support@example.com"
        ),
        license = License(
            name = "MIT",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = [
        Server(
            url = "http://localhost:8080",
            description = "Local backend server"
        ),
        Server(
            url = "http://localhost",
            description = "Docker Compose frontend reverse proxy"
        )
    ]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT access token returned by `POST /api/login` or `POST /api/register`. Use the `Authorization: Bearer <token>` header."
)
@SpringBootApplication
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
