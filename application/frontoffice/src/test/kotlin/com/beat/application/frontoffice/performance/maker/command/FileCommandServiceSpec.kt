package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
class FileCommandServiceSpec : FunSpec({
    lateinit var performanceImageStorage: PerformanceImageStorage
    lateinit var fileService: FileCommandService

    beforeTest {
        performanceImageStorage = Mockito.mock(PerformanceImageStorage::class.java)
        fileService = FileCommandService(performanceImageStorage)
    }

    test("issueAllPresignedUrls는 storage 호출 전에 null 리스트를 정규화한다") {
        val presignedUrls = PerformancePresignedUrls(
            mapOf(
                "poster" to mapOf(
                    "poster.png" to ImagePresignedUpload.of(
                        "https://example.com/poster.png",
                        "dev/poster/poster.png",
                    ),
                ),
            ),
        )
        Mockito.`when`(
            performanceImageStorage.issueAllPresignedUrls("poster.png", emptyList(), emptyList(), emptyList()),
        ).thenReturn(presignedUrls)

        fileService.issueAllPresignedUrlsForPerformanceMaker("poster.png", null, null, null)

        Mockito.verify(performanceImageStorage).issueAllPresignedUrls(
            "poster.png",
            emptyList(),
            emptyList(),
            emptyList(),
        )
    }

    test("issueAllPresignedUrls는 레거시 빈 리스트 placeholder를 무시한다") {
        fileService.issueAllPresignedUrlsForPerformanceMaker(
            "poster.png",
            listOf(""),
            listOf(""),
            listOf(""),
        )

        Mockito.verify(performanceImageStorage).issueAllPresignedUrls(
            "poster.png",
            emptyList(),
            emptyList(),
            emptyList(),
        )
    }

    test("issueAllPresignedUrls는 storage 호출 전에 경로 형태의 파일명을 거부한다") {
        val exception = shouldThrow<FrontofficeApplicationException> {
            fileService.issueAllPresignedUrlsForPerformanceMaker("../poster.png", null, null, null)
        }

        exception.errorCode shouldBe FileApplicationErrorCode.INVALID_FILE_NAME
        Mockito.verifyNoInteractions(performanceImageStorage)
    }
})
