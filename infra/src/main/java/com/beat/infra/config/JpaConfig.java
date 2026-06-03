package com.beat.infra.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.beat.infra.InfraBaseConfig;
import com.beat.infra.persistence.InfraPersistenceConfig;
import com.beat.infra.persistence.InfraPersistenceMarker;
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
@EntityScan(basePackageClasses = InfraPersistenceMarker.class)
@EnableJpaRepositories(basePackageClasses = InfraPersistenceMarker.class)
@Import(InfraPersistenceConfig.class)
public class JpaConfig implements InfraBaseConfig {

	/**
	 * Kotlin JDSL render context for read/query adapters.
	 *
	 * <p>We depend on {@code hibernate-support} (EntityManager extension only), not the Spring Data
	 * integration module, so no {@code KotlinJdslJpqlExecutor} / auto-configuration is on the classpath.
	 * The render context is immutable and thread-safe, so it is registered once here and reused by every
	 * read/query adapter via the {@code EntityManager + JpqlRenderContext} path.</p>
	 *
	 * <p>Note: this config is wired through {@code @EnableInfraBaseConfig}'s {@code DeferredImportSelector},
	 * which the IntelliJ Spring plugin cannot statically trace. Injecting this bean is therefore reported as
	 * an IDE autowiring false positive ("Could not autowire") at the read/query adapters — it is a warning
	 * only and safe to ignore, not a wiring problem. Runtime wiring is verified by the module context-boot
	 * integration tests.</p>
	 */
	@Bean
	public JpqlRenderContext jpqlRenderContext() {
		return new JpqlRenderContext();
	}
}
