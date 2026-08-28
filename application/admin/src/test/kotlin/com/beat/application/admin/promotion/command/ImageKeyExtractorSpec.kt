package com.beat.application.admin.promotion.command

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImageKeyExtractorSpec :
    FunSpec({
        test("네임스페이스가 있는 image key는 그대로 유지된다") {
            ImageKeyExtractor.extract("prod/poster/image.jpg") shouldBe "prod/poster/image.jpg"
            ImageKeyExtractor.extract("dev/carousel/image.jpg") shouldBe "dev/carousel/image.jpg"
        }

        test("절대 CDN URL은 네임스페이스 key로 정규화된다") {
            ImageKeyExtractor.extract("https://cdn.beatlive.kr/prod/poster/image.jpg") shouldBe
                "prod/poster/image.jpg"
        }

        test("환경 네임스페이스가 없는 레거시 key는 거부된다") {
            val exception =
                shouldThrow<AdminApplicationException> {
                    ImageKeyExtractor.extract("poster/image.jpg")
                }
            exception.errorCode shouldBe PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT
        }
    })
