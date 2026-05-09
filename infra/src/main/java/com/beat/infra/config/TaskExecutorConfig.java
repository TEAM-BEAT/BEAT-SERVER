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

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class TaskExecutorConfig {

	private final ThreadPoolProperties threadPoolProperties;
	private final ObjectProvider<TaskDecorator> taskDecorators;

	public TaskExecutorConfig(
		ThreadPoolProperties threadPoolProperties,
		ObjectProvider<TaskDecorator> taskDecorators
	) {
		this.threadPoolProperties = threadPoolProperties;
		this.taskDecorators = taskDecorators;
	}

	@Bean(name = "beatApplicationTaskExecutor")
	public ThreadPoolTaskExecutor beatApplicationTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threadPoolProperties.getCoreSize());
		executor.setMaxPoolSize(Math.max(threadPoolProperties.getMaxPoolSize(), threadPoolProperties.getCoreSize()));
		executor.setQueueCapacity(threadPoolProperties.getQueueCapacity());
		executor.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		applyTaskDecorator(executor);
		executor.initialize();
		return executor;
	}

	private void applyTaskDecorator(ThreadPoolTaskExecutor executor) {
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
