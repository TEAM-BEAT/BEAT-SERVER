package com.beat.apps.admin.promotion.api.request

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "캐러셀 프로모션 생성 또는 수정 요청")
data class CarouselHandleRequest(
    @field:Schema(
        description = "캐러셀 프로모션 생성 또는 수정 항목 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example =
            """[{"type":"generate","carouselNumber":"ONE","newImageUrl":"https://cdn.beatlive.kr/prod/carousel/summer.png","isExternal":false,"redirectUrl":"/performances/11","performanceId":11}]""",
    )
    val carousels: List<PromotionHandleRequest>
) {
    @Schema(description = "기존 캐러셀 프로모션 수정 요청")
    data class PromotionModifyRequest(
        @field:Schema(
            description = "수정할 프로모션 식별자",
            format = "int64",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1",
        )
        val promotionId: Long,
        @field:Schema(
            description = "프로모션을 배치할 캐러셀 순서",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ONE",
        )
        val carouselNumber: AdminCarouselNumber,
        @field:Schema(
            description = "새 이미지 URL 또는 저장 키",
            format = "uri-reference",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://cdn.beatlive.kr/prod/carousel/summer.png",
        )
        val newImageUrl: String,
        @field:Schema(
            description = "외부 URL 연결 여부",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "false",
        )
        @param:JsonProperty("isExternal")
        @param:JsonAlias("external")
        val isExternal: Boolean,
        @field:Schema(
            description = "이미지 클릭 시 이동할 URL 또는 경로",
            format = "uri-reference",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "/performances/11",
        )
        val redirectUrl: String,
        @field:Schema(
            description = "연결할 공연 식별자",
            types = ["integer", "null"],
            format = "int64",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "11",
        )
        val performanceId: Long?,
    ) : PromotionHandleRequest

    @Schema(description = "새 캐러셀 프로모션 생성 요청")
    data class PromotionGenerateRequest(
        @field:Schema(
            description = "프로모션을 배치할 캐러셀 순서",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ONE",
        )
        val carouselNumber: AdminCarouselNumber,
        @field:Schema(
            description = "새 이미지 URL 또는 저장 키",
            format = "uri-reference",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://cdn.beatlive.kr/prod/carousel/summer.png",
        )
        val newImageUrl: String,
        @field:Schema(
            description = "외부 URL 연결 여부",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "false",
        )
        @param:JsonProperty("isExternal")
        @param:JsonAlias("external")
        val isExternal: Boolean,
        @field:Schema(
            description = "이미지 클릭 시 이동할 URL 또는 경로",
            format = "uri-reference",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "/performances/11",
        )
        val redirectUrl: String,
        @field:Schema(
            description = "연결할 공연 식별자",
            types = ["integer", "null"],
            format = "int64",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "11",
        )
        val performanceId: Long?,
    ) : PromotionHandleRequest
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CarouselHandleRequest.PromotionModifyRequest::class, name = "modify"),
    JsonSubTypes.Type(
        value = CarouselHandleRequest.PromotionGenerateRequest::class,
        name = "generate",
    ),
)
@Schema(
    description = "캐러셀 프로모션 생성 또는 수정 항목",
    oneOf =
        [
            CarouselHandleRequest.PromotionModifyRequest::class,
            CarouselHandleRequest.PromotionGenerateRequest::class,
        ],
    discriminatorProperty = "type",
    discriminatorMapping =
        [
            DiscriminatorMapping(
                value = "modify",
                schema = CarouselHandleRequest.PromotionModifyRequest::class,
            ),
            DiscriminatorMapping(
                value = "generate",
                schema = CarouselHandleRequest.PromotionGenerateRequest::class,
            ),
        ],
)
sealed interface PromotionHandleRequest
