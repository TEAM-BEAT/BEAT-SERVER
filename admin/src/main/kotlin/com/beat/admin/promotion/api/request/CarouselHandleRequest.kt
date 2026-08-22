package com.beat.admin.promotion.api.request

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

private const val INVALID_REQUEST_MESSAGE = "잘못된 요청 형식입니다."

data class CarouselHandleRequest(
    @field:NotNull(message = INVALID_REQUEST_MESSAGE)
    @field:Valid
    val carousels: List<@NotNull(message = INVALID_REQUEST_MESSAGE) @Valid PromotionHandleRequest?>?,
) {
    data class PromotionModifyRequest(
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val promotionId: Long?,
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val carouselNumber: AdminCarouselNumber?,
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val newImageUrl: String?,
        @param:JsonProperty("isExternal")
        @param:JsonAlias("external")
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val isExternal: Boolean?,
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val redirectUrl: String?,
        val performanceId: Long?,
    ) : PromotionHandleRequest

    data class PromotionGenerateRequest(
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val carouselNumber: AdminCarouselNumber?,
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val newImageUrl: String?,
        @param:JsonProperty("isExternal")
        @param:JsonAlias("external")
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val isExternal: Boolean?,
        @field:NotNull(message = INVALID_REQUEST_MESSAGE)
        val redirectUrl: String?,
        val performanceId: Long?,
    ) : PromotionHandleRequest
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CarouselHandleRequest.PromotionModifyRequest::class, name = "modify"),
    JsonSubTypes.Type(value = CarouselHandleRequest.PromotionGenerateRequest::class, name = "generate"),
)
sealed interface PromotionHandleRequest
