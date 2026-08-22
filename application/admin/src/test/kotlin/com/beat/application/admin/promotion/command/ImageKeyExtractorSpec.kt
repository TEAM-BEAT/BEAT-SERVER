package com.beat.application.admin.promotion.command

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImageKeyExtractorSpec : FunSpec({
    test("namespaced image key is preserved") {
        ImageKeyExtractor.extract("prod/poster/image.jpg") shouldBe "prod/poster/image.jpg"
        ImageKeyExtractor.extract("dev/carousel/image.jpg") shouldBe "dev/carousel/image.jpg"
    }

    test("absolute CDN URL is normalized to its namespaced key") {
        ImageKeyExtractor.extract("https://cdn.beatlive.kr/prod/poster/image.jpg") shouldBe
            "prod/poster/image.jpg"
    }

    test("legacy key without environment namespace is rejected") {
        shouldThrow<IllegalArgumentException> {
            ImageKeyExtractor.extract("poster/image.jpg")
        }
    }
})
