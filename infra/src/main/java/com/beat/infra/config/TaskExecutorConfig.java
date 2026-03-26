package com.beat.infra.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class TaskExecutorConfig {

	private final ThreadPoolProperties threadPoolProperties;

	public TaskExecutorConfig(ThreadPoolProperties threadPoolProperties) {
		this.threadPoolProperties = threadPoolProperties;
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
		executor.initialize();
		return executor;
	}
}
