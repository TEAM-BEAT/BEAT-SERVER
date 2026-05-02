package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.apis.performance.application.dto.home.HomeFindAllResponse;
import com.beat.apis.performance.application.dto.home.HomePerformanceDetail;
import com.beat.apis.performance.application.dto.home.HomePromotionDetail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ApisDtoJsonContractTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void homeResponseJsonFieldNamesAndEnumValuesStayCompatible() {
		HomeFindAllResponse response = HomeFindAllResponse.of(
			List.of(HomePromotionDetail.of(1L, "promotion.png", 11L, "redirect", true, "ONE")),
			List.of(HomePerformanceDetail.of(11L, "title", "period", 30000, 3, "BAND", "poster.png", "venue"))
		);

		JsonNode json = objectMapper.valueToTree(response);
		JsonNode promotion = json.get("promotionList").get(0);
		JsonNode performance = json.get("performanceList").get(0);

		assertTrue(json.has("promotionList"));
		assertTrue(json.has("performanceList"));
		assertTrue(promotion.has("carouselNumber"));
		assertTrue(performance.has("genre"));
		assertFalse(promotion.get("carouselNumber").isObject());
		assertFalse(performance.get("genre").isObject());
		assertEquals("ONE", promotion.get("carouselNumber").asText());
		assertEquals("BAND", performance.get("genre").asText());
	}
}
