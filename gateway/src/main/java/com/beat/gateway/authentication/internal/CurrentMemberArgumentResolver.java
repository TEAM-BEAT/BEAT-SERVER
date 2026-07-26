package com.beat.gateway.authentication.internal;

import com.beat.gateway.CurrentMember;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentMember.class)
			&& isMemberIdType(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (isUnauthenticated(authentication)) {
			return missingMemberId(parameter);
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof Long memberId) {
			return memberId;
		}
		throw new IllegalStateException("Current member principal must be a Long");
	}

	private static boolean isMemberIdType(Class<?> parameterType) {
		return parameterType == Long.class || parameterType == long.class;
	}

	private static boolean isUnauthenticated(Authentication authentication) {
		return authentication == null
			|| !authentication.isAuthenticated()
			|| authentication instanceof AnonymousAuthenticationToken;
	}

	private static Long missingMemberId(MethodParameter parameter) {
		if (parameter.getParameterType() == long.class) {
			throw new IllegalStateException("A non-null @CurrentMember parameter requires authentication");
		}
		return null;
	}
}
