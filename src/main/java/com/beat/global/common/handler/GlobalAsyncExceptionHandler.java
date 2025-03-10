package com.beat.global.common.handler;

import java.lang.reflect.Method;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

	/**
	 * Handles uncaught exceptions thrown during asynchronous method execution.
	 *
	 * This method logs an error message with the method name and exception details. If the provided
	 * parameters array is null, it logs that no parameters were provided; otherwise, it logs the string
	 * representation of the parameters.
	 *
	 * @param ex the exception that occurred (non-null)
	 * @param method the method where the exception was thrown (non-null)
	 * @param params the parameters passed to the method; may be null
	 */
	@Override
	public void handleUncaughtException(@NonNull Throwable ex, @NonNull Method method, @Nullable Object... params) {
		if (params == null) {
			log.error("비동기 작업 중 예외 발생! Method: [{}], Params: [null], Exception: [{}]",
				method.getName(), ex.getMessage(), ex);
			return;
		}

		String paramValues = java.util.Arrays.toString(params);

		log.error("비동기 작업 중 예외 발생! Method: [{}], Params: [{}], Exception: [{}]",
			method.getName(), paramValues, ex.getMessage(), ex);
	}
}
