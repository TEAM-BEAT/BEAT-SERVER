package com.beat.batch.config

import com.beat.domain.booking.application.TicketCleanupScheduler
import com.beat.domain.promotion.application.PromotionSchedulerService
import com.beat.global.common.scheduler.application.JobSchedulerTransactionalService
import com.beat.global.common.scheduler.application.JobSchedulerService
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(
    JobSchedulerService::class,
    JobSchedulerTransactionalService::class,
    TicketCleanupScheduler::class,
    PromotionSchedulerService::class,
)
class BatchSchedulerBootstrapConfig
