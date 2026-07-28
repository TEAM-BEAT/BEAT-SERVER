package com.beat.gateway.authentication.internal

import com.beat.contracts.auth.JwtTokenPort
import com.beat.contracts.auth.JwtTokenType
import com.beat.contracts.auth.TokenValidationResult
import com.beat.observability.logging.filter.BaseMdcLoggingFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component("gatewayJwtAuthenticationFilter")
class JwtAuthenticationFilter(
    private val jwtTokenPort: JwtTokenPort,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    private val authenticationDetailsSource = WebAuthenticationDetailsSource()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.bearerToken()

        if (token == null) {
            log.debug("JWT Token not found in request header. Assuming guest access or public API request.")
            filterChain.doFilter(request, response)
            return
        }

        try {
            val validationResult = jwtTokenPort.validateAccessToken(token)

            if (validationResult != TokenValidationResult.VALID) {
                response.status = validationResult.toHttpStatus()
                return
            }

            authenticate(token, request)
            filterChain.doFilter(request, response)
        } catch (exception: IllegalArgumentException) {
            log.warn("Invalid JWT claims: {}", exception.message)
            response.status = HttpServletResponse.SC_UNAUTHORIZED
        } catch (exception: Exception) {
            log.error("JWT Authentication Exception: ", exception)
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }
    }

    private fun authenticate(token: String, request: HttpServletRequest) {
        val memberId = jwtTokenPort.getMemberId(token, JwtTokenType.ACCESS)
        val roleName = jwtTokenPort.getRoleName(token, JwtTokenType.ACCESS)

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
            else -> UsernamePasswordAuthenticationToken(memberId, null, authorities)
        }
    }

    /**
     * `EXPIRED -> 401`, 그 외 실패 -> `400`은 현재 client 호환 계약이다.
     */
    private fun TokenValidationResult.toHttpStatus(): Int = when (this) {
        TokenValidationResult.EXPIRED -> HttpServletResponse.SC_UNAUTHORIZED
        else -> HttpServletResponse.SC_BAD_REQUEST
    }

    private fun HttpServletRequest.bearerToken(): String? =
        getHeader(HEADER_AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
            ?.takeIf(String::isNotBlank)

    companion object {
        private const val ROLE_ADMIN = "ROLE_ADMIN"
        private const val ROLE_MEMBER = "ROLE_MEMBER"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
