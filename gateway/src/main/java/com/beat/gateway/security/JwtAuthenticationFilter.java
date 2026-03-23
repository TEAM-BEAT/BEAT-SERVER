package com.beat.gateway.security;

import com.beat.contracts.auth.JwtTokenPort;
import com.beat.contracts.auth.TokenValidationResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String ROLE_MEMBER = "ROLE_MEMBER";

	private final JwtTokenPort jwtTokenPort;

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		final String token = getJwtFromRequest(request);

		if (!StringUtils.hasText(token)) {
			log.debug("JWT Token not found in request header. Assuming guest access or public API request.");
			filterChain.doFilter(request, response);
			return;
		}

		try {
			TokenValidationResult validationResult = jwtTokenPort.validateToken(token);

			if (validationResult != TokenValidationResult.VALID) {
				handleInvalidToken(validationResult, response);
				return;
			}

			setAuthentication(token, request);
			filterChain.doFilter(request, response);
		} catch (IllegalArgumentException exception) {
			log.warn("Invalid JWT claims: {}", exception.getMessage());
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		} catch (Exception exception) {
			log.error("JWT Authentication Exception: ", exception);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private void setAuthentication(String token, HttpServletRequest request) {
		Long memberId = jwtTokenPort.getMemberId(token);
		String roleName = jwtTokenPort.getRoleName(token);

		Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));
		UsernamePasswordAuthenticationToken authentication = createAuthentication(memberId, authorities, roleName);
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void handleInvalidToken(TokenValidationResult validationResult, HttpServletResponse response) {
		if (validationResult == TokenValidationResult.EXPIRED) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	private UsernamePasswordAuthenticationToken createAuthentication(
		Long memberId,
		Collection<GrantedAuthority> authorities,
		String roleName
	) {
		if (ROLE_ADMIN.equals(roleName)) {
			return new AdminAuthentication(memberId, null, authorities);
		}
		if (ROLE_MEMBER.equals(roleName)) {
			return new MemberAuthentication(memberId, null, authorities);
		}
		return new UsernamePasswordAuthenticationToken(memberId, null, authorities);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring("Bearer ".length());
		}
		return null;
	}
}
