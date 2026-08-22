package com.beat.apis

import com.beat.application.frontoffice.booking.booker.command.MemberBookingCommandService
import com.beat.application.frontoffice.booking.booker.query.BookerBookingReader
import com.beat.application.frontoffice.ticket.maker.command.TicketCommandService
import com.beat.application.frontoffice.ticket.maker.query.TicketQueryService
import com.beat.apis.support.BeatAcceptanceTest
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.schedule.repository.ScheduleRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springdoc.core.models.GroupedOpenApi
import io.swagger.v3.oas.models.OpenAPI

@BeatAcceptanceTest
@Tags("acceptance")
open class ApisModuleContextBootSpec : FunSpec() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("api context wires general OpenAPI and frontoffice booking/ticket capabilities without admin or batch") {
            applicationContext.getBeansOfType(GroupedOpenApi::class.java).size shouldBe 1
            applicationContext.getBeansOfType(OpenAPI::class.java).size shouldBe 1
            applicationContext.containsBean("generalApi") shouldBe true
            applicationContext.containsBean("adminApi") shouldBe false
            applicationContext.containsBean("jobSchedulerService") shouldBe false
            applicationContext.getBeansOfType(TaskScheduler::class.java).isEmpty() shouldBe true

            applicationContext.getBeansOfType(PerformanceRepository::class.java).size shouldBe 1
            applicationContext.getBeansOfType(PromotionRepository::class.java).size shouldBe 1
            applicationContext.getBeansOfType(ScheduleRepository::class.java).size shouldBe 1
            applicationContext.getBeansOfType(MemberBookingCommandService::class.java).size shouldBe 1
            applicationContext.getBeansOfType(BookerBookingReader::class.java).size shouldBe 1
            applicationContext.getBeansOfType(TicketCommandService::class.java).size shouldBe 1
            applicationContext.getBeansOfType(TicketQueryService::class.java).size shouldBe 1
        }

        test("serves the general OpenAPI document") {
            mockMvc.perform(get("/v3/api-docs/general"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths").exists())
        }
    }
}
