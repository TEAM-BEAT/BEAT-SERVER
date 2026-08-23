package com.beat.admin.promotion.api.request

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class CarouselHandleRequest(
    val carousels: List<PromotionHandleRequest>,
) {
    data class PromotionModifyRequest(
        val promotionId: Long,
        val carouselNumber: AdminCarouselNumber,
        val newImageUrl: String,
        @param:JsonProperty("isExternal")
        @param:JsonAlias("external")
        val isExternal: Boolean,
        val redirectUrl: String,
        val performanceId: Long?,
    ) : PromotionHandleRequest

    data class PromotionGenerateRequest(
        val carouselNumber: AdminCarouselNumber,
        val newImageUrl: String,
        @param:JsonProperty("isExternal")
        @param:JsonAlias("external")
        val isExternal: Boolean,
        val redirectUrl: String,
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
    JsonSubTypes.Type(value = CarouselHandleRequest.PromotionGenerateRequest::class, name = "generate"),
)
sealed interface PromotionHandleRequest
