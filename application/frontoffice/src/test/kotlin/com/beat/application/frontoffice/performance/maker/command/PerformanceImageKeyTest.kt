package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class PerformanceImageKeyTest {

    private val performanceImageStorage = Mockito.mock(PerformanceImageStorage::class.java)

    @Test
    fun `validates an absolute image URL after extracting its storage key`() {
        val imageKey = "dev/poster/poster.png"
        Mockito.`when`(performanceImageStorage.exists(imageKey)).thenReturn(true)

        val result = validateStoredPerformanceImage(
            performanceImageStorage,
            "https://example.com/$imageKey",
            "poster",
        )

        assertEquals(imageKey, result)
    }

    @Test
    fun `rejects an uploaded image from another category`() {
        val exception = assertThrows<FrontofficeApplicationException> {
            validateStoredPerformanceImage(performanceImageStorage, "dev/staff/staff.png", "cast")
        }

        assertEquals(PerformanceApplicationErrorCode.INVALID_IMAGE_KEY, exception.errorCode)
        Mockito.verifyNoInteractions(performanceImageStorage)
    }

    @Test
    fun `rejects an image missing from object storage`() {
        val imageKey = "dev/performance/detail.png"
        Mockito.`when`(performanceImageStorage.exists(imageKey)).thenReturn(false)

        val exception = assertThrows<FrontofficeApplicationException> {
            validateStoredPerformanceImage(performanceImageStorage, imageKey, "performance")
        }

        assertEquals(PerformanceApplicationErrorCode.INVALID_IMAGE_KEY, exception.errorCode)
    }

    @Test
    fun `allows an empty optional image without object storage access`() {
        val result = validateStoredPerformanceImage(
            performanceImageStorage = performanceImageStorage,
            value = "",
            category = "staff",
            required = false,
        )

        assertEquals("", result)
        Mockito.verifyNoInteractions(performanceImageStorage)
    }
}
