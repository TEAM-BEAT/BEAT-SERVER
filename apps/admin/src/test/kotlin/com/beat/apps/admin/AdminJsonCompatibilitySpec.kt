package com.beat.apps.admin

import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.AdminPromotionResults.AdminPromotionResult
import com.beat.application.admin.promotion.command.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.application.admin.promotion.command.PromotionImageUpload
import com.beat.apps.admin.promotion.api.request.AdminCarouselNumber
import com.beat.apps.admin.promotion.api.request.CarouselHandleRequest
import com.beat.apps.admin.promotion.api.request.CarouselHandleRequest.PromotionModifyRequest
import com.beat.apps.admin.promotion.api.response.CarouselFindAllResponse
import com.beat.apps.admin.promotion.api.response.CarouselHandleAllResponse
import com.beat.apps.admin.promotion.api.response.CarouselPresignedUrlFindAllResponse
import com.beat.apps.admin.user.api.response.UserFindAllResponse
import com.beat.domain.promotion.model.CarouselNumber
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AdminJsonCompatibilitySpec : FunSpec() {

    private val objectMapper = jacksonObjectMapper()

    init {
        isolationMode = IsolationMode.SingleInstance

        test("admin carousel 번호는 domain enum 이름을 유지한다") {
            AdminCarouselNumber.entries.map { it.name } shouldBe
                CarouselNumber.entries.map { it.name }
        }

        test("carousel 요청은 표준 필드명과 임시 외부 필드명을 모두 허용한다") {
            val json =
                """
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
                """
                    .trimIndent()

            val request = objectMapper.readValue(json, CarouselHandleRequest::class.java)
            request.carousels!!.size shouldBe 1
            val modifyRequest = request.carousels[0] as PromotionModifyRequest
            modifyRequest.carouselNumber shouldBe AdminCarouselNumber.THREE
            modifyRequest.isExternal shouldBe true

            val aliasRequest =
                objectMapper.readValue(
                    json.replace("\"isExternal\"", "\"external\""),
                    CarouselHandleRequest::class.java,
                )
            val aliasModifyRequest = aliasRequest.carousels!![0] as PromotionModifyRequest
            aliasModifyRequest.isExternal shouldBe true
        }

        test("carousel 요청의 필수 필드는 역직렬화 단계에서 누락과 null을 거부한다") {
            // Kotlin non-null 프로퍼티가 타입으로 필수성을 보증한다(Bean Validation 불필요).
            shouldThrow<Exception> {
                objectMapper.readValue("""{"carousels":null}""", CarouselHandleRequest::class.java)
            }
            shouldThrow<Exception> {
                objectMapper.readValue(
                    """{"carousels":[{"type":"generate","carouselNumber":null,"newImageUrl":"u","isExternal":true,"redirectUrl":"r"}]}""",
                    CarouselHandleRequest::class.java,
                )
            }
        }

        test("response JSON은 레거시 컬렉션 이름을 유지한다") {
            val userResponse =
                UserFindAllResponse(listOf(UserFindAllResponse.UserFindResponse(1L, "ROLE_USER")))
            val userJson = objectMapper.valueToTree<JsonNode>(userResponse)
            userJson.has("users") shouldBe true
            userJson.has("userResponses") shouldBe false

            val promotionResults =
                AdminPromotionResults(
                    listOf(AdminPromotionResult(1L, "ONE", "image", false, "redirect", 11L))
                )
            val carouselFindJson =
                objectMapper.valueToTree<JsonNode>(CarouselFindAllResponse(promotionResults))
            val carouselHandleJson =
                objectMapper.valueToTree<JsonNode>(CarouselHandleAllResponse(promotionResults))

            carouselFindJson.has("carousels") shouldBe true
            carouselFindJson.has("carouselResponses") shouldBe false
            carouselHandleJson.has("modifiedPromotions") shouldBe true
            carouselHandleJson.has("modifiedPromotionResponses") shouldBe false
        }

        test("presigned response는 레거시 URL 맵과 명시적인 upload 메타데이터를 유지한다") {
            val response =
                CarouselPresignedUrlFindAllResponse(
                    CarouselPresignedUrlsResult(
                        mapOf(
                            "carousel.png" to
                                PromotionImageUpload(
                                    "signed-upload-url",
                                    "dev/carousel/carousel.png",
                                )
                        )
                    )
                )

            response.carouselPresignedUrls shouldBe mapOf("carousel.png" to "signed-upload-url")
            response.carouselPresignedUploads["carousel.png"]!!.uploadUrl shouldBe
                "signed-upload-url"
            response.carouselPresignedUploads["carousel.png"]!!.imageKey shouldBe
                "dev/carousel/carousel.png"

            val json = objectMapper.valueToTree<JsonNode>(response)
            json.get("carouselPresignedUrls").isObject shouldBe true
            json.get("carouselPresignedUrls").get("carousel.png").asText() shouldBe
                "signed-upload-url"
            json.get("carouselPresignedUploads").isObject shouldBe true
            json.get("carouselPresignedUploads").get("carousel.png").isObject shouldBe true
            json
                .get("carouselPresignedUploads")
                .get("carousel.png")
                .get("uploadUrl")
                .asText() shouldBe "signed-upload-url"
            json
                .get("carouselPresignedUploads")
                .get("carousel.png")
                .get("imageKey")
                .asText() shouldBe "dev/carousel/carousel.png"
        }
    }

    private companion object {
        const val INVALID_REQUEST_MESSAGE = "잘못된 요청 형식입니다."
    }
}
