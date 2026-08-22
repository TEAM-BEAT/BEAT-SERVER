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

    test("issueAllPresignedUrls normalizes nullable lists before calling storage") {
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

    test("issueAllPresignedUrls ignores legacy empty list placeholders") {
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

    test("issueAllPresignedUrls rejects path-like file names before calling storage") {
        val exception = shouldThrow<FrontofficeApplicationException> {
            fileService.issueAllPresignedUrlsForPerformanceMaker("../poster.png", null, null, null)
        }

        exception.errorCode shouldBe FileApplicationErrorCode.INVALID_FILE_NAME
        Mockito.verifyNoInteractions(performanceImageStorage)
    }
})
