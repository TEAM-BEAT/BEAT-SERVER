package com.beat.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomJwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) {
		String path = request.getRequestURI();
		String method = request.getMethod();
		log.warn("Unauthorized access attempt: Method: {}, Path: {}, Message: {}", method, path,
			authException.getMessage());
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}
}
