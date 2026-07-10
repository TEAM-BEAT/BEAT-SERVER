package com.beat.global.support.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ImageKeyExtractorTest {

    @Test
    fun `extract accepts namespaced image keys`() {
        assertEquals("prod/poster/image.jpg", ImageKeyExtractor.extract("prod/poster/image.jpg"))
        assertEquals("dev/carousel/image.jpg", ImageKeyExtractor.extract("dev/carousel/image.jpg"))
    }

    @Test
    fun `extract accepts namespaced absolute image urls`() {
        assertEquals(
            "prod/poster/image.jpg",
            ImageKeyExtractor.extract("https://cdn.beatlive.kr/prod/poster/image.jpg")
        )
    }

    @Test
    fun `extract rejects legacy category only keys`() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageKeyExtractor.extract("poster/image.jpg")
        }
    }
}
