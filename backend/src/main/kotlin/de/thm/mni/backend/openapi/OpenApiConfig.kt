package de.thm.mni.backend.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
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
                .description("REST API for the THM mail support application")
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

    private companion object {
        const val OIDC_SECURITY_SCHEME = "keycloakOidc"
    }
}
