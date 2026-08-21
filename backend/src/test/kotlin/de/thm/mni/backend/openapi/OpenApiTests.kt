package de.thm.mni.backend.openapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiTests {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `generated OpenAPI describes every operation schemas errors and OIDC authentication`() {
        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI("http://localhost:$port/v3/api-docs")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )

        assertEquals(200, response.statusCode())
        val spec = ObjectMapper().readTree(response.body())
        assertFalse(response.body().contains("2147483647"))
        assertEquals("3.1.0", spec["openapi"].stringValue())
        assertEquals("THM Mail Support API", spec["info"]["title"].stringValue())
        assertTrue(spec["info"]["description"].stringValue().isNotBlank())
        assertTrue(spec["info"]["version"].stringValue().isNotBlank())
        assertEquals(
            "openIdConnect",
            spec["components"]["securitySchemes"][OpenApiConfig.OIDC_SECURITY_SCHEME]["type"].stringValue(),
        )
        assertTrue(
            spec["components"]["securitySchemes"][OpenApiConfig.OIDC_SECURITY_SCHEME]["openIdConnectUrl"]
                .stringValue().endsWith("/.well-known/openid-configuration")
        )
        assertNotNull(spec["security"][0][OpenApiConfig.OIDC_SECURITY_SCHEME])

        val operations = mutableListOf<JsonNode>()
        spec["paths"].properties().forEach { (_, path) ->
            HTTP_METHODS.mapNotNull(path::get).forEach(operations::add)
        }
        assertEquals(17, operations.size)
        assertEquals(operations.size, operations.map { it["operationId"].stringValue() }.distinct().size)
        operations.forEach { operation ->
            assertTrue(operation["operationId"].stringValue().isNotBlank())
            assertTrue(operation["summary"].stringValue().isNotBlank())
            assertTrue(operation["description"].stringValue().isNotBlank())
            assertTrue(operation["tags"].size() > 0)
            assertTrue(operation["responses"].properties().any { (code, _) -> code.startsWith("2") })
            assertNotNull(operation["responses"]["401"])
            assertNotNull(operation["responses"]["500"])
            operation["responses"].properties()
                .filter { (code, _) -> code.toIntOrNull()?.let { it >= 400 } == true }
                .forEach { (_, errorResponse) ->
                    assertEquals(
                        "#/components/schemas/AppError",
                        errorResponse["content"]["application/json"]["schema"]["\$ref"].stringValue(),
                    )
                }
        }

        REQUIRED_SCHEMAS.forEach { schemaName ->
            val schema = spec["components"]["schemas"][schemaName]
            assertNotNull(schema, "Missing schema $schemaName")
            assertTrue(schema["description"].stringValue().isNotBlank(), "Missing description for $schemaName")
            schema["properties"]?.properties()?.forEach { (propertyName, property) ->
                assertTrue(
                    property["description"]?.stringValue()?.isNotBlank() == true,
                    "Missing description for $schemaName.$propertyName",
                )
            }
        }

        REQUIRED_RESPONSE_IDS.forEach { schemaName ->
            val requiredProperties = spec["components"]["schemas"][schemaName]["required"]
                .iterator().asSequence().map(JsonNode::stringValue).toSet()
            assertTrue("id" in requiredProperties, "$schemaName.id must be required")
        }

        assertEquals(
            setOf("subject", "content", "toIds", "ccIds", "bccIds"),
            spec["components"]["schemas"]["MailRequest"]["required"]
                .iterator().asSequence().map(JsonNode::stringValue).toSet(),
        )
        assertEquals(
            setOf("subject", "content", "toIds", "ccIds", "bccIds"),
            spec["components"]["schemas"]["MailUpdateRequest"]["required"]
                .iterator().asSequence().map(JsonNode::stringValue).toSet(),
        )
        assertEquals(
            setOf("firstName", "lastName", "email"),
            spec["components"]["schemas"]["UserUpdate"]["required"]
                .iterator().asSequence().map(JsonNode::stringValue).toSet(),
        )
        val userUpdateProperties = spec["components"]["schemas"]["UserUpdate"]["properties"]
        listOf("firstName", "lastName", "email").forEach { property ->
            assertEquals(1, userUpdateProperties[property]["minLength"].intValue())
            assertEquals(255, userUpdateProperties[property]["maxLength"].intValue())
        }
        val ensureUserProperties = spec["components"]["schemas"]["EnsureUserRequest"]["properties"]
        listOf("email", "firstName", "lastName").forEach { property ->
            assertEquals(1, ensureUserProperties[property]["minLength"].intValue())
            assertEquals(255, ensureUserProperties[property]["maxLength"].intValue())
        }

        val createRequest = spec["paths"]["/api/mails"]["post"]["requestBody"]["content"]
            .get("multipart/form-data")["schema"]
        val requiredParts = createRequest["required"].toString()
        assertTrue(requiredParts.contains("data"))
        assertFalse(requiredParts.contains("attachments"))
        assertNotNull(
            spec["paths"]["/api/mails/send"]["post"]["requestBody"]["content"]
                .get("multipart/form-data"),
        )
        assertEquals(
            "binary",
            spec["paths"]["/api/attachments/{attachmentId}"]["get"]["responses"]["200"]["content"]
                .get("application/octet-stream")["schema"]["format"].stringValue(),
        )
    }

    private companion object {
        val HTTP_METHODS = listOf("get", "post", "put", "delete", "patch")
        val REQUIRED_SCHEMAS = listOf(
            "AppError",
            "AttachmentDTO",
            "EnsureUserRequest",
            "EnsureUserResponse",
            "MailDTO",
            "MailListItemDTO",
            "MailRequest",
            "MailUpdateRequest",
            "PageResponseMailListItemDTO",
            "UserDTO",
            "UserUpdate",
        )
        val REQUIRED_RESPONSE_IDS = listOf(
            "EnsureUserResponse",
            "MailDTO",
            "MailListItemDTO",
            "UserDTO",
        )
    }
}
