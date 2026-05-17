package com.beat.gateway.security.internal.servlet;

import com.beat.observability.logging.filter.BaseMdcLoggingFilter;
import com.beat.observability.tracing.TraceContextResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityMdcLoggingFilter extends BaseMdcLoggingFilter {

	public SecurityMdcLoggingFilter(TraceContextResolver traceContextResolver) {
		super(traceContextResolver);
	}

	@Override
	protected String resolveUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
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
