package com.beat.infrastructure.config

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

/**
 * Provides DSLContext for infrastructure jOOQ View adapters.
 * Production: Spring Boot JooqAutoConfiguration also creates DSLContext via
 * `spring.jooq.sql-dialect=MYSQL` + TransactionAwareDataSourceProxy.
 * This bean is kept with @ConditionalOnMissingBean to satisfy @DataJpaTest slices
 * (which do not load full Boot auto-config) while preferring Boot's auto-config
 * in full application context. See Mission 17.
 */
@Configuration(proxyBeanMethods = false)
internal class JooqConfig {

    @Bean
    @ConditionalOnMissingBean
    fun dslContext(dataSource: DataSource): DSLContext =
        DSL.using(TransactionAwareDataSourceProxy(dataSource), SQLDialect.MYSQL)
}
