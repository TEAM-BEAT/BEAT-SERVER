package com.beat.admin.application

import com.beat.admin.promotion.api.request.AdminCarouselNumber
import com.beat.admin.promotion.api.request.CarouselHandleRequest
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionGenerateRequest
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionModifyRequest
import com.beat.admin.promotion.api.response.CarouselFindAllResponse
import com.beat.admin.promotion.api.response.CarouselHandleAllResponse
import com.beat.admin.promotion.application.result.AdminPromotionResults
import com.beat.admin.promotion.application.result.AdminPromotionResults.AdminPromotionResult
import com.beat.admin.user.api.response.UserFindAllResponse
import com.beat.domain.promotion.model.CarouselNumber
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections

class AdminDtoJsonContractTest {

    private val objectMapper = ObjectMapper()

    @Test
    fun adminCarouselNumberKeepsLegacyDomainEnumNames() {
        val domainEnumNames = CarouselNumber.entries.map { it.name }
        val requestEnumNames = AdminCarouselNumber.entries.map { it.name }

        assertEquals(domainEnumNames, requestEnumNames)
    }

    @Test
    fun carouselHandleRequestAcceptsCanonicalAndTemporaryExternalFieldNames() {
        val json = """
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
        """.trimIndent()

        val request = objectMapper.readValue(json, CarouselHandleRequest::class.java)

        assertEquals(1, request.carousels!!.size)
        val modifyRequest = assertInstanceOf(PromotionModifyRequest::class.java, request.carousels[0])
        assertEquals(AdminCarouselNumber.THREE, modifyRequest.carouselNumber)
        assertTrue(modifyRequest.isExternal!!)

        val aliasRequest = objectMapper.readValue(
            json.replace("\"isExternal\"", "\"external\""),
            CarouselHandleRequest::class.java,
        )
        val aliasModifyRequest = assertInstanceOf(PromotionModifyRequest::class.java, aliasRequest.carousels!![0])
        assertTrue(aliasModifyRequest.isExternal!!)
    }

    @Test
    fun carouselHandleRequestValidatesNullableHttpFields() {
        val validator = Validation.buildDefaultValidatorFactory().validator
        val missingCarousels = validator.validate(CarouselHandleRequest(null))
        val missingRequiredItemFields = validator.validate(
            CarouselHandleRequest(listOf(PromotionGenerateRequest(null, null, null, null, null))),
        )
        val nullCarousel = validator.validate(
            CarouselHandleRequest(Collections.singletonList(null)),
        )

        assertEquals(1, missingCarousels.size)
        assertEquals(4, missingRequiredItemFields.size)
        assertEquals(1, nullCarousel.size)
        assertTrue(missingCarousels.all { it.message == "잘못된 요청 형식입니다." })
        assertTrue(missingRequiredItemFields.all { it.message == "잘못된 요청 형식입니다." })
    }

    @Test
    fun responseJsonFieldNamesStayLegacyCompatible() {
        val userResponse = UserFindAllResponse(listOf(UserFindAllResponse.UserFindResponse(1L, "ROLE_USER")))
        val userJson = objectMapper.valueToTree<JsonNode>(userResponse)

        assertTrue(userJson.has("users"))
        assertFalse(userJson.has("userResponses"))

        val promotionResults = AdminPromotionResults(listOf(AdminPromotionResult(1L, "ONE", "image", false, "redirect", 11L)))
        val carouselFindJson = objectMapper.valueToTree<JsonNode>(
            CarouselFindAllResponse(promotionResults),
        )
        val carouselHandleJson = objectMapper.valueToTree<JsonNode>(
            CarouselHandleAllResponse(promotionResults),
        )

        assertTrue(carouselFindJson.has("carousels"))
        assertFalse(carouselFindJson.has("carouselResponses"))
        assertTrue(carouselHandleJson.has("modifiedPromotions"))
        assertFalse(carouselHandleJson.has("modifiedPromotionResponses"))
    }
}