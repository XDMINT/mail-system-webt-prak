package de.thm.mni.backend.security

import de.thm.mni.backend.error.AuthErrorHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration

@Configuration
class SecurityConfig(
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: String,
    private val authErrorHandler: AuthErrorHandler,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors {
                it.configurationSource { _ ->
                    CorsConfiguration().apply {
                        allowedOrigins = this@SecurityConfig.allowedOrigins
                            .split(',')
                            .map(String::trim)
                            .filter(String::isNotBlank)
                        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
                        allowCredentials = true
                        maxAge = CORS_PREFLIGHT_MAX_AGE_SECONDS
                    }
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                ).permitAll()
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt { }
                it.authenticationEntryPoint(authErrorHandler)
            }
            .exceptionHandling { it.authenticationEntryPoint(authErrorHandler) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .build()

    private companion object {
        private const val CORS_PREFLIGHT_MAX_AGE_SECONDS = 3600L
    }
}
