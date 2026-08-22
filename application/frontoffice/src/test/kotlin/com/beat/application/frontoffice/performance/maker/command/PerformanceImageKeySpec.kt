package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito

class PerformanceImageKeySpec : FunSpec({
    lateinit var performanceImageStorage: PerformanceImageStorage

    beforeTest {
        performanceImageStorage = Mockito.mock(PerformanceImageStorage::class.java)
    }

    test("validates an absolute image URL after extracting its storage key") {
        val imageKey = "dev/poster/poster.png"
        Mockito.`when`(performanceImageStorage.exists(imageKey)).thenReturn(true)

        val result = validateStoredPerformanceImage(
            performanceImageStorage,
            "https://example.com/$imageKey",
            "poster",
        )

        result shouldBe imageKey
    }

    test("rejects an uploaded image from another category") {
        val exception = shouldThrow<FrontofficeApplicationException> {
            validateStoredPerformanceImage(performanceImageStorage, "dev/staff/staff.png", "cast")
        }

        exception.errorCode shouldBe PerformanceApplicationErrorCode.INVALID_IMAGE_KEY
        Mockito.verifyNoInteractions(performanceImageStorage)
    }

    test("rejects an image missing from object storage") {
        val imageKey = "dev/performance/detail.png"
        Mockito.`when`(performanceImageStorage.exists(imageKey)).thenReturn(false)

        val exception = shouldThrow<FrontofficeApplicationException> {
            validateStoredPerformanceImage(performanceImageStorage, imageKey, "performance")
        }

        exception.errorCode shouldBe PerformanceApplicationErrorCode.INVALID_IMAGE_KEY
    }

    test("allows an empty optional image without object storage access") {
        val result = validateStoredPerformanceImage(
            performanceImageStorage = performanceImageStorage,
            value = "",
            category = "staff",
            required = false,
        )

        result shouldBe ""
        Mockito.verifyNoInteractions(performanceImageStorage)
    }
})
