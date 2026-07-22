package com.beat.apis.performance.application

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
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
