package com.beat.apps.admin.promotion.api.response

import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.AdminPromotionResults.AdminPromotionResult
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "캐러셀 프로모션 전체 조회 응답")
data class CarouselFindAllResponse(
    @field:Schema(
        description = "캐러셀 번호 순으로 정렬된 프로모션 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example =
            """[{"promotionId":1,"carouselNumber":"ONE","newImageUrl":"dev/carousel/summer.png","isExternal":false,"redirectUrl":"/performances/11","performanceId":11}]""",
    )
    @get:JsonProperty("carousels")
    val carouselResponses: List<CarouselFindResponse>
) {
    constructor(
        promotionResults: AdminPromotionResults
    ) : this(promotionResults.promotionResults.map { CarouselFindResponse(it) })

    @Schema(description = "캐러셀에 등록된 프로모션 정보")
    data class CarouselFindResponse(
        @field:Schema(
            description = "프로모션 식별자",
            types = ["integer", "null"],
            format = "int64",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1",
        )
        val promotionId: Long?,
        @field:Schema(
            description = "프로모션이 배치된 캐러셀 순서",
            type = "string",
            allowableValues = ["ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN"],
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ONE",
        )
        val carouselNumber: String,
        @field:Schema(
            description = "프로모션 이미지 URL 또는 저장 키",
            type = "string",
            format = "uri-reference",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "dev/carousel/summer.png",
        )
        val newImageUrl: String,
        @field:Schema(
            description = "외부 URL 연결 여부",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "false",
        )
        val isExternal: Boolean,
        @field:Schema(
            description = "이미지 클릭 시 이동할 URL 또는 경로",
            type = "string",
            format = "uri-reference",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "/performances/11",
        )
        val redirectUrl: String,
        @field:Schema(
            description = "연결된 공연 식별자",
            types = ["integer", "null"],
            format = "int64",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "11",
        )
        val performanceId: Long?,
    ) {
        constructor(
            promotionResult: AdminPromotionResult
        ) : this(
            promotionId = promotionResult.promotionId,
            carouselNumber = promotionResult.carouselNumber,
            newImageUrl = promotionResult.newImageUrl,
            isExternal = promotionResult.isExternal,
            redirectUrl = promotionResult.redirectUrl,
            performanceId = promotionResult.performanceId,
        )
    }
}
