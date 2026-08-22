package com.beat.admin

import com.beat.admin.promotion.api.request.AdminCarouselNumber
import com.beat.admin.promotion.api.request.CarouselHandleRequest
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionGenerateRequest
import com.beat.admin.promotion.api.request.CarouselHandleRequest.PromotionModifyRequest
import com.beat.admin.promotion.api.response.CarouselFindAllResponse
import com.beat.admin.promotion.api.response.CarouselHandleAllResponse
import com.beat.admin.promotion.api.response.CarouselPresignedUrlFindAllResponse
import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.AdminPromotionResults.AdminPromotionResult
import com.beat.application.admin.promotion.PromotionImageUpload
import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.admin.user.api.response.UserFindAllResponse
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.admin.promotion.api.request.PromotionHandleRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class AdminJsonCompatibilitySpec : FunSpec() {

    private val objectMapper = jacksonObjectMapper()

    init {
        isolationMode = IsolationMode.SingleInstance

        test("admin carousel numbers keep the domain enum names") {
            AdminCarouselNumber.entries.map { it.name } shouldBe CarouselNumber.entries.map { it.name }
        }

        test("carousel requests accept canonical and temporary external field names") {
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
            request.carousels!!.size shouldBe 1
            val modifyRequest = request.carousels[0] as PromotionModifyRequest
            modifyRequest.carouselNumber shouldBe AdminCarouselNumber.THREE
            modifyRequest.isExternal shouldBe true

            val aliasRequest = objectMapper.readValue(
                json.replace("\"isExternal\"", "\"external\""),
                CarouselHandleRequest::class.java,
            )
            val aliasModifyRequest = aliasRequest.carousels!![0] as PromotionModifyRequest
            aliasModifyRequest.isExternal shouldBe true
        }

        test("nullable carousel request fields retain validation counts and messages") {
            val validator = Validation.buildDefaultValidatorFactory().validator
            val missingCarousels = validator.validate(CarouselHandleRequest(null))
            val missingRequiredItemFields = validator.validate(
                CarouselHandleRequest(
                    listOf<PromotionHandleRequest?>(PromotionGenerateRequest(null, null, null, null, null)),
                ),
            )
            val nullCarousel = validator.validate(CarouselHandleRequest(listOf<PromotionHandleRequest?>(null)))

            missingCarousels.size shouldBe 1
            missingRequiredItemFields.size shouldBe 4
            nullCarousel.size shouldBe 1
            missingCarousels.all { it.message == INVALID_REQUEST_MESSAGE } shouldBe true
            missingRequiredItemFields.all { it.message == INVALID_REQUEST_MESSAGE } shouldBe true
        }

        test("response JSON keeps legacy collection names") {
            val userResponse = UserFindAllResponse(
                listOf(UserFindAllResponse.UserFindResponse(1L, "ROLE_USER")),
            )
            val userJson = objectMapper.valueToTree<JsonNode>(userResponse)
            userJson.has("users") shouldBe true
            userJson.has("userResponses") shouldBe false

            val promotionResults = AdminPromotionResults(
                listOf(AdminPromotionResult(1L, "ONE", "image", false, "redirect", 11L)),
            )
            val carouselFindJson = objectMapper.valueToTree<JsonNode>(CarouselFindAllResponse(promotionResults))
            val carouselHandleJson = objectMapper.valueToTree<JsonNode>(CarouselHandleAllResponse(promotionResults))

            carouselFindJson.has("carousels") shouldBe true
            carouselFindJson.has("carouselResponses") shouldBe false
            carouselHandleJson.has("modifiedPromotions") shouldBe true
            carouselHandleJson.has("modifiedPromotionResponses") shouldBe false
        }

        test("presigned response keeps the legacy URL map and explicit upload metadata") {
            val response = CarouselPresignedUrlFindAllResponse(
                CarouselPresignedUrlsResult(
                    mapOf(
                        "carousel.png" to PromotionImageUpload(
                            "signed-upload-url",
                            "dev/carousel/carousel.png",
                        ),
                    ),
                ),
            )

            response.carouselPresignedUrls shouldBe mapOf("carousel.png" to "signed-upload-url")
            response.carouselPresignedUploads["carousel.png"]!!.uploadUrl shouldBe "signed-upload-url"
            response.carouselPresignedUploads["carousel.png"]!!.imageKey shouldBe "dev/carousel/carousel.png"
        }
    }

    private companion object {
        const val INVALID_REQUEST_MESSAGE = "잘못된 요청 형식입니다."
    }
}
