package com.beat.apps.api.home.api

import com.beat.apps.api.home.api.response.HomeSuccessCode
import com.beat.apps.api.home.api.type.HomeGenreType
import com.beat.apps.api.home.facade.HomeFacade
import com.beat.application.frontoffice.home.booker.query.HomeFindAllResult
import com.beat.application.frontoffice.home.booker.query.HomePerformanceResult
import com.beat.application.frontoffice.home.booker.query.HomePromotionResult
import com.beat.application.frontoffice.home.booker.query.HomeQueryService
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(HomeController::class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [HomeController::class, HomeFacade::class])
class HomeControllerWebSpec : FunSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var homeQueryService: HomeQueryService

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            Mockito.reset(homeQueryService)
        }

        test("요청한 genre로 home 데이터를 조회하고 실제 facade를 통해 매핑한다") {
            Mockito.`when`(homeQueryService.findHomePerformanceList(HomeGenreType.BAND.name))
                .thenReturn(homeResult())

            mockMvc.perform(get("/api/main").param("genre", HomeGenreType.BAND.name))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value(HomeSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS.status))
                .andExpect(jsonPath("$.message").value(HomeSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS.message))
                .andExpect(jsonPath("$.data.promotionList").isArray)
                .andExpect(jsonPath("$.data.performanceList").isArray)
                .andExpect(jsonPath("$.data.promotionList[0].promotionId").value(1))
                .andExpect(jsonPath("$.data.promotionList[0].performanceId").value(11))
                .andExpect(jsonPath("$.data.promotionList[0].redirectUrl").value("redirect"))
                .andExpect(jsonPath("$.data.promotionList[0].isExternal").value(true))
                .andExpect(jsonPath("$.data.promotionList[0].carouselNumber").value("ONE"))
                .andExpect(jsonPath("$.data.promotionList[0].promotionPhoto").exists())
                .andExpect(jsonPath("$.data.performanceList[0].performanceId").value(11))
                .andExpect(jsonPath("$.data.performanceList[0].performanceTitle").value("title"))
                .andExpect(jsonPath("$.data.performanceList[0].performancePeriod").value("period"))
                .andExpect(jsonPath("$.data.performanceList[0].ticketPrice").value(30000))
                .andExpect(jsonPath("$.data.performanceList[0].dueDate").value(3))
                .andExpect(jsonPath("$.data.performanceList[0].genre").value("BAND"))
                .andExpect(jsonPath("$.data.performanceList[0].posterImage").exists())
                .andExpect(jsonPath("$.data.performanceList[0].performanceVenue").value("venue"))

            Mockito.verify(homeQueryService).findHomePerformanceList(HomeGenreType.BAND.name)
        }

        test("genre 파라미터가 없으면 null로 매핑하고 빈 home 목록을 유지한다") {
            Mockito.`when`(homeQueryService.findHomePerformanceList(null))
                .thenReturn(HomeFindAllResult(emptyList(), emptyList()))

            mockMvc.perform(get("/api/main"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value(HomeSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS.status))
                .andExpect(jsonPath("$.message").value(HomeSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS.message))
                .andExpect(jsonPath("$.data.promotionList").isArray)
                .andExpect(jsonPath("$.data.promotionList").isEmpty)
                .andExpect(jsonPath("$.data.performanceList").isArray)
                .andExpect(jsonPath("$.data.performanceList").isEmpty)

            Mockito.verify(homeQueryService).findHomePerformanceList(null)
        }
    }

    private fun homeResult() = HomeFindAllResult(
        promotionList = listOf(
            HomePromotionResult(
                promotionId = 1L,
                promotionPhoto = "promotion.png",
                performanceId = 11L,
                redirectUrl = "redirect",
                isExternal = true,
                carouselNumber = "ONE",
            ),
        ),
        performanceList = listOf(
            HomePerformanceResult(
                performanceId = 11L,
                performanceTitle = "title",
                performancePeriod = "period",
                ticketPrice = 30_000,
                dueDate = 3,
                genre = "BAND",
                posterImage = "poster.png",
                performanceVenue = "venue",
            ),
        ),
    )
}
