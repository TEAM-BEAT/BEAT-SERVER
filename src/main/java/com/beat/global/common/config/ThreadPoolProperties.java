package com.beat.global.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@ConfigurationProperties(prefix = "thread-pool")
@RequiredArgsConstructor
@Getter
public class ThreadPoolProperties {
	private final int coreSize;
	private final String threadNamePrefix;
}
