package com.beat.infra.config

import com.beat.infra.InfraBaseConfig
import com.beat.infra.persistence.InfraPersistenceConfig
import com.beat.infra.persistence.InfraPersistenceMarker
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
@EntityScan(basePackageClasses = [InfraPersistenceMarker::class])
@EnableJpaRepositories(basePackageClasses = [InfraPersistenceMarker::class])
@Import(InfraPersistenceConfig::class)
internal class JpaConfig : InfraBaseConfig {
    @Bean
    fun jpqlRenderContext(): JpqlRenderContext = JpqlRenderContext()
}
