package com.beat.gateway.authentication.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beat.gateway.CurrentMember;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentMemberArgumentResolverTest {

	private final CurrentMemberArgumentResolver resolver = new CurrentMemberArgumentResolver();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void supportsBoxedAndPrimitiveLongMemberIds() throws NoSuchMethodException {
		assertTrue(resolver.supportsParameter(parameter("boxedMemberId")));
		assertTrue(resolver.supportsParameter(parameter("primitiveMemberId")));
		assertFalse(resolver.supportsParameter(parameter("unsupportedMemberId")));
	}

	@Test
	void resolvesAuthenticatedMemberIdForPrimitiveLong() throws NoSuchMethodException {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(2L, null, List.of())
		);

		assertEquals(2L, resolver.resolveArgument(parameter("primitiveMemberId"), null, null, null));
	}

	@Test
	void returnsNullForUnauthenticatedBoxedLong() throws NoSuchMethodException {
		assertNull(resolver.resolveArgument(parameter("boxedMemberId"), null, null, null));
	}

	@Test
	void returnsNullForAnonymousBoxedLong() throws NoSuchMethodException {
		SecurityContextHolder.getContext().setAuthentication(
			new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))
		);

		assertNull(resolver.resolveArgument(parameter("boxedMemberId"), null, null, null));
	}

	@Test
	void rejectsUnauthenticatedPrimitiveLong() throws NoSuchMethodException {
		assertThrows(
			IllegalStateException.class,
			() -> resolver.resolveArgument(parameter("primitiveMemberId"), null, null, null)
		);
	}

	private MethodParameter parameter(String methodName) throws NoSuchMethodException {
		Method method = Handler.class.getDeclaredMethod(methodName, methodParameterType(methodName));
		return new MethodParameter(method, 0);
	}

	private Class<?> methodParameterType(String methodName) {
		if ("primitiveMemberId".equals(methodName)) {
			return long.class;
		}
		if ("unsupportedMemberId".equals(methodName)) {
			return String.class;
		}
		return Long.class;
	}

	private static class Handler {

		void boxedMemberId(@CurrentMember Long memberId) {
		}

		void primitiveMemberId(@CurrentMember long memberId) {
		}

		void unsupportedMemberId(@CurrentMember String memberId) {
		}
	}
}
