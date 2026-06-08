package com.beat.infra.config;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// BEAT 도메인 비동기 작업 전용 executor.
// Spring Boot가 자동 구성하는 applicationTaskExecutor는 MVC/JPA/WebSocket 등 프레임워크 통합용으로 유지하고,
// 비즈니스 @Async 작업은 @Async("beatAsyncExecutor")로 명시해 별도 풀에서 실행한다.
// defaultCandidate=false를 통해 타입 기반 기본 주입 후보에서는 제외하고, 명시적 이름/qualifier로만 사용되게 한다.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class TaskExecutorConfig {

	@Bean(name = "beatAsyncExecutor", defaultCandidate = false)
	public ThreadPoolTaskExecutor beatAsyncExecutor(
		ThreadPoolProperties threadPoolProperties,
		ObjectProvider<TaskDecorator> taskDecorators
	) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threadPoolProperties.getCoreSize());
		executor.setMaxPoolSize(
			Math.max(threadPoolProperties.getMaxPoolSize(), threadPoolProperties.getCoreSize()));
		executor.setQueueCapacity(threadPoolProperties.getQueueCapacity());
		executor.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		applyTaskDecorator(executor, taskDecorators);
		executor.initialize();
		return executor;
	}

	private static void applyTaskDecorator(
		ThreadPoolTaskExecutor executor,
		ObjectProvider<TaskDecorator> taskDecorators
	) {
		List<TaskDecorator> decorators = taskDecorators.orderedStream().toList();
		if (decorators.isEmpty()) {
			return;
		}
		if (decorators.size() == 1) {
			executor.setTaskDecorator(decorators.getFirst());
			return;
		}
		executor.setTaskDecorator(new CompositeTaskDecorator(decorators));
	}
}
