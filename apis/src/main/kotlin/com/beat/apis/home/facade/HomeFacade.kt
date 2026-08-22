package com.beat.apis.home.facade

import com.beat.apis.home.api.response.HomeFindAllResponse
import com.beat.apis.home.api.type.HomeGenreType
import com.beat.application.frontoffice.home.booker.query.HomeQueryService
import org.springframework.stereotype.Service

@Service
class HomeFacade(
    private val homeQueryService: HomeQueryService,
) {
    fun findHomePerformanceList(genre: HomeGenreType?): HomeFindAllResponse =
        HomeFindAllResponse.from(homeQueryService.findHomePerformanceList(genre?.name))
}
