package com.beat.application.admin.promotion.command

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
        require(environmentPrefix in allowedEnvironmentPrefixes) {
            "Invalid image key environment prefix: '$environmentPrefix' (allowed: $allowedEnvironmentPrefixes)"
        }
        require(prefix in allowedPrefixes) {
            "Invalid image key prefix: '$prefix' (allowed: $allowedPrefixes)"
        }
    }

    private fun isAbsoluteUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
}
