package com.beat.application.frontoffice

import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = [FrontofficeApplicationConfig::class])
class FrontofficeApplicationConfig {
    @Bean
    fun scheduleSequenceDomainService(): ScheduleSequenceDomainService = ScheduleSequenceDomainService()
}
