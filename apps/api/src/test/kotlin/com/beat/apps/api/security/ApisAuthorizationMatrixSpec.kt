package com.beat.apps.api.security

import com.beat.apps.api.support.BeatAcceptanceTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.stereotype.Controller
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@BeatAcceptanceTest
@Tags("acceptance")
class ApisAuthorizationMatrixSpec : FunSpec() {

    @Autowired
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val expectedAuthorizationMatrix = listOf(
        Endpoint(HttpMethod.POST, "/api/bookings/member", Access.MEMBER),
        Endpoint(HttpMethod.GET, "/api/bookings/member/retrieve", Access.MEMBER),
        Endpoint(HttpMethod.POST, "/api/bookings/guest", Access.PUBLIC),
        Endpoint(HttpMethod.POST, "/api/bookings/guest/retrieve", Access.PUBLIC),
        Endpoint(HttpMethod.PATCH, "/api/bookings/refund", Access.PUBLIC_OR_MEMBER),
        Endpoint(HttpMethod.PATCH, "/api/bookings/cancel", Access.PUBLIC_OR_MEMBER),
        Endpoint(HttpMethod.GET, "/api/files/presigned-url", Access.MEMBER),
        Endpoint(HttpMethod.GET, "/api/main", Access.PUBLIC),
        Endpoint(HttpMethod.POST, "/api/users/sign-up", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/users/refresh-token", Access.PUBLIC),
        Endpoint(HttpMethod.POST, "/api/users/sign-out", Access.MEMBER),
        Endpoint(HttpMethod.POST, "/api/performances", Access.MEMBER),
        Endpoint(HttpMethod.PUT, "/api/performances", Access.MEMBER),
        Endpoint(HttpMethod.GET, "/api/performances/{performanceId}", Access.MEMBER),
        Endpoint(HttpMethod.GET, "/api/performances/detail/{performanceId}", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/performances/booking/{performanceId}", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/performances/user", Access.MEMBER),
        Endpoint(HttpMethod.DELETE, "/api/performances/{performanceId}", Access.MEMBER),
        Endpoint(HttpMethod.GET, "/api/schedules/{scheduleId}/availability", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/tickets/{performanceId}", Access.MEMBER),
        Endpoint(HttpMethod.GET, "/api/tickets/search/{performanceId}", Access.MEMBER),
        Endpoint(HttpMethod.PUT, "/api/tickets/update", Access.MEMBER),
        Endpoint(HttpMethod.PUT, "/api/tickets/refund", Access.MEMBER),
        Endpoint(HttpMethod.PUT, "/api/tickets/delete", Access.MEMBER),
    )

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("API controller 매핑은 인가 매트릭스 경로와 정확히 일치한다") {
            apiControllerEndpoints() shouldBe expectedAuthorizationMatrix.map { it.route }.toSet()
        }

        test("인증되지 않은 요청은 인가 매트릭스에 따라 처리된다") {
            expectedAuthorizationMatrix.forEach { endpoint ->
                val status = perform(endpoint).response.status

                when (endpoint.access) {
                    Access.MEMBER -> status shouldBe UNAUTHORIZED
                    Access.PUBLIC,
                    Access.PUBLIC_OR_MEMBER,
                    -> {
                        status shouldNotBe UNAUTHORIZED
                        status shouldNotBe FORBIDDEN
                    }
                }
            }
        }

        test("member로 인증된 요청은 member 엔드포인트의 보안 경계를 통과한다") {
            expectedAuthorizationMatrix
                .filter { it.access == Access.MEMBER }
                .forEach { endpoint ->
                    val status = perform(endpoint, authenticated = true).response.status

                    status shouldNotBe UNAUTHORIZED
                    status shouldNotBe FORBIDDEN
                }
        }
    }

    private fun apiControllerEndpoints(): Set<Route> =
        handlerMapping.handlerMethods.entries
            .asSequence()
            .filter { (_, handlerMethod) -> handlerMethod.isApiController() }
            .flatMap { (mapping, _) ->
                val paths = mapping.pathPatternsCondition?.patterns
                    ?.map { it.patternString }
                    .orEmpty()
                val methods = mapping.methodsCondition.methods
                methods.asSequence().flatMap { method ->
                    paths.asSequence().map { path -> Route(HttpMethod.valueOf(method.name), path) }
                }
            }
            .toSet()

    private fun perform(endpoint: Endpoint, authenticated: Boolean = false) =
        mockMvc.perform(
            request(endpoint.method, endpoint.path.replace(PATH_VARIABLE_REGEX, "1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .let { builder ->
                    if (authenticated) {
                        builder.with(authentication(memberAuthentication()))
                    } else {
                        builder
                    }
                },
        ).andReturn()

    private fun memberAuthentication() = UsernamePasswordAuthenticationToken(
        1L,
        null,
        listOf(SimpleGrantedAuthority("ROLE_MEMBER")),
    )

    private data class Endpoint(
        val method: HttpMethod,
        val path: String,
        val access: Access,
    ) {
        val route: Route get() = Route(method, path)
    }

    private data class Route(
        val method: HttpMethod,
        val path: String,
    )

    private enum class Access {
        MEMBER,
        PUBLIC,
        PUBLIC_OR_MEMBER,
    }

    private companion object {
        val PATH_VARIABLE_REGEX = Regex("\\{[^}]+}")
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
    }
}

private fun HandlerMethod.isApiController(): Boolean =
    beanType.packageName.startsWith("com.beat.apps.api") &&
        AnnotatedElementUtils.hasAnnotation(beanType, Controller::class.java)
