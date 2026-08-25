package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PerformanceImageKeySpec :
    FunSpec({
        test("storage key를 추출한 뒤 절대 image URL을 검증한다") {
            val performanceImageStorage = mockk<PerformanceImageStorage>(relaxed = true)
            val imageKey = "dev/poster/poster.png"
            every { performanceImageStorage.exists(imageKey) } returns true

            val result =
                validateStoredPerformanceImage(
                    performanceImageStorage,
                    "https://example.com/$imageKey",
                    "poster",
                )

            result shouldBe imageKey
        }

        test("다른 카테고리에서 업로드된 image는 거부된다") {
            val performanceImageStorage = mockk<PerformanceImageStorage>(relaxed = true)

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    validateStoredPerformanceImage(
                        performanceImageStorage,
                        "dev/staff/staff.png",
                        "cast",
                    )
                }

            exception.errorCode shouldBe PerformanceApplicationErrorCode.INVALID_IMAGE_KEY
            verify { performanceImageStorage wasNot Called }
        }

        test("object storage에 없는 image는 거부된다") {
            val performanceImageStorage = mockk<PerformanceImageStorage>(relaxed = true)
            val imageKey = "dev/performance/detail.png"
            every { performanceImageStorage.exists(imageKey) } returns false

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    validateStoredPerformanceImage(performanceImageStorage, imageKey, "performance")
                }

            exception.errorCode shouldBe PerformanceApplicationErrorCode.INVALID_IMAGE_KEY
        }

        test("빈 선택 image는 object storage 접근 없이 허용된다") {
            val performanceImageStorage = mockk<PerformanceImageStorage>(relaxed = true)

            val result =
                validateStoredPerformanceImage(
                    performanceImageStorage = performanceImageStorage,
                    value = "",
                    category = "staff",
                    required = false,
                )

            result shouldBe ""
            verify { performanceImageStorage wasNot Called }
        }
    })
