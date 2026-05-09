package com.beat.observability.logging.filter

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class BaseMdcLoggingFilterTest {

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `uses request id header as trace id and writes it to response`() {
        val filter = testFilter("42")
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER, "trace-from-client")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response) { _, _ ->
            assertEquals("trace-from-client", MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY))
            assertEquals("42", MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        }

        assertEquals("trace-from-client", response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        assertMdcCleared()
    }

    @Test
    fun `generates trace id when request id header is missing`() {
        val filter = testFilter(null)
        val request = request()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response) { _, _ ->
            val traceId = MDC.get(BaseMdcLoggingFilter.TRACE_ID_KEY)
            assertNotNull(traceId)
            assertEquals(32, traceId.length)
            assertEquals(traceId, response.getHeader(BaseMdcLoggingFilter.TRACE_ID_HEADER))
        }

        assertMdcCleared()
    }

    @Test
    fun `uses first forwarded for ip before real ip and remote addr`() {
        val filter = testFilter(null)
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.X_FORWARDED_FOR_HEADER, "10.0.0.1, 10.0.0.2")
        request.addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("10.0.0.1", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }

        assertMdcCleared()
    }

    @Test
    fun `uses real ip when forwarded for is missing`() {
        val filter = testFilter(null)
        val request = request()
        request.addHeader(BaseMdcLoggingFilter.X_REAL_IP_HEADER, "10.0.0.3")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("10.0.0.3", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }

        assertMdcCleared()
    }

    @Test
    fun `uses remote addr when proxy headers are missing`() {
        val filter = testFilter(null)
        val request = request()
        request.remoteAddr = "127.0.0.1"

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("127.0.0.1", MDC.get(BaseMdcLoggingFilter.CLIENT_IP_KEY))
        }

        assertMdcCleared()
    }

    @Test
    fun `stores request info and falls back to guest user`() {
        val filter = testFilter(" ")
        val request = request(method = "POST", uri = "/api/bookings")

        filter.doFilter(request, MockHttpServletResponse()) { _, _ ->
            assertEquals("POST /api/bookings", MDC.get(BaseMdcLoggingFilter.REQUEST_INFO_KEY))
            assertEquals(BaseMdcLoggingFilter.DEFAULT_GUEST_USER, MDC.get(BaseMdcLoggingFilter.USER_ID_KEY))
        }

        assertMdcCleared()
    }

    private fun testFilter(userId: String?): BaseMdcLoggingFilter = object : BaseMdcLoggingFilter() {
        override fun resolveUserId(): String? = userId
    }

    private fun request(method: String = "GET", uri: String = "/api/main"): MockHttpServletRequest =
        MockHttpServletRequest(method, uri)

    private fun BaseMdcLoggingFilter.doFilter(
        request: MockHttpServletRequest,
        response: MockHttpServletResponse,
        assertion: (MockHttpServletRequest, MockHttpServletResponse) -> Unit,
    ) {
        val chain = FilterChain { servletRequest, servletResponse ->
            assertion(servletRequest as MockHttpServletRequest, servletResponse as MockHttpServletResponse)
        }
        doFilter(request, response, chain)
    }

    private fun assertMdcCleared() {
        assertTrue(MDC.getCopyOfContextMap().isNullOrEmpty())
        assertFalse(MDC.getCopyOfContextMap()?.containsKey(BaseMdcLoggingFilter.TRACE_ID_KEY) ?: false)
    }
}
