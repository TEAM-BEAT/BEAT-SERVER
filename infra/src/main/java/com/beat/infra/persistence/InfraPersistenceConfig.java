package com.beat.infra.persistence;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = InfraPersistenceMarker.class)
public class InfraPersistenceConfig {
}
