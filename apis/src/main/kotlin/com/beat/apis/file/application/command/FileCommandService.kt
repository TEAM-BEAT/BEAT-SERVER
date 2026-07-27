package com.beat.apis.file.application.command

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.file.exception.FileApplicationErrorCode
import com.beat.contracts.storage.FileStoragePort
import com.beat.contracts.storage.PerformancePresignedUrls
import org.springframework.stereotype.Service

@Service
class FileCommandService(
    private val fileStoragePort: FileStoragePort,
) {
    fun issueAllPresignedUrlsForPerformanceMaker(
        posterImage: String,
        castImages: List<String>?,
        staffImages: List<String>?,
        performanceImages: List<String>?,
    ): PerformancePresignedUrls {
        val normalizedCastImages = castImages.orEmpty().filter(String::isNotBlank)
        val normalizedStaffImages = staffImages.orEmpty().filter(String::isNotBlank)
        val normalizedPerformanceImages = performanceImages.orEmpty().filter(String::isNotBlank)
        val fileNames = listOf(posterImage) + normalizedCastImages + normalizedStaffImages + normalizedPerformanceImages
        if (fileNames.any { !isValidFileName(it) }) {
            throw ApiApplicationException(FileApplicationErrorCode.INVALID_FILE_NAME)
        }

        return fileStoragePort.issueAllPresignedUrlsForPerformanceMaker(
            posterImage,
            normalizedCastImages,
            normalizedStaffImages,
            normalizedPerformanceImages,
        )
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
