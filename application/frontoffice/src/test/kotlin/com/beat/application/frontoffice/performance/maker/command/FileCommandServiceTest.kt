package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FileCommandServiceTest {

    @Mock
    private lateinit var performanceImageStorage: PerformanceImageStorage

    private lateinit var fileService: FileCommandService

    @BeforeEach
    fun setUp() {
        fileService = FileCommandService(performanceImageStorage)
    }

    @Test
    fun `issueAllPresignedUrls normalizes nullable lists before calling storage`() {
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

    @Test
    fun `issueAllPresignedUrls ignores legacy empty list placeholders`() {
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

    @Test
    fun `issueAllPresignedUrls rejects path-like file names before calling storage`() {
        val exception = assertThrows<FrontofficeApplicationException> {
            fileService.issueAllPresignedUrlsForPerformanceMaker("../poster.png", null, null, null)
        }

        assertEquals(FileApplicationErrorCode.INVALID_FILE_NAME, exception.errorCode)
        Mockito.verifyNoInteractions(performanceImageStorage)
    }
}
