package com.beat.infra.config;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;

// Spring Boot 4.0 의 TaskExecutionAutoConfiguration 이 `applicationTaskExecutor`
// 라는 이름의 ThreadPoolTaskExecutor 를 자동 등록. 우리는 그 default executor 의
// 설정만 Customizer 로 override → bean 이름 충돌 없이 BEAT 의 thread pool 설정 + TaskDecorator
// 가 그대로 적용되고, ApplicationTaskExecutorAsyncConfigurer 의 lookup 도 그대로 작동.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class TaskExecutorConfig {

	@Bean
	ThreadPoolTaskExecutorCustomizer beatThreadPoolTaskExecutorCustomizer(
		ThreadPoolProperties threadPoolProperties,
		ObjectProvider<TaskDecorator> taskDecorators
	) {
		return executor -> {
			executor.setCorePoolSize(threadPoolProperties.getCoreSize());
			executor.setMaxPoolSize(
				Math.max(threadPoolProperties.getMaxPoolSize(), threadPoolProperties.getCoreSize()));
			executor.setQueueCapacity(threadPoolProperties.getQueueCapacity());
			executor.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());
			executor.setWaitForTasksToCompleteOnShutdown(true);
			executor.setAwaitTerminationSeconds(30);
			executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
			applyTaskDecorator(executor, taskDecorators);
		};
	}

	private static void applyTaskDecorator(
		org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor,
		ObjectProvider<TaskDecorator> taskDecorators
	) {
		List<TaskDecorator> decorators = taskDecorators.orderedStream().toList();
		if (decorators.isEmpty()) {
			return;
		}
		if (decorators.size() == 1) {
			executor.setTaskDecorator(decorators.get(0));
			return;
		}
		executor.setTaskDecorator(new CompositeTaskDecorator(decorators));
	}
}
