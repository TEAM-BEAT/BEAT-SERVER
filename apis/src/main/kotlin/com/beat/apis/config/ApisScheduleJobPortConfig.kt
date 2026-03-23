package com.beat.apis.config

import com.beat.contracts.schedule.ScheduleJobPort
import com.beat.domain.schedule.domain.Schedule
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ApisScheduleJobPortConfig {

    @Bean
    @ConditionalOnProperty(name = ["beat.scheduler.owner"], havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(ScheduleJobPort::class)
    fun scheduleJobPort(): ScheduleJobPort = NonOwnerScheduleJobPort
}

private object NonOwnerScheduleJobPort : ScheduleJobPort {

    private val log = LoggerFactory.getLogger(NonOwnerScheduleJobPort::class.java)

    override fun registerOrRefresh(schedule: Schedule) {
        log.debug("Skipping schedule registration in apis runtime for scheduleId={}", schedule.id)
    }

    override fun cancel(schedule: Schedule) {
        log.debug("Skipping schedule cancellation in apis runtime for scheduleId={}", schedule.id)
    }
}
