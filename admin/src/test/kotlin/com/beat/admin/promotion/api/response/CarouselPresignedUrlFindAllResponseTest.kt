package com.beat.admin.promotion.api.response

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.contracts.storage.CarouselPresignedUpload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CarouselPresignedUrlFindAllResponseTest {

    @Test
    fun responsePreservesLegacyUrlsAndAddsExplicitUploadMetadata() {
        val response = CarouselPresignedUrlFindAllResponse(
            CarouselPresignedUrlsResult(
                mapOf("carousel.png" to CarouselPresignedUpload.of("signed-upload-url", "dev/carousel/carousel.png")),
            ),
        )

        assertEquals(mapOf("carousel.png" to "signed-upload-url"), response.carouselPresignedUrls)
        assertEquals("signed-upload-url", response.carouselPresignedUploads["carousel.png"]!!.uploadUrl)
        assertEquals("dev/carousel/carousel.png", response.carouselPresignedUploads["carousel.png"]!!.imageKey)
    }
}