package com.beat.apps.admin.user.api

import com.beat.application.admin.user.query.AdminUserQueryService
import com.beat.application.admin.user.query.AdminUserResults
import com.beat.apps.admin.user.api.response.UserSuccessCode
import com.beat.apps.admin.user.facade.AdminUserFacade
import com.beat.support.security.CurrentMember
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@WebMvcTest(controllers = [AdminUserController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(
    classes =
        [AdminUserController::class, AdminUserFacade::class, AdminUserWebTestConfiguration::class]
)
class AdminUserControllerWebSpec : FunSpec() {

    @Autowired private lateinit var mockMvc: MockMvc

    @TestBean private lateinit var adminUserQueryService: AdminUserQueryService

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            clearMocks(adminUserQueryService)
        }

        test("관리자 회원 id를 전달하고 User 목록 response contract를 유지한다") {
            every { adminUserQueryService.findAllUsers(MEMBER_ID) } returns
                AdminUserResults(
                    listOf(
                        AdminUserResults.AdminUserResult(1L, "ROLE_USER"),
                        AdminUserResults.AdminUserResult(2L, "ROLE_ADMIN"),
                    )
                )

            mockMvc
                .perform(get("/api/admin/users"))
                .andExpect(status().isOk)
                .andExpect(
                    jsonPath("$.status").value(UserSuccessCode.FETCH_ALL_USERS_SUCCESS.status)
                )
                .andExpect(
                    jsonPath("$.message").value(UserSuccessCode.FETCH_ALL_USERS_SUCCESS.message)
                )
                .andExpect(jsonPath("$.data.users[0].id").value(1))
                .andExpect(jsonPath("$.data.users[0].role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.users[1].id").value(2))
                .andExpect(jsonPath("$.data.users[1].role").value("ROLE_ADMIN"))

            verify { adminUserQueryService.findAllUsers(MEMBER_ID) }
        }
    }

    private companion object {
        const val MEMBER_ID = 7L

        @JvmStatic fun adminUserQueryService(): AdminUserQueryService = mockk()
    }
}

@TestConfiguration(proxyBeanMethods = false)
private class AdminUserWebTestConfiguration : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(AdminUserCurrentMemberArgumentResolver())
    }
}

private class AdminUserCurrentMemberArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentMember::class.java) &&
            (parameter.parameterType == Long::class.javaObjectType ||
                parameter.parameterType == Long::class.javaPrimitiveType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any = MEMBER_ID

    private companion object {
        const val MEMBER_ID = 7L
    }
}
