package com.beat.apps.api.security

import com.beat.apps.api.support.BeatAcceptanceTest
import com.beat.support.security.token.TokenIssuer
import com.beat.support.security.token.TokenSubject
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpHeaders
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

    @Autowired
    private lateinit var tokenIssuer: TokenIssuer

    private val expectedAuthorizationMatrix = listOf(
        Endpoint(HttpMethod.POST, "/api/bookings/member", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.GET, "/api/bookings/member/retrieve", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.POST, "/api/bookings/guest", Access.PUBLIC),
        Endpoint(HttpMethod.POST, "/api/bookings/guest/retrieve", Access.PUBLIC),
        Endpoint(HttpMethod.PATCH, "/api/bookings/refund", Access.PUBLIC),
        Endpoint(HttpMethod.PATCH, "/api/bookings/cancel", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/files/presigned-url", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.GET, "/api/main", Access.PUBLIC),
        Endpoint(HttpMethod.POST, "/api/users/sign-up", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/users/refresh-token", Access.PUBLIC),
        Endpoint(HttpMethod.POST, "/api/users/sign-out", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.POST, "/api/performances", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.PUT, "/api/performances", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.GET, "/api/performances/{performanceId}", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.GET, "/api/performances/detail/{performanceId}", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/performances/booking/{performanceId}", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/performances/user", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.DELETE, "/api/performances/{performanceId}", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.GET, "/api/schedules/{scheduleId}/availability", Access.PUBLIC),
        Endpoint(HttpMethod.GET, "/api/tickets/{performanceId}", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.GET, "/api/tickets/search/{performanceId}", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.PUT, "/api/tickets/update", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.PUT, "/api/tickets/refund", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.PUT, "/api/tickets/delete", Access.MEMBER_OR_ADMIN),
        Endpoint(HttpMethod.GET, "/api/admin/security-boundary-probe", Access.ADMIN, controllerRoute = false),
    )

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("API controller 매핑은 인가 매트릭스 경로와 정확히 일치한다") {
            apiControllerEndpoints() shouldBe expectedAuthorizationMatrix
                .filter(Endpoint::controllerRoute)
                .map { it.route }
                .toSet()
        }

        test("인증되지 않은 요청은 인가 매트릭스에 따라 처리된다") {
            expectedAuthorizationMatrix.forEach { endpoint ->
                val status = perform(endpoint).response.status

                when (endpoint.access) {
                    Access.MEMBER_OR_ADMIN,
                    Access.ADMIN,
                    -> status shouldBe UNAUTHORIZED
                    Access.PUBLIC -> {
                        status shouldNotBe UNAUTHORIZED
                        status shouldNotBe FORBIDDEN
                    }
                }
            }
        }

        test("MEMBER는 일반 보호 API만 통과하고 admin API는 거부된다") {
            expectedAuthorizationMatrix
                .filter { it.access != Access.PUBLIC }
                .forEach { endpoint ->
                    val status = perform(endpoint, ROLE_MEMBER).response.status

                    if (endpoint.access == Access.ADMIN) {
                        status shouldBe FORBIDDEN
                    } else {
                        status shouldNotBe UNAUTHORIZED
                        status shouldNotBe FORBIDDEN
                    }
                }
        }

        test("ADMIN은 일반 보호 API와 admin API를 모두 통과한다") {
            expectedAuthorizationMatrix
                .filter { it.access != Access.PUBLIC }
                .forEach { endpoint ->
                    val status = perform(endpoint, ROLE_ADMIN).response.status

                    status shouldNotBe UNAUTHORIZED
                    status shouldNotBe FORBIDDEN
                }
        }

        test("USER와 unknown role은 모든 보호 API에서 거부된다") {
            listOf(ROLE_USER, ROLE_UNKNOWN).forEach { role ->
                expectedAuthorizationMatrix
                    .filter { it.access != Access.PUBLIC }
                    .forEach { endpoint ->
                        perform(endpoint, role).response.status shouldBe FORBIDDEN
                    }
            }
        }

        test("실제 서명된 MEMBER JWT는 filter를 거쳐 일반 보호 API를 통과한다") {
            val token = tokenIssuer.issueAccessToken(TokenSubject(1L, ROLE_MEMBER))
            val endpoint = expectedAuthorizationMatrix.first { it.access == Access.MEMBER_OR_ADMIN }
            val status = performWithBearer(endpoint, token).response.status

            status shouldNotBe UNAUTHORIZED
            status shouldNotBe FORBIDDEN
        }

        test("실제 서명되었어도 USER와 unknown role JWT는 filter에서 401로 거부된다") {
            listOf(ROLE_USER, ROLE_UNKNOWN).forEach { role ->
                val token = tokenIssuer.issueAccessToken(TokenSubject(1L, role))
                val endpoint = expectedAuthorizationMatrix.first { it.access == Access.MEMBER_OR_ADMIN }

                performWithBearer(endpoint, token).response.status shouldBe UNAUTHORIZED
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

    private fun perform(endpoint: Endpoint, role: String? = null) =
        mockMvc.perform(
            request(endpoint.method, endpoint.path.replace(PATH_VARIABLE_REGEX, "1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .let { builder ->
                    if (role != null) {
                        builder.with(authentication(authenticationFor(role)))
                    } else {
                        builder
                    }
                },
        ).andReturn()

    private fun performWithBearer(endpoint: Endpoint, token: String) =
        mockMvc.perform(
            request(endpoint.method, endpoint.path.replace(PATH_VARIABLE_REGEX, "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andReturn()

    private fun authenticationFor(role: String) = UsernamePasswordAuthenticationToken(
        1L,
        null,
        listOf(SimpleGrantedAuthority(role)),
    )

    private data class Endpoint(
        val method: HttpMethod,
        val path: String,
        val access: Access,
        val controllerRoute: Boolean = true,
    ) {
        val route: Route get() = Route(method, path)
    }

    private data class Route(
        val method: HttpMethod,
        val path: String,
    )

    private enum class Access {
        PUBLIC,
        MEMBER_OR_ADMIN,
        ADMIN,
    }

    private companion object {
        val PATH_VARIABLE_REGEX = Regex("\\{[^}]+}")
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val ROLE_MEMBER = "ROLE_MEMBER"
        const val ROLE_ADMIN = "ROLE_ADMIN"
        const val ROLE_USER = "ROLE_USER"
        const val ROLE_UNKNOWN = "ROLE_UNKNOWN"
    }
}

private fun HandlerMethod.isApiController(): Boolean =
    beanType.packageName.startsWith("com.beat.apps.api") &&
        AnnotatedElementUtils.hasAnnotation(beanType, Controller::class.java)
