package com.beat.observability.aop;

import org.aspectj.lang.annotation.Pointcut;

public class Pointcuts {
	@Pointcut("execution(* com.beat..*Controller.*(..))")
	public void allController() {
	}

	@Pointcut("execution(* com.beat..*Service.*(..)) || execution(* com.beat..*UseCase.*(..)) || execution(* com.beat..*Facade.*(..))")
	public void allService() {
	}

	@Pointcut("execution(* com.beat..*(..))" +
		" && !within(com.beat.global..*)" +
		" && !within(com.beat.observability..*)")
	public void allApplicationLogic() {
	}
}
