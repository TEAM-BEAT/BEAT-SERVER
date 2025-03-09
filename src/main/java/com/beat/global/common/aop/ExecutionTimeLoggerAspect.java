package com.beat.global.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@Order(0)
public class ExecutionTimeLoggerAspect {

	private ExecutionTimeLoggerAspect() {
	}

	/** prod 환경에서는 서비스 계층만 실행 시간 측정 */
	@Aspect
	@Component
	@Profile("prod")
	public static class ExecutionTimeLoggerForProd {
		@Around("com.beat.global.common.aop.Pointcuts.allService()")
		public Object logServiceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
			return measureExecutionTime(joinPoint);
		}
	}

	/** local/dev 환경에서는 전체 애플리케이션 로직 실행 시간 측정 */
	@Aspect
	@Component
	@Profile({"local", "dev"})
	public static class ExecutionTimeLoggerForLocalDev {
		@Around("com.beat.global.common.aop.Pointcuts.allApplicationLogic()")
		public Object logApplicationExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
			return measureExecutionTime(joinPoint);
		}
	}

	/** 실행 시간 측정 공통 메서드 */
	private static Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();
		try {
			return joinPoint.proceed();
		} finally {
			long timeInMs = System.currentTimeMillis() - start;
			log.info("[실행 시간] {}.{}() | time = {}ms",
				joinPoint.getSignature().getDeclaringType().getSimpleName(),
				joinPoint.getSignature().getName(),
				timeInMs);
		}
	}
}
