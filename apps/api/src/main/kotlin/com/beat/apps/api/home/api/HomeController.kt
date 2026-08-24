package com.beat.apps.api.home.api

import com.beat.apps.api.home.api.response.HomeFindAllResponse
import com.beat.apps.api.home.api.response.HomeSuccessCode
import com.beat.apps.api.home.api.type.HomeGenreType
import com.beat.apps.api.home.facade.HomeFacade
import com.beat.apps.api.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/main")
class HomeController(
    private val homeFacade: HomeFacade,
) : HomeApi {

    @GetMapping
    override fun getHomePerformanceList(
        @RequestParam(required = false) genre: HomeGenreType?,
    ): ResponseEntity<SuccessResponse<HomeFindAllResponse>> {
        val homeFindAllResponse = homeFacade.findHomePerformanceList(genre)
        return ResponseEntity.ok(
            SuccessResponse.of(HomeSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS, homeFindAllResponse),
        )
    }
}
