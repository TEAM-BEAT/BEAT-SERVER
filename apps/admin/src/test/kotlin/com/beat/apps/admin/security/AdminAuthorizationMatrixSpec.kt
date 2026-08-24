package com.beat.apps.admin.security

import com.beat.apps.admin.support.BeatAdminAcceptanceTest
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

@BeatAdminAcceptanceTest
@Tags("acceptance")
class AdminAuthorizationMatrixSpec : FunSpec() {

    @Autowired
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val expectedRoutes = setOf(
        Route(HttpMethod.GET, "/api/admin/carousels/presigned-url"),
        Route(HttpMethod.GET, "/api/admin/banner/presigned-url"),
        Route(HttpMethod.GET, "/api/admin/carousels"),
        Route(HttpMethod.PUT, "/api/admin/carousels"),
        Route(HttpMethod.GET, "/api/admin/users"),
    )

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("admin controller 매핑은 admin 인가 경로와 정확히 일치한다") {
            adminControllerRoutes() shouldBe expectedRoutes
        }

        test("인증되지 않은 요청은 admin 경계에서 거부된다") {
            expectedRoutes.forEach { endpoint ->
                val status = perform(endpoint).response.status

                status shouldBe UNAUTHORIZED
            }
        }

        test("member로 인증된 요청은 admin 경로에 접근할 수 없다") {
            expectedRoutes.forEach { endpoint ->
                val status = perform(endpoint, memberAuthentication()).response.status

                status shouldBe FORBIDDEN
            }
        }

        test("admin으로 인증된 요청은 admin 보안 경계를 통과한다") {
            expectedRoutes.forEach { endpoint ->
                val status = perform(endpoint, adminAuthentication()).response.status

                status shouldNotBe UNAUTHORIZED
                status shouldNotBe FORBIDDEN
            }
        }
    }

    private fun adminControllerRoutes(): Set<Route> =
        handlerMapping.handlerMethods.entries
            .asSequence()
            .filter { (_, handlerMethod) -> handlerMethod.isAdminController() }
            .flatMap { (mapping, _) ->
                val paths = mapping.pathPatternsCondition?.patterns
                    ?.map { it.patternString }
                    .orEmpty()
                mapping.methodsCondition.methods.asSequence().flatMap { method ->
                    paths.asSequence().map { path -> Route(HttpMethod.valueOf(method.name), path) }
                }
            }
            .filter { it.path.startsWith("/api/admin/") }
            .toSet()

    private fun perform(endpoint: Route, token: UsernamePasswordAuthenticationToken? = null) =
        mockMvc.perform(
            request(endpoint.method, endpoint.path)
                .param("carouselImages", "carousel.png")
                .param("bannerImage", "banner.png")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .let { builder ->
                    token?.let { builder.with(authentication(it)) } ?: builder
                },
        ).andReturn()

    private fun memberAuthentication() = UsernamePasswordAuthenticationToken(
        1L,
        null,
        listOf(SimpleGrantedAuthority("ROLE_MEMBER")),
    )

    private fun adminAuthentication() = UsernamePasswordAuthenticationToken(
        1L,
        null,
        listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

    private data class Route(
        val method: HttpMethod,
        val path: String,
    )

    private companion object {
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
    }
}

private fun HandlerMethod.isAdminController(): Boolean =
    beanType.packageName.startsWith("com.beat.apps.admin") &&
        AnnotatedElementUtils.hasAnnotation(beanType, Controller::class.java)
