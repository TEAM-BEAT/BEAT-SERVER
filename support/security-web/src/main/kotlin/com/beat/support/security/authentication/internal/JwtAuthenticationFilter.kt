package com.beat.support.security.authentication.internal

import com.beat.support.observability.logging.filter.BaseMdcLoggingFilter
import com.beat.support.security.access.AccessTokenAuthenticationFailure
import com.beat.support.security.access.AccessTokenAuthenticationResult
import com.beat.support.security.access.AccessTokenAuthenticator
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component("gatewayJwtAuthenticationFilter")
internal class JwtAuthenticationFilter(
    private val accessTokenAuthenticator: AccessTokenAuthenticator,
) : OncePerRequestFilter() {

    private val authenticationDetailsSource = WebAuthenticationDetailsSource()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.bearerToken()

        if (token == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            when (val result = accessTokenAuthenticator.authenticateAccessToken(token)) {
                is AccessTokenAuthenticationResult.Authenticated -> {
                    authenticate(result.memberId, result.roleName, request)
                    filterChain.doFilter(request, response)
                }

                is AccessTokenAuthenticationResult.Rejected -> {
                    response.status = result.failure.toHttpStatus()
                }
            }
        } catch (_: IllegalArgumentException) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
        } catch (exception: Exception) {
            log.error(exception) { "Unexpected JWT authentication failure" }
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }
    }

    private fun authenticate(memberId: Long, roleName: String, request: HttpServletRequest) {
        val authentication = createAuthentication(memberId, roleName).apply {
            details = authenticationDetailsSource.buildDetails(request)
        }

        SecurityContextHolder.getContext().authentication = authentication
        MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, memberId.toString())
    }

    private fun createAuthentication(memberId: Long, roleName: String): UsernamePasswordAuthenticationToken {
        val authorities: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority(roleName))
        return when (roleName) {
            ROLE_ADMIN -> AdminAuthentication(memberId, authorities)
            ROLE_MEMBER -> MemberAuthentication(memberId, authorities)
            else -> throw IllegalArgumentException("Unsupported role: $roleName")
        }
    }

    /**
     * `EXPIRED -> 401`, 그 외 실패 -> `400`은 현재 client 호환 계약이다.
     */
    private fun AccessTokenAuthenticationFailure.toHttpStatus(): Int = when (this) {
        AccessTokenAuthenticationFailure.EXPIRED -> HttpServletResponse.SC_UNAUTHORIZED
        else -> HttpServletResponse.SC_BAD_REQUEST
    }

    private fun HttpServletRequest.bearerToken(): String? =
        getHeader(HEADER_AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
            ?.takeIf(String::isNotBlank)

    companion object {
        private val log = KotlinLogging.logger {}
        private const val ROLE_ADMIN = "ROLE_ADMIN"
        private const val ROLE_MEMBER = "ROLE_MEMBER"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
