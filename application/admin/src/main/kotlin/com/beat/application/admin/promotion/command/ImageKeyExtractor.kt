package com.beat.application.admin.promotion.command

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import java.net.URI

internal object ImageKeyExtractor {
    private val allowedPrefixes = setOf("poster", "cast", "staff", "performance", "carousel", "banner")
    private val allowedEnvironmentPrefixes = setOf("dev", "prod")

    fun extract(value: String?): String? {
        if (value.isNullOrBlank()) {
            return value
        }
        val key = if (isAbsoluteUrl(value)) toKey(value) else value
        requireAllowedPrefix(key)
        return key
    }

    private fun toKey(absoluteUrl: String): String = try {
        val path = URI.create(absoluteUrl).path
        when {
            path.isNullOrEmpty() -> absoluteUrl
            path.startsWith("/") -> path.drop(1)
            else -> path
        }
    } catch (_: IllegalArgumentException) {
        absoluteUrl
    }

    private fun requireAllowedPrefix(key: String) {
        val segments = key.split("/", limit = 3)
        val environmentPrefix = segments.getOrNull(0).orEmpty()
        val prefix = segments.getOrNull(1).orEmpty()
        if (environmentPrefix !in allowedEnvironmentPrefixes) {
            throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
        }
        if (prefix !in allowedPrefixes) {
            throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
        }
    }

    private fun isAbsoluteUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
}
