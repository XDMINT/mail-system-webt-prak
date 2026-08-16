package de.thm.mni.backend.security

import de.thm.mni.backend.util.SaltPepperPasswordEncoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder


@Configuration
class SecurityConfig(@Value("\${app.secret}") private val pepper: String) {

    @Bean
    fun jwtDecoder(
        @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        issuer: String
    ): JwtDecoder {
        return NimbusJwtDecoder.withIssuerLocation(issuer).build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = SaltPepperPasswordEncoder(pepper)

    @Bean
    fun securityFilterChain(http: HttpSecurity, authFilter: AuthFilter): SecurityFilterChain =
        http
            .csrf { it.disable() } // Explicitly disable CSRF protection for stateless APIs
            .cors {
                it.configurationSource { _ ->
                    val config = CorsConfiguration()
                    config.allowedOriginPatterns = listOf("*")
                    config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    config.allowedHeaders = listOf("*")
                    config.allowCredentials = true
                    config.maxAge = 3600L
                    config
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/register",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/v1/**"
                ).permitAll()
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt { }
            }
            .build()
}