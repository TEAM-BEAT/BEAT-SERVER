package com.beat.apis.config

import com.beat.domain.booking.api.BookingController
import com.beat.domain.booking.application.TicketService
import com.beat.domain.member.api.MemberController
import com.beat.domain.member.application.MemberService
import com.beat.domain.performance.api.PerformanceController
import com.beat.domain.performance.application.PerformanceService
import com.beat.domain.promotion.application.PromotionService
import com.beat.domain.schedule.api.ScheduleController
import com.beat.domain.schedule.application.ScheduleService
import com.beat.domain.user.api.HealthCheckController
import com.beat.domain.user.application.UserService
import com.beat.global.external.notification.slack.event.BookingCreatedEventListener
import com.beat.global.external.s3.api.FileController
import com.beat.global.common.scheduler.application.JobSchedulerService
import com.beat.global.swagger.config.SwaggerConfig
import com.beat.domain.booking.application.TicketCleanupScheduler
import com.beat.domain.promotion.application.PromotionSchedulerService
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

@Configuration(proxyBeanMethods = false)
@ComponentScan(
    basePackageClasses = [
        BookingController::class,
        TicketService::class,
        MemberController::class,
        MemberService::class,
        PerformanceController::class,
        PerformanceService::class,
        PromotionService::class,
        ScheduleController::class,
        ScheduleService::class,
        HealthCheckController::class,
        UserService::class,
        FileController::class,
        BookingCreatedEventListener::class,
        JobSchedulerService::class,
        SwaggerConfig::class,
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [
                TicketCleanupScheduler::class,
                PromotionSchedulerService::class,
            ],
        ),
    ],
)
class ApisBootstrapConfig
