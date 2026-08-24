package com.beat.infrastructure.config

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
internal class JooqConfig {

    @Bean
    fun dslContext(dataSource: DataSource): DSLContext =
        DSL.using(TransactionAwareDataSourceProxy(dataSource), SQLDialect.MYSQL)
}
