package com.beat.gateway.security.internal.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import com.beat.observability.logging.filter.BaseMdcLoggingFilter;
import com.beat.observability.tracing.NoOpTraceContextResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityMdcLoggingFilterTest {

	private static final int MANAGEMENT_PORT = 55555;

	private final SecurityMdcLoggingFilter filter = new SecurityMdcLoggingFilter(NoOpTraceContextResolver.INSTANCE);
	private final SecurityMdcLoggingFilter filterWithManagementPort =
		new SecurityMdcLoggingFilter(NoOpTraceContextResolver.INSTANCE, MANAGEMENT_PORT);

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
		MDC.clear();
	}

	@Test
	void usesGuestWhenSecurityContextIsEmpty() throws ServletException, IOException {
		doFilterAndAssertUserId(BaseMdcLoggingFilter.DEFAULT_GUEST_USER);
	}

	@Test
	void usesLongPrincipalAsUserId() throws ServletException, IOException {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(123L, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))
		);

		doFilterAndAssertUserId("123");
	}

	@Test
	void usesGuestWhenAnonymousAuthentication() throws ServletException, IOException {
		SecurityContextHolder.getContext().setAuthentication(
			new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
		);

		doFilterAndAssertUserId(BaseMdcLoggingFilter.DEFAULT_GUEST_USER);
	}

	@Test
	void fallsBackToAuthenticationName() throws ServletException, IOException {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("member-name", null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))
		);

		doFilterAndAssertUserId("member-name");
	}

	@Test
	void skipsFilterOnManagementPort() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
		request.setLocalPort(MANAGEMENT_PORT);
		MockHttpServletResponse response = new MockHttpServletResponse();
		boolean[] chainInvoked = {false};
		boolean[] mdcPopulatedDuringChain = {false};
		FilterChain chain = (req, res) -> {
			chainInvoked[0] = true;
			mdcPopulatedDuringChain[0] = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null;
		};

		filterWithManagementPort.doFilter(request, response, chain);

		assertTrue(chainInvoked[0]);
		assertFalse(mdcPopulatedDuringChain[0]);
		assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
	}

	@Test
	void doesNotSkipFilterOnRegularPort() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/main");
		request.setLocalPort(8080);
		MockHttpServletResponse response = new MockHttpServletResponse();
		boolean[] mdcPopulated = {false};
		FilterChain chain = (req, res) -> mdcPopulated[0] = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null;

		filterWithManagementPort.doFilter(request, response, chain);

		assertTrue(mdcPopulated[0]);
	}

	@Test
	void doesNotSkipFilterWhenNoManagementPortInjected() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
		request.setLocalPort(MANAGEMENT_PORT);
		MockHttpServletResponse response = new MockHttpServletResponse();
		boolean[] mdcPopulated = {false};
		FilterChain chain = (req, res) -> mdcPopulated[0] = MDC.get(BaseMdcLoggingFilter.USER_ID_KEY) != null;

		filter.doFilter(request, response, chain);

		assertTrue(mdcPopulated[0]);
	}

	private void doFilterAndAssertUserId(String expectedUserId) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/main");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = (servletRequest, servletResponse) -> assertEquals(
			expectedUserId,
			MDC.get(BaseMdcLoggingFilter.USER_ID_KEY)
		);

		filter.doFilter(request, response, chain);

		assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
	}
}
