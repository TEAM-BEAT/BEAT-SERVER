package com.beat.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Setter-based binding is kept intentionally during the transition baseline.
 * This avoids constructor binding friction while infra config ownership is still moving across modules.
 */
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {
	private int coreSize = 2;

	private int maxPoolSize = 4;

	private int queueCapacity = 50;

	private String threadNamePrefix = "executor-";

	public ThreadPoolProperties() {
	}

	public int getCoreSize() {
		return coreSize;
	}

	public void setCoreSize(int coreSize) {
		this.coreSize = Math.max(coreSize, 1);
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = Math.max(maxPoolSize, 1);
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = Math.max(queueCapacity, 1);
	}

	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		if (StringUtils.hasText(threadNamePrefix)) {
			this.threadNamePrefix = threadNamePrefix;
		}
	}
}
