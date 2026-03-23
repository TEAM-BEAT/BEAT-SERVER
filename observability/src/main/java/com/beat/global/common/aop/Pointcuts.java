package com.beat.global.common.aop;

import org.aspectj.lang.annotation.Pointcut;

public class Pointcuts {
	@Pointcut("execution(* com.beat..*Controller.*(..))")
	public void allController() {}

	@Pointcut("execution(* com.beat..*Service.*(..)) || execution(* com.beat..*UseCase.*(..)) || execution(* com.beat..*Facade.*(..))")
	public void allService() {}

	@Pointcut("execution(* com.beat..*(..))" +
		" && !within(com.beat.global..*)")
	public void allApplicationLogic() {}
}
