package com.beat.apis.exception

import com.beat.apis.response.ErrorResponse
import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode as FrontofficeBookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
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
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.resource.NoResourceFoundException

class ApiExceptionHttpContractSpec : FunSpec({
    val handler = ApiGlobalExceptionHandler()
    val mockMvc = standaloneSetup(TestController())
        .setControllerAdvice(handler)
        .build()

    test("malformed JSON uses the legacy 400 error envelope") {
        mockMvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
    }

    test("unsupported methods use the legacy 405 envelope and Allow header") {
        mockMvc.perform(put("/test"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
    }

    test("unknown routes keep the legacy 404 error envelope") {
        mockMvc.perform(get("/unknown-route"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
    }

    test("missing static resources delegate to Spring with an empty 404 body") {
        val response = invokeNoResourceFoundException(
            handler,
            NoResourceFoundException(HttpMethod.GET, "/missing-resource", "missing-resource"),
            HttpHeaders(),
            HttpStatus.NOT_FOUND,
            ServletWebRequest(MockHttpServletRequest(), MockHttpServletResponse()),
        )

        response?.statusCode shouldBe HttpStatus.NOT_FOUND
        response?.body shouldBe null
    }

    test("committed responses delegate protection to Spring") {
        val servletResponse = MockHttpServletResponse().apply { setCommitted(true) }

        val response = invokeExceptionInternal(
            handler,
            IllegalStateException("already committed"),
            null,
            HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            ServletWebRequest(MockHttpServletRequest(), servletResponse),
        )

        response shouldBe null
    }

    test("unexpected exception messages are masked") {
        val response = handler.handleException(
            IllegalArgumentException("internal contract detail"),
            MockHttpServletRequest(),
        )

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.")
    }

    test("migrated Booking failures keep the legacy Frontoffice HTTP contract") {
        val mappings = listOf(
            Triple(
                FrontofficeBookingApplicationErrorCode.SCHEDULE_NOT_FOUND,
                "SCHEDULE_NOT_FOUND",
                HttpStatus.NOT_FOUND to "해당 회차를 찾을 수 없습니다.",
            ),
            Triple(
                FrontofficeBookingApplicationErrorCode.BOOKING_CLOSED,
                "SCHEDULE_BOOKING_CLOSED",
                HttpStatus.CONFLICT to "예매가 마감된 회차입니다.",
            ),
        )

        mappings.forEach { (errorCode, expectedCode, expected) ->
            errorCode.code shouldBe expectedCode
            val response = handler.handleFrontofficeApplicationException(
                FrontofficeApplicationException(errorCode),
                MockHttpServletRequest(),
            )

            assertError(response, expected.first, expected.second)
        }
    }

    test("Frontoffice application error types include RATE_LIMITED") {
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

        expectedStatuses.forEach { (type, expectedStatus) ->
            val response = handler.handleFrontofficeApplicationException(
                FrontofficeApplicationException(TestFrontofficeErrorCode(type)),
                MockHttpServletRequest(),
            )

            assertError(response, expectedStatus, "test message")
        }
    }
})

private fun assertError(
    response: ResponseEntity<ErrorResponse>,
    expectedStatus: HttpStatus,
    expectedMessage: String,
) {
    response.statusCode shouldBe expectedStatus
    val body = response.body ?: error("Expected an error response body")
    body.status shouldBe expectedStatus.value()
    body.message shouldBe expectedMessage
}

private fun invokeNoResourceFoundException(
    handler: ApiGlobalExceptionHandler,
    exception: NoResourceFoundException,
    headers: HttpHeaders,
    status: HttpStatus,
    request: WebRequest,
): ResponseEntity<Any>? = invokeProtected(
    handler,
    "handleNoResourceFoundException",
    arrayOf(
        NoResourceFoundException::class.java,
        HttpHeaders::class.java,
        HttpStatusCode::class.java,
        WebRequest::class.java,
    ),
    exception,
    headers,
    status,
    request,
)

private fun invokeExceptionInternal(
    handler: ApiGlobalExceptionHandler,
    exception: Exception,
    body: Any?,
    headers: HttpHeaders,
    status: HttpStatus,
    request: WebRequest,
): ResponseEntity<Any>? = invokeProtected(
    handler,
    "handleExceptionInternal",
    arrayOf(
        Exception::class.java,
        Any::class.java,
        HttpHeaders::class.java,
        HttpStatusCode::class.java,
        WebRequest::class.java,
    ),
    exception,
    body,
    headers,
    status,
    request,
)

@Suppress("UNCHECKED_CAST")
private fun invokeProtected(
    handler: ApiGlobalExceptionHandler,
    methodName: String,
    parameterTypes: Array<Class<*>>,
    vararg arguments: Any?,
): ResponseEntity<Any>? = handler.javaClass
    .getDeclaredMethod(methodName, *parameterTypes)
    .apply { isAccessible = true }
    .invoke(handler, *arguments) as ResponseEntity<Any>?

private data class TestFrontofficeErrorCode(
    override val type: FrontofficeApplicationErrorType,
) : FrontofficeApplicationErrorCode {
    override val code: String = "TEST_ERROR"
    override val message: String = "test message"
}

@RestController
@org.springframework.boot.test.context.TestComponent
private class TestController {
    @PostMapping("/test")
    fun accept(@RequestBody request: TestRequest) {
    }
}

private data class TestRequest(val value: String)
