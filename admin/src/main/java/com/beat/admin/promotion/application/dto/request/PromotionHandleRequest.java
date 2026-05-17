package com.beat.admin.promotion.application.dto.request;

import static com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionGenerateRequest;
import static com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = PromotionModifyRequest.class, name = "modify"),
	@JsonSubTypes.Type(value = PromotionGenerateRequest.class, name = "generate")
})
public sealed interface PromotionHandleRequest
	permits PromotionModifyRequest, PromotionGenerateRequest {
	AdminCarouselNumber carouselNumber();

	String newImageUrl();

	boolean isExternal();

	String redirectUrl();

	Long performanceId();
}
