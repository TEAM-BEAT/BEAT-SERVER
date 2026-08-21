package com.beat.apis

import com.beat.apis.support.AbstractIntegrationTest
import com.beat.application.frontoffice.booking.booker.command.MemberBookingCommandService
import com.beat.application.frontoffice.booking.booker.query.BookerBookingReader
import com.beat.application.frontoffice.auth.command.RefreshTokenStore
import com.beat.application.frontoffice.member.command.MemberRegistrationNotifier
import com.beat.application.frontoffice.member.command.SocialLoginProvider
import com.beat.application.frontoffice.performance.maker.command.PerformanceImageStorage
import com.beat.application.frontoffice.ticket.maker.command.TicketCommandService
import com.beat.application.frontoffice.ticket.maker.query.TicketQueryService
import com.beat.contracts.auth.guest.GuestAccessThrottlePort
import com.beat.contracts.auth.guest.GuestSessionPort
import com.beat.contracts.cdn.ImageCachePort
import com.beat.contracts.notification.BookingNotificationPort
import com.beat.contracts.storage.FileStoragePort
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.infra.external.notification.slack.SlackBookingNotificationAdapter
import com.beat.infra.redis.auth.guest.RedisGuestAccessThrottleAdapter
import com.beat.infra.redis.auth.guest.RedisGuestSessionAdapter
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@Tag("integration")
class ApisModuleContextBootTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun contextLoadsWithoutBatchScheduler() {
        assertEquals(1, applicationContext.getBeansOfType(GroupedOpenApi::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(OpenAPI::class.java).size)
        assertTrue(applicationContext.containsBean("generalApi"))
        assertFalse(applicationContext.containsBean("adminApi"))
        assertFalse(applicationContext.containsBean("jobSchedulerService"))
        assertTrue(applicationContext.getBeansOfType(TaskScheduler::class.java).isEmpty())
        assertEquals(1, applicationContext.getBeansOfType(PerformanceRepository::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(PromotionRepository::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(ScheduleRepository::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(MemberBookingCommandService::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(BookerBookingReader::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(TicketCommandService::class.java).size)
        assertEquals(1, applicationContext.getBeansOfType(TicketQueryService::class.java).size)
    }

    @Test
    fun registersSelectedInfraBeans() {
        assertEquals(1, applicationContext.getBeansOfType(RefreshTokenStore::class.java).size)
        assertSame(
            applicationContext.getBean(RedisGuestSessionAdapter::class.java),
            applicationContext.getBean(GuestSessionPort::class.java),
        )
        assertSame(
            applicationContext.getBean(RedisGuestAccessThrottleAdapter::class.java),
            applicationContext.getBean(GuestAccessThrottlePort::class.java),
        )
        assertEquals(1, applicationContext.getBeansOfType(SocialLoginProvider::class.java).size)
        assertSame(
            applicationContext.getBean(SlackBookingNotificationAdapter::class.java),
            applicationContext.getBean(BookingNotificationPort::class.java),
        )
        assertEquals(1, applicationContext.getBeansOfType(MemberRegistrationNotifier::class.java).size)
        assertNotNull(applicationContext.getBean(PerformanceImageStorage::class.java))
        assertNotNull(applicationContext.getBean(FileStoragePort::class.java))
        assertNotNull(applicationContext.getBean(ImageCachePort::class.java))
        assertSame(
            applicationContext.getBean(PerformanceImageStorage::class.java),
            applicationContext.getBean(FileStoragePort::class.java),
        )

        val script = applicationContext.getBean("recordGuestAccessFailureScript", RedisScript::class.java)
        assertEquals(Long::class.javaObjectType, script.resultType)
        assertTrue(script.scriptAsString.contains("redis.call('INCR'"))
    }

    @Test
    fun servesGroupedSwaggerDocsForGeneralApis() {
        mockMvc.perform(get("/v3/api-docs/general"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.paths").exists())
    }
}
