package de.thm.mni.backend.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${app.openapi.oidc-discovery-url}") private val oidcDiscoveryUrl: String,
) {

    @Bean
    fun mailSystemOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("THM Mail Support API")
                .description(
                    "REST API for the shared THM mail support application. " +
                        "Application operations require a Keycloak access token."
                )
                .version("1.0.0")
        )
        .components(
            Components().addSecuritySchemes(
                OIDC_SECURITY_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.OPENIDCONNECT)
                    .openIdConnectUrl(oidcDiscoveryUrl)
            )
        )
        .addSecurityItem(SecurityRequirement().addList(OIDC_SECURITY_SCHEME))

    @Bean
    fun errorResponseContentCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.paths.orEmpty().values.forEach { pathItem ->
            pathItem.readOperations().forEach { operation ->
                operation.responses.orEmpty()
                    .filterKeys { code -> code.toIntOrNull()?.let { it >= 400 } == true }
                    .values
                    .forEach { response ->
                        response.content = Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/AppError")),
                        )
                    }
            }
        }
    }

    companion object {
        const val OIDC_SECURITY_SCHEME = "keycloakOidc"
    }
}
