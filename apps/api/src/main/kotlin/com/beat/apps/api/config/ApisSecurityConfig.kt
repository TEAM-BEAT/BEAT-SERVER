package com.beat.apps.api.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter

@Configuration(proxyBeanMethods = false)
class ApisSecurityConfig(
    @param:Qualifier("gatewayJwtAuthenticationFilter")
    private val jwtAuthenticationFilter: OncePerRequestFilter,
    @param:Qualifier("gatewaySecurityMdcLoggingFilter")
    private val securityMdcLoggingFilter: OncePerRequestFilter,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
    private val accessDeniedHandler: AccessDeniedHandler,
    private val guestSessionOriginFilter: GuestSessionOriginFilter,
    private val environment: Environment,
    @param:Value("\${management.endpoints.web.base-path}")
    private val actuatorEndpoint: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // Bearer endpoints are stateless. Cookie-authenticated guest mutations are origin-checked separately.
        http.csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(*authWhitelist()).permitAll()
                    .requestMatchers(*AUTH_ADMIN_ONLY).hasAuthority(ROLE_ADMIN)
                    .anyRequest().authenticated()
            }
            .addFilterBefore(securityMdcLoggingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(guestSessionOriginFilter, securityMdcLoggingFilter.javaClass)
            .addFilterAfter(jwtAuthenticationFilter, guestSessionOriginFilter.javaClass)
        return http.build()
    }

    private fun authWhitelist(): Array<String> = buildList {
        addAll(
            listOf(
                "/api/users/sign-up",
                "/api/users/refresh-token",
                "/api/bookings/guest/**",
                "/api/main",
                "/api/performances/booking/**",
                "/api/schedules/**",
                "/api/notifications/**",
                "/api/performances/detail/**",
                "/error",
                "/api/bookings/refund",
                "/api/bookings/cancel",
                "$actuatorEndpoint/health",
                "$actuatorEndpoint/prometheus",
            ),
        )
        if (!environment.acceptsProfiles(Profiles.of("prod"))) addAll(SWAGGER_WHITELIST)
    }.toTypedArray()

    private companion object {
        const val ROLE_ADMIN = "ROLE_ADMIN"
        val SWAGGER_WHITELIST = arrayOf("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**")
        val AUTH_ADMIN_ONLY = arrayOf("/api/admin/**")
    }
}
