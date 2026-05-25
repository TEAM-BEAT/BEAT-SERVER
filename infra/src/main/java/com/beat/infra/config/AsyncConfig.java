package com.beat.infra.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.beat.infra.InfraBaseConfig;

// AsyncConfigurer 를 클래스가 직접 implements 하면 Spring Boot 4.0 의
// AsyncConfigurerWrapperConfiguration BeanPostProcessor 가 @Configuration 인스턴스를
// wrap 하면서 원본 + wrapper 가 동시에 컨테이너에 남아 "Only one AsyncConfigurer may exist"
// 가 발생. AsyncConfigurer 는 별도 @Bean 으로 분리해 BeanPostProcessor 가 깔끔하게
// 단일 인스턴스로 wrap 하도록 유도.
@Configuration(proxyBeanMethods = false)
@EnableAsync
@Import({TaskExecutorConfig.class, AsyncConfigurerDiagnostic.class})
public class AsyncConfig implements InfraBaseConfig {

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	@Bean
	AsyncConfigurer beatAsyncConfigurer(
		@Qualifier("beatApplicationTaskExecutor") ThreadPoolTaskExecutor applicationTaskExecutor
	) {
		return new AsyncConfigurer() {
			@Override
			public Executor getAsyncExecutor() {
				return applicationTaskExecutor;
			}

			@Override
			public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
				return (ex, method, params) ->
					log.error(formatAsyncExceptionMessage(method, params, ex), ex);
			}
		};
	}

	static String formatAsyncExceptionMessage(Method method, @Nullable Object[] params, Throwable ex) {
		return "비동기 작업 중 예외 발생! Method: [%s], Params: [%s], Exception: [%s]"
			.formatted(method.getName(), formatAsyncParameters(params), ex.getMessage());
	}

	static String formatAsyncParameters(@Nullable Object[] params) {
		if (params == null) {
			return "null";
		}
		return Arrays.toString(params);
	}
}
