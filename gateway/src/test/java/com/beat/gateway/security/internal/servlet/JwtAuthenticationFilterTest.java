package com.beat.gateway.security.internal.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beat.contracts.auth.JwtTokenPort;
import com.beat.contracts.auth.JwtTokenType;
import com.beat.contracts.auth.TokenValidationResult;
import com.beat.observability.logging.filter.BaseMdcLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

	private final JwtTokenPort jwtTokenPort = mock(JwtTokenPort.class);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenPort);

	@AfterEach
	void tearDown() {
		MDC.clear();
		SecurityContextHolder.clearContext();
	}

	@Test
	void validTokenUpdatesSecurityContextAndAlreadyInitializedMdcUserId() throws Exception {
		MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123");
		MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER);
		when(jwtTokenPort.validateAccessToken("valid-token")).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId("valid-token", JwtTokenType.ACCESS)).thenReturn(42L);
		when(jwtTokenPort.getRoleName("valid-token", JwtTokenType.ACCESS)).thenReturn("ROLE_MEMBER");
		MockHttpServletRequest request = requestWithBearer("valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = (servletRequest, servletResponse) -> {
			assertEquals("trace-123", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY));
			assertEquals("42", MDC.get(BaseMdcLoggingFilter.USER_ID_KEY));
			assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		};

		filter.doFilter(request, response, chain);

		assertEquals("42", MDC.get(BaseMdcLoggingFilter.USER_ID_KEY));
		assertEquals(200, response.getStatus());
	}

	@Test
	void invalidTokenDoesNotClearAlreadyInitializedMdcWhenShortCircuitingChain() throws Exception {
		MDC.put(BaseMdcLoggingFilter.TRACE_ID_KEY, "trace-123");
		MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, BaseMdcLoggingFilter.DEFAULT_GUEST_USER);
		when(jwtTokenPort.validateAccessToken("expired-token")).thenReturn(TokenValidationResult.EXPIRED);
		MockHttpServletRequest request = requestWithBearer("expired-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
		assertEquals("trace-123", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY));
		assertEquals(BaseMdcLoggingFilter.DEFAULT_GUEST_USER, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY));
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(chain, never()).doFilter(request, response);
	}

	private MockHttpServletRequest requestWithBearer(String token) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/main");
		request.addHeader("Authorization", "Bearer " + token);
		return request;
	}

}
