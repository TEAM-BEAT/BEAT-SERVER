package com.beat.apis.web.jackson

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.json.JsonMapper

class CdnImageUrlSerializerSpec : FunSpec({
    val objectMapper = JsonMapper.builder().build()

    afterEach {
        CdnImageUrlSerializer.initialize(null)
    }

    test("relative image key is serialized with the configured CDN domain") {
        CdnImageUrlSerializer.initialize("https://cdn.beatlive.kr/")

        objectMapper.writeValueAsString(ImageResponse("/prod/poster/image.jpg")) shouldBe
            """{"imageUrl":"https://cdn.beatlive.kr/prod/poster/image.jpg"}"""
    }

    test("absolute URL and blank value remain unchanged") {
        CdnImageUrlSerializer.initialize("https://cdn.beatlive.kr")

        objectMapper.writeValueAsString(ImageResponse("https://external.test/image.jpg")) shouldBe
            """{"imageUrl":"https://external.test/image.jpg"}"""
        objectMapper.writeValueAsString(ImageResponse("")) shouldBe """{"imageUrl":""}"""
    }
})

private data class ImageResponse(
    @get:CdnImageUrl
    val imageUrl: String,
)
