package com.beat.apis.config

import com.beat.contracts.schedule.ScheduleBookingCloseJobPort
import com.beat.contracts.schedule.ScheduleBookingCloseJobTarget
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class NoOpScheduleBookingCloseJobConfig {

    @Bean
    @ConditionalOnMissingBean(ScheduleBookingCloseJobPort::class)
    fun scheduleBookingCloseJobPort(): ScheduleBookingCloseJobPort = NoOpScheduleBookingCloseJobPort
}

private object NoOpScheduleBookingCloseJobPort : ScheduleBookingCloseJobPort {

    private val log = LoggerFactory.getLogger(NoOpScheduleBookingCloseJobPort::class.java)

    override fun registerOrRefresh(target: ScheduleBookingCloseJobTarget) {
        log.debug("Skipping schedule booking close job registration in apis runtime for scheduleId={}", target.scheduleId)
    }

    override fun cancel(target: ScheduleBookingCloseJobTarget) {
        log.debug("Skipping schedule booking close job cancellation in apis runtime for scheduleId={}", target.scheduleId)
    }
}
