package com.beat.apps.api.home.facade

import com.beat.application.frontoffice.home.booker.query.HomeQueryService
import com.beat.apps.api.home.api.response.HomeFindAllResponse
import com.beat.apps.api.home.api.type.HomeGenreType
import org.springframework.stereotype.Service

@Service
class HomeFacade(private val homeQueryService: HomeQueryService) {
    fun findHomePerformanceList(genre: HomeGenreType?): HomeFindAllResponse =
        HomeFindAllResponse.from(homeQueryService.findHomePerformanceList(genre?.name))
}
