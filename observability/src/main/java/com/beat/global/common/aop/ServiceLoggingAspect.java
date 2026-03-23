package com.beat.global.common.aop;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(3)
@Component
@Profile("!test")
public class ServiceLoggingAspect {

	private static final Set<String> SENSITIVE_KEYS = Set.of(
		"accessToken",
		"refreshToken",
		"authorizationCode",
		"token",
		"jwt",
		"password",
		"secret"
	);

	/** Service 메서드 실행 전 로깅 */
	@Before("com.beat.global.common.aop.Pointcuts.allService()")
	public void doLog(JoinPoint joinPoint) {
		log.info("[메서드 실행] {}.{}() | 인자: {}",
			joinPoint.getSignature().getDeclaringType().getSimpleName(),
			joinPoint.getSignature().getName(),
			buildMaskedArgs(joinPoint));
	}

	/** Service 정상 반환 로깅 */
	@AfterReturning(value = "com.beat.global.common.aop.Pointcuts.allService()", returning = "result")
	public void logReturn(JoinPoint joinPoint, Object result) {
		log.debug("[Service 정상 반환] {}.{}() | 반환 타입: {}",
			joinPoint.getSignature().getDeclaringType().getSimpleName(),
			joinPoint.getSignature().getName(),
			result != null ? result.getClass().getSimpleName() : "null");
	}

	/** 예외 발생 시 로깅 */
	@AfterThrowing(value = "com.beat.global.common.aop.Pointcuts.allService()", throwing = "ex")
	public void logException(JoinPoint joinPoint, Exception ex) {
		log.error("[예외 발생] {}.{}() | 예외 메시지: {}",
			joinPoint.getSignature().getDeclaringType().getSimpleName(),
			joinPoint.getSignature().getName(),
			ex.getMessage());
	}

	/** 메서드 실행 후 로깅 */
	@After("com.beat.global.common.aop.Pointcuts.allService()")
	public void doAfter(JoinPoint joinPoint) {
		log.info("[메서드 종료] {}.{}()",
			joinPoint.getSignature().getDeclaringType().getSimpleName(),
			joinPoint.getSignature().getName());
	}

	private Map<String, Object> buildMaskedArgs(JoinPoint joinPoint) {
		CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
		String[] parameterNames = codeSignature.getParameterNames();
		Object[] args = joinPoint.getArgs();

		Map<String, Object> maskedArgs = new LinkedHashMap<>();

		for (int index = 0; index < parameterNames.length; index++) {
			String parameterName = parameterNames[index];
			Object argumentValue = args[index];
			maskedArgs.put(parameterName, maskIfSensitive(parameterName, argumentValue));
		}

		return maskedArgs;
	}

	private Object maskIfSensitive(String key, Object value) {
		if (value == null) {
			return null;
		}

		String normalizedKey = key == null ? "" : key.toLowerCase();

		boolean isSensitive = SENSITIVE_KEYS.stream()
			.map(String::toLowerCase)
			.anyMatch(normalizedKey::contains);

		if (isSensitive) {
			return "***";
		}

		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			return "Array(length=" + length + ")";
		}

		return value;
	}
}
