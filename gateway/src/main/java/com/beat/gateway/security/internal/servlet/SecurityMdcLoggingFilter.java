package com.beat.gateway.security.internal.servlet;

import com.beat.observability.logging.filter.BaseMdcLoggingFilter;
import io.micrometer.tracing.Tracer;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityMdcLoggingFilter extends BaseMdcLoggingFilter {

	public SecurityMdcLoggingFilter() {
		super(null);
	}

	public SecurityMdcLoggingFilter(@Nullable Tracer tracer) {
		super(tracer);
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
