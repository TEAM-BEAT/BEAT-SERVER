package com.beat.application.frontoffice

import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionOperations
import org.springframework.transaction.support.TransactionTemplate

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = [FrontofficeApplicationConfig::class])
class FrontofficeApplicationConfig {
    @Bean
    fun scheduleSequenceDomainService(): ScheduleSequenceDomainService = ScheduleSequenceDomainService()

    @Bean
    fun transactionOperations(transactionManager: PlatformTransactionManager): TransactionOperations =
        TransactionTemplate(transactionManager)
}
