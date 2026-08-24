package com.beat.apps.api.web.jackson

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.json.JsonMapper

class CdnImageUrlSerializerSpec : FunSpec({
    val objectMapper = JsonMapper.builder().build()

    afterEach {
        CdnImageUrlSerializer.initialize(null)
    }

    test("상대 경로 image key는 설정된 CDN 도메인과 함께 직렬화된다") {
        CdnImageUrlSerializer.initialize("https://cdn.beatlive.kr/")

        objectMapper.writeValueAsString(ImageResponse("/prod/poster/image.jpg")) shouldBe
            """{"imageUrl":"https://cdn.beatlive.kr/prod/poster/image.jpg"}"""
    }

    test("절대 URL과 빈 값은 그대로 유지된다") {
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
