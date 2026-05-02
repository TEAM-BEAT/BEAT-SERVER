package com.beat.admin.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.admin.promotion.application.dto.request.AdminCarouselNumber;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest;
import com.beat.admin.promotion.application.dto.request.CarouselHandleRequest.PromotionModifyRequest;
import com.beat.admin.promotion.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.promotion.application.dto.response.CarouselHandleAllResponse;
import com.beat.admin.user.application.dto.response.UserFindAllResponse;
import com.beat.admin.promotion.application.dto.result.AdminPromotionResults;
import com.beat.admin.promotion.application.dto.result.AdminPromotionResults.AdminPromotionResult;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AdminDtoJsonContractTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void adminCarouselNumberKeepsLegacyDomainEnumNames() {
		List<String> domainEnumNames = Arrays.stream(CarouselNumber.values())
			.map(Enum::name)
			.toList();
		List<String> requestEnumNames = Arrays.stream(AdminCarouselNumber.values())
			.map(Enum::name)
			.toList();

		assertEquals(domainEnumNames, requestEnumNames);
	}

	@Test
	void carouselHandleRequestDeserializesLegacyCarouselNumberValue() throws Exception {
		String json = """
			{
			  "carousels": [
			    {
			      "type": "modify",
			      "promotionId": 1,
			      "carouselNumber": "THREE",
			      "newImageUrl": "image",
			      "isExternal": true,
			      "redirectUrl": "redirect",
			      "performanceId": 11
			    }
			  ]
			}
			""";

		CarouselHandleRequest request = objectMapper.readValue(json, CarouselHandleRequest.class);

		assertEquals(1, request.carousels().size());
		PromotionModifyRequest modifyRequest = assertInstanceOf(PromotionModifyRequest.class,
			request.carousels().get(0));
		assertEquals(AdminCarouselNumber.THREE, modifyRequest.carouselNumber());
	}

	@Test
	void responseJsonFieldNamesStayLegacyCompatible() {
		UserFindAllResponse userResponse = UserFindAllResponse.from(List.of(
			UserFindAllResponse.UserFindResponse.of(1L, "ROLE_USER")
		));
		JsonNode userJson = objectMapper.valueToTree(userResponse);

		assertTrue(userJson.has("users"));
		assertFalse(userJson.has("userResponses"));

		AdminPromotionResults promotionResults = AdminPromotionResults.from(List.of(
			AdminPromotionResult.of(1L, "ONE", "image", false, "redirect", 11L)
		));
		JsonNode carouselFindJson = objectMapper.valueToTree(CarouselFindAllResponse.from(promotionResults));
		JsonNode carouselHandleJson = objectMapper.valueToTree(CarouselHandleAllResponse.from(promotionResults));

		assertTrue(carouselFindJson.has("carousels"));
		assertFalse(carouselFindJson.has("carouselResponses"));
		assertTrue(carouselHandleJson.has("modifiedPromotions"));
		assertFalse(carouselHandleJson.has("modifiedPromotionResponses"));
	}
}
