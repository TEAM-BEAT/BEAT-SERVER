package com.beat.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.beat.infra.InfraBaseConfig;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class TaskSchedulerConfig implements InfraBaseConfig {

	private final ThreadPoolProperties threadPoolProperties;

	public TaskSchedulerConfig(ThreadPoolProperties threadPoolProperties) {
		this.threadPoolProperties = threadPoolProperties;
	}

	@Bean(name = "taskScheduler")
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(threadPoolProperties.getCoreSize());
		scheduler.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix() + "scheduler-");
		scheduler.initialize();
		return scheduler;
	}
}
