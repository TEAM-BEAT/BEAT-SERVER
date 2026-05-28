package com.beat.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

import com.beat.infra.InfraBaseConfig;

// Spring Boot 4.0 의 TaskExecutionAutoConfiguration 이 우리 applicationTaskExecutor
// bean 을 @ConditionalOnMissingBean(Executor.class) 로 감지 → 자체 default executor 생성을
// backoff. 그리고 AsyncConfigurerConfiguration 의 ApplicationTaskExecutorAsyncConfigurer 가
// `applicationTaskExecutor` 라는 이름의 Executor bean 을 lookup 해 @Async 의 default
// executor 로 자동 wiring. 즉 user @Bean AsyncConfigurer 등록 없이도 BEAT 의 executor 가
// 그대로 사용됨. AsyncUncaughtExceptionHandler 는 framework default
// (SimpleAsyncUncaughtExceptionHandler) 사용.
@Configuration(proxyBeanMethods = false)
@EnableAsync
@Import(TaskExecutorConfig.class)
public class AsyncConfig implements InfraBaseConfig {
}
