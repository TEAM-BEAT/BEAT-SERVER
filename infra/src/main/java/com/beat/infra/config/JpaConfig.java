package com.beat.infra.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.beat.infra.InfraBaseConfig;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
@EntityScan("com.beat")
@EnableJpaRepositories(
	basePackages = "com.beat",
	excludeFilters = @ComponentScan.Filter(
		type = FilterType.REGEX,
		pattern = "com\\.beat\\.global\\.auth\\.jwt\\.dao\\..*"
	)
)
public class JpaConfig implements InfraBaseConfig {
}
