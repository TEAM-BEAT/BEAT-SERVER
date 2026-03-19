package com.beat.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Setter-based binding is kept intentionally during the transition baseline.
 * This avoids constructor binding friction while infra config ownership is still moving across modules.
 */
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {
	private int coreSize;
	private String threadNamePrefix;

	public ThreadPoolProperties() {
	}

	public int getCoreSize() {
		return coreSize;
	}

	public void setCoreSize(int coreSize) {
		this.coreSize = coreSize;
	}

	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}
}
