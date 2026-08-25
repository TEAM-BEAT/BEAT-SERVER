package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import org.springframework.stereotype.Service

@Service
class FileCommandService
internal constructor(private val performanceImageStorage: PerformanceImageStorage) {
    fun issueAllPresignedUrlsForPerformanceMaker(
        posterImage: String,
        castImages: List<String>?,
        staffImages: List<String>?,
        performanceImages: List<String>?,
    ): PerformancePresignedUrls {
        return translateDomainFailure {
            val normalizedCastImages = castImages.orEmpty().filter(String::isNotBlank)
            val normalizedStaffImages = staffImages.orEmpty().filter(String::isNotBlank)
            val normalizedPerformanceImages = performanceImages.orEmpty().filter(String::isNotBlank)
            val fileNames =
                listOf(posterImage) +
                    normalizedCastImages +
                    normalizedStaffImages +
                    normalizedPerformanceImages
            if (fileNames.any { !isValidFileName(it) }) {
                throw FrontofficeApplicationException(FileApplicationErrorCode.INVALID_FILE_NAME)
            }

            performanceImageStorage.issueAllPresignedUrls(
                posterImage,
                normalizedCastImages,
                normalizedStaffImages,
                normalizedPerformanceImages,
            )
        }
    }

    private fun isValidFileName(fileName: String): Boolean =
        fileName.isNotBlank() &&
            fileName.length <= MAX_FILE_NAME_LENGTH &&
            '/' !in fileName &&
            '\\' !in fileName &&
            fileName.none(Char::isISOControl)

    private companion object {
        const val MAX_FILE_NAME_LENGTH = 255
    }
}
