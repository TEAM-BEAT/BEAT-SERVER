package com.beat.global.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableJpaRepositories(
	basePackages = "com.beat",
	excludeFilters = @ComponentScan.Filter(
		type = FilterType.REGEX,
		pattern = "com\\.beat\\.global\\.auth\\.jwt\\.dao\\..*"
	)
)
@EnableRedisRepositories(
	basePackages = "com.beat.global.auth.jwt.dao"
)
public class RepositoryConfig {
}
