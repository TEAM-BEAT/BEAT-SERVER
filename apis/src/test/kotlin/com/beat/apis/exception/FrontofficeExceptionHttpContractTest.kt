package com.beat.apis.exception

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest

class FrontofficeExceptionHttpContractTest {
    private val handler = ApiGlobalExceptionHandler()

    @Test
    fun `maps migrated booking failure to the legacy HTTP contract`() {
        val response = handler.handleFrontofficeApplicationException(
            FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND),
            MockHttpServletRequest(),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.status).isEqualTo(404)
        assertThat(response.body?.message).isEqualTo("해당 회차를 찾을 수 없습니다.")
    }

    @Test
    fun `maps each frontoffice error type to its HTTP status`() {
        val expectedStatuses = mapOf(
            FrontofficeApplicationErrorType.INVALID_INPUT to HttpStatus.BAD_REQUEST,
            FrontofficeApplicationErrorType.UNAUTHENTICATED to HttpStatus.UNAUTHORIZED,
            FrontofficeApplicationErrorType.FORBIDDEN to HttpStatus.FORBIDDEN,
            FrontofficeApplicationErrorType.NOT_FOUND to HttpStatus.NOT_FOUND,
            FrontofficeApplicationErrorType.STATE_CONFLICT to HttpStatus.CONFLICT,
            FrontofficeApplicationErrorType.UPSTREAM_FAILURE to HttpStatus.BAD_GATEWAY,
            FrontofficeApplicationErrorType.UPSTREAM_UNAVAILABLE to HttpStatus.SERVICE_UNAVAILABLE,
            FrontofficeApplicationErrorType.UPSTREAM_TIMEOUT to HttpStatus.GATEWAY_TIMEOUT,
            FrontofficeApplicationErrorType.RATE_LIMITED to HttpStatus.TOO_MANY_REQUESTS,
            FrontofficeApplicationErrorType.INTERNAL_ERROR to HttpStatus.INTERNAL_SERVER_ERROR,
        )

        expectedStatuses.forEach { (type, status) ->
            val response = handler.handleFrontofficeApplicationException(
                FrontofficeApplicationException(TestErrorCode(type)),
                MockHttpServletRequest(),
            )

            assertThat(response.statusCode).isEqualTo(status)
            assertThat(response.body?.status).isEqualTo(status.value())
            assertThat(response.body?.message).isEqualTo("test message")
        }
    }

    private data class TestErrorCode(
        override val type: FrontofficeApplicationErrorType,
    ) : FrontofficeApplicationErrorCode {
        override val code: String = "TEST_ERROR"
        override val message: String = "test message"
    }
}
