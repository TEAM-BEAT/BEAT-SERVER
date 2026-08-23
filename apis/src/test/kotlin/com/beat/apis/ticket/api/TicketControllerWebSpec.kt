package com.beat.apis.ticket.api

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.apis.schedule.api.type.ScheduleNumberType
import com.beat.apis.ticket.facade.TicketFacade
import com.beat.apis.ticket.api.response.TicketSuccessCode
import com.beat.application.frontoffice.ticket.maker.command.TicketCommandService
import com.beat.application.frontoffice.ticket.maker.query.TicketListQuery
import com.beat.application.frontoffice.ticket.maker.query.TicketQueryService
import com.beat.application.frontoffice.ticket.maker.query.TicketRetrieveResult
import com.beat.support.security.CurrentMember
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

@WebMvcTest(controllers = [TicketController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [TicketController::class, TicketFacade::class, TicketWebTestConfiguration::class])
class TicketControllerWebSpec : FunSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var ticketQueryService: TicketQueryService

    @MockitoBean
    private lateinit var ticketCommandService: TicketCommandService

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            Mockito.reset(ticketQueryService, ticketCommandService)
        }

        test("실제 facade로 매핑된 API 필터로 티켓 목록을 조회한다") {
            val expected = ticketResult()
            val query = TicketListQuery(null, listOf("FIRST"), listOf("CHECKING_PAYMENT"))
            Mockito.`when`(ticketQueryService.findAllTicketsByConditions(MEMBER_ID, PERFORMANCE_ID, query))
                .thenReturn(expected)

            mockMvc.perform(
                get("/api/tickets/{performanceId}", PERFORMANCE_ID)
                    .param("scheduleNumbers", ScheduleNumberType.FIRST.name)
                    .param("bookingStatuses", BookingStatusType.CHECKING_PAYMENT.name),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value(TicketSuccessCode.TICKET_RETRIEVE_SUCCESS.status))
                .andExpect(jsonPath("$.message").value(TicketSuccessCode.TICKET_RETRIEVE_SUCCESS.message))
                .andExpect(jsonPath("$.data.performanceTitle").value("title"))
                .andExpect(jsonPath("$.data.performanceTeamName").value("team"))
                .andExpect(jsonPath("$.data.totalScheduleCount").value(1))
                .andExpect(jsonPath("$.data.totalPerformanceTicketCount").value(100))
                .andExpect(jsonPath("$.data.totalPerformanceSoldTicketCount").value(10))
                .andExpect(jsonPath("$.data.bookingList").isArray)

            Mockito.verify(ticketQueryService).findAllTicketsByConditions(MEMBER_ID, PERFORMANCE_ID, query)
        }

        test("실제 facade로 매핑된 API 필터로 티켓을 검색한다") {
            val expected = ticketResult()
            val query = TicketListQuery("ab", listOf("FIRST"), listOf("CHECKING_PAYMENT"))
            Mockito.`when`(ticketQueryService.searchAllTicketsByConditions(MEMBER_ID, PERFORMANCE_ID, query))
                .thenReturn(expected)

            mockMvc.perform(
                get("/api/tickets/search/{performanceId}", PERFORMANCE_ID)
                    .param("searchWord", "ab")
                    .param("scheduleNumbers", ScheduleNumberType.FIRST.name)
                    .param("bookingStatuses", BookingStatusType.CHECKING_PAYMENT.name),
            )
                .andExpect(status().isOk)
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
                .andExpect(jsonPath("$.status").value(TicketSuccessCode.TICKET_SEARCH_SUCCESS.status))
                .andExpect(jsonPath("$.message").value(TicketSuccessCode.TICKET_SEARCH_SUCCESS.message))
                .andExpect(jsonPath("$.data.performanceTitle").value("title"))
                .andExpect(jsonPath("$.data.performanceTeamName").value("team"))
                .andExpect(jsonPath("$.data.totalScheduleCount").value(1))
                .andExpect(jsonPath("$.data.totalPerformanceTicketCount").value(100))
                .andExpect(jsonPath("$.data.totalPerformanceSoldTicketCount").value(10))
                .andExpect(jsonPath("$.data.bookingList").isArray)

            Mockito.verify(ticketQueryService).searchAllTicketsByConditions(MEMBER_ID, PERFORMANCE_ID, query)
        }
    }

    private fun ticketResult() = TicketRetrieveResult(
        performanceTitle = "title",
        performanceTeamName = "team",
        totalScheduleCount = 1,
        totalPerformanceTicketCount = 100,
        totalPerformanceSoldTicketCount = 10,
        bookingList = emptyList(),
    )

    private companion object {
        const val MEMBER_ID = 1L
        const val PERFORMANCE_ID = 100L
    }
}

@TestConfiguration(proxyBeanMethods = false)
private class TicketWebTestConfiguration : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(FixedCurrentMemberArgumentResolver())
    }
}

private class FixedCurrentMemberArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentMember::class.java) &&
            (
                parameter.parameterType == Long::class.javaObjectType ||
                    parameter.parameterType == Long::class.javaPrimitiveType
                )

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any = 1L
}
