package com.sentinelx.shared.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity
    ): SecurityFilterChain {

        http
            .csrf { csrf ->
                csrf.disable()
            }
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authorizeHttpRequests { auth ->

                auth
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/actuator/prometheus"
                    )
                    .permitAll()

                    .requestMatchers(
                        "/api/auth/**"
                    )
                    .permitAll()

                    .requestMatchers(
                        "/api/public/**"
                    )
                    .permitAll()

                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/community/**"
                    )
                    .permitAll()

                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                    )
                    .permitAll()

                    .anyRequest()
                    .authenticated()
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
            .exceptionHandling { exceptions ->

                exceptions.authenticationEntryPoint {
                        _,
                        response,
                        _ ->

                    response.status = 401
                    response.contentType = "application/json"

                    response.writer.write(
                        """
                        {
                          "success": false,
                          "error": {
                            "code": "UNAUTHORIZED",
                            "message": "Authentication required"
                          }
                        }
                        """.trimIndent()
                    )
                }

                exceptions.accessDeniedHandler {
                        _,
                        response,
                        _ ->

                    response.status = 403
                    response.contentType = "application/json"

                    response.writer.write(
                        """
                        {
                          "success": false,
                          "error": {
                            "code": "FORBIDDEN",
                            "message": "Access denied"
                          }
                        }
                        """.trimIndent()
                    )
                }
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {

        val configuration = CorsConfiguration()

        configuration.allowedOrigins = listOf(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        )

        configuration.allowedMethods = listOf(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        )

        configuration.allowedHeaders = listOf("*")

        configuration.allowCredentials = true

        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()

        source.registerCorsConfiguration(
            "/**",
            configuration
        )

        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(12)
    }

    @Bean
    fun authenticationManager(
        configuration: AuthenticationConfiguration
    ): AuthenticationManager {
        return configuration.authenticationManager
    }
}