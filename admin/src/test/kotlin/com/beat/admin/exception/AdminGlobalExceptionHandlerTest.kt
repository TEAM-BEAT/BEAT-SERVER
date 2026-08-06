package com.beat.admin.exception

import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.global.support.response.ErrorResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.util.stream.Stream

class AdminGlobalExceptionHandlerTest {
    private val handler = AdminGlobalExceptionHandler()
    private val mockMvc = standaloneSetup(TestController())
        .setControllerAdvice(handler)
        .build()

    @Test
    fun malformedJsonUsesLegacyErrorEnvelope() {
        mockMvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
    }

    @Test
    fun unsupportedMethodPreservesAllowHeader() {
        mockMvc.perform(put("/test"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.status").value(405))
    }

    @Test
    fun unknownRouteKeepsLegacyErrorEnvelope() {
        mockMvc.perform(get("/unknown-route"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
    }

    @Test
    fun missingStaticResourceKeepsEmptyNotFoundBody() {
        val response = handler.handleNoResourceFoundException(
            NoResourceFoundException(HttpMethod.GET, "/missing-resource", "missing-resource"),
            HttpHeaders(),
            HttpStatus.NOT_FOUND,
            ServletWebRequest(MockHttpServletRequest(), MockHttpServletResponse()),
        )

        assertEquals(HttpStatus.NOT_FOUND, response?.statusCode)
        assertNull(response?.body)
    }

    @Test
    fun mapsDomainStateConflictToMatchingHttpAndBodyStatus() {
        val response: ResponseEntity<ErrorResponse> =
            handler.handleDomainException(DomainException(ScheduleErrorCode.INSUFFICIENT_TICKETS))

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(HttpStatus.CONFLICT.value(), response.body!!.status)
        assertEquals(ScheduleErrorCode.INSUFFICIENT_TICKETS.message, response.body!!.message)
    }

    @ParameterizedTest
    @MethodSource("genericV1DomainMappings")
    fun preservesGenericV1DomainContract(errorCode: DomainErrorCode) {
        val response: ResponseEntity<ErrorResponse> = handler.handleDomainException(DomainException(errorCode))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("잘못된 데이터 형식입니다.", response.body!!.message)
    }

    @Test
    fun doesNotExposeUnexpectedExceptionMessage() {
        val response = handler.handleException(
            IllegalArgumentException("internal contract detail"),
            MockHttpServletRequest(),
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.body!!.status)
        assertEquals("서버 내부 오류입니다.", response.body!!.message)
    }

    @Test
    fun delegatesCommittedResponseProtectionToSpring() {
        val servletResponse = MockHttpServletResponse()
        servletResponse.isCommitted = true

        val response = handler.handleExceptionInternal(
            IllegalStateException("already committed"),
            null,
            HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            ServletWebRequest(MockHttpServletRequest(), servletResponse),
        )

        assertNull(response)
    }

    @ParameterizedTest
    @MethodSource("applicationStatusMappings")
    fun mapsEveryApplicationErrorTypeToHttpStatus(type: ApplicationErrorType, expectedStatus: HttpStatus) {
        val response = handler.handleApplicationException(
            AdminApplicationException(TestApplicationErrorCode(type)),
            MockHttpServletRequest(),
        )

        assertEquals(expectedStatus, response.statusCode)
        assertEquals(expectedStatus.value(), response.body!!.status)
    }

    @RestController
    private class TestController {
        @PostMapping("/test")
        fun accept(@RequestBody request: TestRequest) {
        }
    }

    private data class TestRequest(val value: String)

    private data class TestApplicationErrorCode(private val errorType: ApplicationErrorType) : ApplicationErrorCode {
        override fun getCode(): String = "TEST_APPLICATION_ERROR"

        override fun getType(): ApplicationErrorType = errorType

        override fun getMessage(): String = "안전한 테스트 오류입니다."
    }

    companion object {
        @JvmStatic
        private fun genericV1DomainMappings(): Stream<Arguments> = Stream.of(
            Arguments.of(BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT),
            Arguments.of(BookingErrorCode.INVALID_REFUND_ACCOUNT),
            Arguments.of(BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED),
            Arguments.of(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED),
            Arguments.of(PerformanceErrorCode.NON_POSITIVE_RUNNING_TIME),
            Arguments.of(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT),
            Arguments.of(ScheduleErrorCode.INVALID_BOOKING_WINDOW),
            Arguments.of(ScheduleErrorCode.NEGATIVE_TICKET_COUNT),
            Arguments.of(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT),
        )

        @JvmStatic
        private fun applicationStatusMappings(): Stream<Arguments> = Stream.of(
            Arguments.of(ApplicationErrorType.INVALID_INPUT, HttpStatus.BAD_REQUEST),
            Arguments.of(ApplicationErrorType.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED),
            Arguments.of(ApplicationErrorType.FORBIDDEN, HttpStatus.FORBIDDEN),
            Arguments.of(ApplicationErrorType.NOT_FOUND, HttpStatus.NOT_FOUND),
            Arguments.of(ApplicationErrorType.STATE_CONFLICT, HttpStatus.CONFLICT),
            Arguments.of(ApplicationErrorType.UPSTREAM_FAILURE, HttpStatus.BAD_GATEWAY),
            Arguments.of(ApplicationErrorType.UPSTREAM_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
            Arguments.of(ApplicationErrorType.UPSTREAM_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT),
            Arguments.of(ApplicationErrorType.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
        )
    }
}