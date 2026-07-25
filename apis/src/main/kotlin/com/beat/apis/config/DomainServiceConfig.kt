package com.beat.apis.config

import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class DomainServiceConfig {
    @Bean
    fun scheduleSequenceDomainService(): ScheduleSequenceDomainService = ScheduleSequenceDomainService()
}
