package com.beat.infra.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.beat.infra.InfraBaseConfig;
import com.beat.infra.persistence.InfraPersistenceConfig;
import com.beat.infra.persistence.InfraPersistenceMarker;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
@EntityScan(basePackages = "com.beat.domain", basePackageClasses = InfraPersistenceMarker.class)
@EnableJpaRepositories(basePackages = "com.beat.domain", basePackageClasses = InfraPersistenceMarker.class)
@Import(InfraPersistenceConfig.class)
public class JpaConfig implements InfraBaseConfig {
}
