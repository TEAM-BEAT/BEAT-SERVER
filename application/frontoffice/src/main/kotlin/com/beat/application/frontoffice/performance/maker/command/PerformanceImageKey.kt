package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.performance.exception.PerformanceApplicationErrorCode
import java.net.URI

private val allowedImagePrefixes = setOf("poster", "cast", "staff", "performance", "carousel", "banner")
private val allowedImageEnvironmentPrefixes = setOf("dev", "prod")

internal fun extractPerformanceImageKey(value: String?): String? =
    try {
        if (value.isNullOrBlank()) {
            value
        } else {
            val key = if (value.isAbsoluteImageUrl()) value.toImageKey() else value
            key.requireAllowedImagePrefix()
            key
        }
    } catch (exception: IllegalArgumentException) {
        throw FrontofficeApplicationException(
            errorCode = PerformanceApplicationErrorCode.INVALID_IMAGE_KEY,
            cause = exception,
        )
    }

private fun String.isAbsoluteImageUrl(): Boolean =
    startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

private fun String.toImageKey(): String = try {
    val path = URI.create(this).path
    when {
        path.isNullOrEmpty() -> this
        path.startsWith("/") -> path.drop(1)
        else -> path
    }
} catch (_: IllegalArgumentException) {
    this
}

private fun String.requireAllowedImagePrefix() {
    val segments = split("/", limit = 3)
    val environmentPrefix = segments.getOrNull(0).orEmpty()
    val imagePrefix = segments.getOrNull(1).orEmpty()
    require(environmentPrefix in allowedImageEnvironmentPrefixes) {
        "Invalid image key environment prefix: '$environmentPrefix' (allowed: $allowedImageEnvironmentPrefixes)"
    }
    require(imagePrefix in allowedImagePrefixes) {
        "Invalid image key prefix: '$imagePrefix' (allowed: $allowedImagePrefixes)"
    }
}

internal fun extractRequiredPerformanceImageKey(value: String?): String =
    extractPerformanceImageKey(value)
        ?: throw FrontofficeApplicationException(PerformanceApplicationErrorCode.INVALID_IMAGE_KEY)

internal fun validateStoredPerformanceImage(
    performanceImageStorage: PerformanceImageStorage,
    value: String?,
    category: String,
    required: Boolean = true,
): String {
    if (!required && value.isNullOrBlank()) return ""
    val imageKey = extractRequiredPerformanceImageKey(value)
    if (imageKey.split("/", limit = 3).getOrNull(1) != category ||
        !performanceImageStorage.exists(imageKey)
    ) {
        throw FrontofficeApplicationException(PerformanceApplicationErrorCode.INVALID_IMAGE_KEY)
    }
    return imageKey
}
