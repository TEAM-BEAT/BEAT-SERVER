package com.beat.admin.exception

import com.beat.application.admin.exception.AdminApplicationErrorCode
import com.beat.application.admin.exception.AdminApplicationErrorType
import com.beat.application.admin.exception.AdminApplicationException
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.TestComponent
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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

class AdminExceptionHttpContractSpec : FunSpec() {

    private val handler = AdminGlobalExceptionHandler()
    private val mockMvc = standaloneSetup(TestController())
        .setControllerAdvice(handler)
        .build()

    init {
        isolationMode = IsolationMode.SingleInstance

        test("malformed JSON uses the legacy error envelope") {
            mockMvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        }

        test("unsupported methods preserve the Allow header") {
            mockMvc.perform(put("/test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(jsonPath("$.status").value(405))
        }

        test("unknown routes keep the legacy error envelope") {
            mockMvc.perform(get("/unknown-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        }

        test("missing static resources keep an empty not-found body") {
            val response = handler.handleNoResourceFoundException(
                NoResourceFoundException(HttpMethod.GET, "/missing-resource", "missing-resource"),
                HttpHeaders(),
                HttpStatus.NOT_FOUND,
                ServletWebRequest(MockHttpServletRequest(), MockHttpServletResponse()),
            )

            response?.statusCode shouldBe HttpStatus.NOT_FOUND
            response?.body.shouldBeNull()
        }

        test("unexpected exception messages are not exposed") {
            val response = handler.handleException(
                IllegalArgumentException("internal contract detail"),
                MockHttpServletRequest(),
            )

            response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
            response.body!!.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
            response.body!!.message shouldBe "서버 내부 오류입니다."
        }

        test("committed responses are protected by Spring MVC") {
            val servletResponse = MockHttpServletResponse()
            servletResponse.isCommitted = true

            val response = handler.handleExceptionInternal(
                IllegalStateException("already committed"),
                null,
                HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                ServletWebRequest(MockHttpServletRequest(), servletResponse),
            )

            response.shouldBeNull()
        }

        test("application errors preserve mapped status and declared message") {
            val response = handler.handleApplicationException(
                AdminApplicationException(TestApplicationErrorCode(AdminApplicationErrorType.NOT_FOUND)),
                MockHttpServletRequest(),
            )

            response.statusCode shouldBe HttpStatus.NOT_FOUND
            response.body!!.status shouldBe HttpStatus.NOT_FOUND.value()
            response.body!!.message shouldBe "안전한 테스트 오류입니다."
        }
    }

    @TestComponent
    @RestController
    private class TestController {
        @PostMapping("/test")
        fun accept(@RequestBody request: TestRequest) = Unit
    }

    private data class TestRequest(val value: String)

    private data class TestApplicationErrorCode(
        override val type: AdminApplicationErrorType,
    ) : AdminApplicationErrorCode {
        override val code: String = "TEST_APPLICATION_ERROR"
        override val message: String = "안전한 테스트 오류입니다."
    }
}
