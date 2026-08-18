package com.beat.apis.exception

import com.beat.application.frontoffice.booking.BookingApplicationErrorCode
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
}
