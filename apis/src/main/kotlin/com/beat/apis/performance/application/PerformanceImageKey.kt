package com.beat.apis.performance.application

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.contracts.storage.FileStoragePort
import com.beat.global.support.utils.ImageKeyExtractor

internal fun extractPerformanceImageKey(value: String?): String? =
    try {
        ImageKeyExtractor.extract(value)
    } catch (exception: IllegalArgumentException) {
        throw ApiApplicationException(
            errorCode = PerformanceApplicationErrorCode.INVALID_IMAGE_KEY,
            cause = exception,
        )
    }

internal fun extractRequiredPerformanceImageKey(value: String?): String =
    extractPerformanceImageKey(value)
        ?: throw ApiApplicationException(PerformanceApplicationErrorCode.INVALID_IMAGE_KEY)

internal fun validateStoredPerformanceImage(
    fileStoragePort: FileStoragePort,
    value: String?,
    category: String,
    required: Boolean = true,
): String {
    if (!required && value.isNullOrBlank()) return ""
    val imageKey = extractRequiredPerformanceImageKey(value)
    if (imageKey.split("/", limit = 3).getOrNull(1) != category ||
        fileStoragePort.findImageObjectMetadata(imageKey) == null
    ) {
        throw ApiApplicationException(PerformanceApplicationErrorCode.INVALID_IMAGE_KEY)
    }
    return imageKey
}
