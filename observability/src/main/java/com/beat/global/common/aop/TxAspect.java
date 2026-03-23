package com.beat.global.common.aop;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(1)
@Component
@Profile("!test")
public class TxAspect {

	@Around("com.beat.global.common.aop.Pointcuts.allService()")
	public Object doTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
		// 메서드 정보를 추출
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		Method method = methodSignature.getMethod();

		// @Transactional 애너테이션 정보 확인 (메서드 혹은 클래스 레벨)
		Transactional transactional = method.getAnnotation(Transactional.class);
		if (transactional == null) {
			transactional = joinPoint.getTarget().getClass().getAnnotation(Transactional.class);
		}

		boolean readOnly = false;
		Propagation propagation = Propagation.REQUIRED;
		Isolation isolation = Isolation.DEFAULT;
		if (transactional != null) {
			readOnly = transactional.readOnly();
			propagation = transactional.propagation();
			isolation = transactional.isolation();
		}

		// 트랜잭션 시작 로깅 (옵션 포함)
		log.info("[트랜잭션 시작] {}.{}() | readOnly={} | propagation={} | isolation={}",
			joinPoint.getSignature().getDeclaringType().getSimpleName(),
			method.getName(), readOnly, propagation, isolation);

		// 실행 시간 측정을 위한 시작 시간
		long start = System.currentTimeMillis();
		try {
			// 실제 비즈니스 로직 실행
			Object result = joinPoint.proceed();
			long elapsed = System.currentTimeMillis() - start;
			log.info("[트랜잭션 커밋] {}.{}() | 소요 시간: {}ms",
				joinPoint.getSignature().getDeclaringType().getSimpleName(),
				method.getName(), elapsed);
			return result;
		} catch (Exception e) {
			long elapsed = System.currentTimeMillis() - start;
			log.error("[트랜잭션 롤백] {}.{}() | 소요 시간: {}ms",
				joinPoint.getSignature().getDeclaringType().getSimpleName(),
				method.getName(), elapsed, e);
			throw e;
		} finally {
			// 리소스 릴리즈 로깅
			log.info("[리소스 릴리즈] {}.{}()",
				joinPoint.getSignature().getDeclaringType().getSimpleName(),
				method.getName());
		}
	}
}
