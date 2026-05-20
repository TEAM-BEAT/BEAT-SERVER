package com.beat.gateway.security.internal.servlet;

import com.beat.observability.logging.filter.BaseMdcLoggingFilter;
import com.beat.observability.tracing.TraceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityMdcLoggingFilter extends BaseMdcLoggingFilter {

	private final int managementPort;

	public SecurityMdcLoggingFilter(TraceContextResolver traceContextResolver) {
		this(traceContextResolver, -1);
	}

	public SecurityMdcLoggingFilter(TraceContextResolver traceContextResolver, int managementPort) {
		super(traceContextResolver);
		this.managementPort = managementPort;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return managementPort > 0 && request.getLocalPort() == managementPort;
	}

	@Override
	protected String resolveUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof Long memberId) {
			return memberId.toString();
		}

		String name = authentication.getName();
		return name == null || name.isBlank() ? null : name;
	}
}
