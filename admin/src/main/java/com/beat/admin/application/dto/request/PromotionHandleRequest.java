package com.beat.admin.application.dto.request;

import static com.beat.admin.application.dto.request.CarouselHandleRequest.*;

import com.beat.domain.promotion.domain.CarouselNumber;
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
	CarouselNumber carouselNumber();

	String newImageUrl();

	boolean isExternal();

	String redirectUrl();

	Long performanceId();
}