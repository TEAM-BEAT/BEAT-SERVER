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
import com.beat.global.swagger.config.SwaggerConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

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
        SwaggerConfig::class,
    ],
)
class ApisBootstrapConfig
