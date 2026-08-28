package com.beat.apps.api.home.api

import com.beat.apps.api.home.api.response.HomeFindAllResponse
import com.beat.apps.api.home.api.type.HomeGenreType
import com.beat.apps.api.response.SuccessResponse
import com.beat.apps.api.swagger.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Home", description = "홈 화면에서 공연 및 홍보목록 조회 API")
interface HomeApi {

    @DisableSwaggerSecurity
    @Operation(
        operationId = "homeRetrievePerformanceAndPromotionList",
        summary = "홈 공연 및 홍보 목록 조회",
        description = "홈 화면에 노출할 홍보 목록과 공연 목록을 장르로 필터링해 조회합니다.",
    )
    @ApiResponses(
        value = [ApiResponse(responseCode = "200", description = "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다.")]
    )
    fun getHomePerformanceList(
        @Parameter(
            description = "공연 장르 필터입니다. 생략하면 모든 장르의 공연을 조회합니다.",
            example = "BAND",
            required = false,
        )
        @RequestParam(required = false)
        genre: HomeGenreType?
    ): ResponseEntity<SuccessResponse<HomeFindAllResponse>>
}
