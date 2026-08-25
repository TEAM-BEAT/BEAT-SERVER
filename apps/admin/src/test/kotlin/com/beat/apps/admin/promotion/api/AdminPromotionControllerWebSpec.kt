package com.beat.apps.admin.promotion.api

import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.AdminPromotionResults.AdminPromotionResult
import com.beat.application.admin.promotion.PromotionImageUpload
import com.beat.application.admin.promotion.command.AdminPromotionCommandService
import com.beat.application.admin.promotion.command.CarouselHandleCommand
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionGenerateCommand
import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult
import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.application.admin.promotion.query.AdminPromotionQueryService
import com.beat.apps.admin.promotion.facade.AdminPromotionFacade
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
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@WebMvcTest(controllers = [AdminPromotionController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(
    classes =
        [
            AdminPromotionController::class,
            AdminPromotionFacade::class,
            AdminPromotionWebTestConfiguration::class,
        ]
)
class AdminPromotionControllerWebSpec : FunSpec() {

    @Autowired private lateinit var mockMvc: MockMvc

    @TestBean private lateinit var queryService: AdminPromotionQueryService

    @TestBean private lateinit var commandService: AdminPromotionCommandService

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            clearMocks(queryService, commandService)
        }

        test("관리자 Promotion 조회와 upload route가 Application query 결과를 기존 JSON으로 매핑한다") {
            every { queryService.findAllPromotionsSortedByCarouselNumber(MEMBER_ID) } returns
                AdminPromotionResults(
                    listOf(AdminPromotionResult(1L, "ONE", "image", false, "redirect", 11L))
                )
            every {
                queryService.issueAllPresignedUrlsForCarousel(MEMBER_ID, listOf("carousel.png"))
            } returns
                CarouselPresignedUrlsResult(
                    mapOf(
                        "carousel.png" to
                            PromotionImageUpload("upload", "dev/carousel/carousel.png")
                    )
                )
            every { queryService.issuePresignedUrlForBanner(MEMBER_ID, "banner.png") } returns
                BannerPresignedUrlResult("banner-upload", "dev/banner/banner.png")

            mockMvc
                .perform(get("/api/admin/carousels"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.carousels[0].promotionId").value(1))
                .andExpect(jsonPath("$.data.carousels[0].performanceId").value(11))
            mockMvc
                .perform(
                    get("/api/admin/carousels/presigned-url")
                        .queryParam("carouselImages", "carousel.png")
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.carouselPresignedUrls['carousel.png']").value("upload"))
                .andExpect(
                    jsonPath("$.data.carouselPresignedUploads['carousel.png'].imageKey")
                        .value("dev/carousel/carousel.png")
                )
            mockMvc
                .perform(
                    get("/api/admin/banner/presigned-url").queryParam("bannerImage", "banner.png")
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.bannerPresignedUrl").value("banner-upload"))

            verify { queryService.findAllPromotionsSortedByCarouselNumber(MEMBER_ID) }
            verify {
                queryService.issueAllPresignedUrlsForCarousel(MEMBER_ID, listOf("carousel.png"))
            }
            verify { queryService.issuePresignedUrlForBanner(MEMBER_ID, "banner.png") }
        }

        test("polymorphic carousel request를 Kotlin command로 변환하고 기존 response shape를 유지한다") {
            val command =
                CarouselHandleCommand(
                    listOf(
                        PromotionGenerateCommand(
                            "ONE",
                            "prod/carousel/new",
                            false,
                            "/performances/11",
                            11L,
                        )
                    )
                )
            every {
                commandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)
            } returns
                AdminPromotionResults(
                    listOf(
                        AdminPromotionResult(
                            3L,
                            "ONE",
                            "prod/carousel/new",
                            false,
                            "/performances/11",
                            11L,
                        )
                    )
                )

            mockMvc
                .perform(
                    put("/api/admin/carousels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"carousels":[{"type":"generate","carouselNumber":"ONE","newImageUrl":"prod/carousel/new","isExternal":false,"redirectUrl":"/performances/11","performanceId":11}]}"""
                        )
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.modifiedPromotions[0].promotionId").value(3))
                .andExpect(jsonPath("$.data.modifiedPromotions[0].carouselNumber").value("ONE"))

            verify { commandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command) }
        }
    }

    private companion object {
        const val MEMBER_ID = 7L

        @JvmStatic fun queryService(): AdminPromotionQueryService = mockk()

        @JvmStatic fun commandService(): AdminPromotionCommandService = mockk()
    }
}

@TestConfiguration(proxyBeanMethods = false)
private class AdminPromotionWebTestConfiguration : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(AdminPromotionCurrentMemberArgumentResolver())
    }
}

private class AdminPromotionCurrentMemberArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentMember::class.java) &&
            (parameter.parameterType == Long::class.javaObjectType ||
                parameter.parameterType == Long::class.javaPrimitiveType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any = 7L
}
