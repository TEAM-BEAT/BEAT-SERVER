package com.beat.global.support.utils

import java.net.URI

object ImageKeyExtractor {

    // CloudFront viewer-request ALLOWED_PREFIXES / ALLOWED_ENV_PREFIXES 와 정확히 일치해야 함.
    private val ALLOWED_PREFIXES: Set<String> =
        setOf("poster", "cast", "staff", "performance", "carousel", "banner")
    private val ALLOWED_ENV_PREFIXES: Set<String> = setOf("dev", "prod")

    @JvmStatic
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
    } catch (e: IllegalArgumentException) {
        absoluteUrl
    }

    private fun requireAllowedPrefix(key: String) {
        val segments = key.split("/", limit = 3)
        val envPrefix = segments.getOrNull(0).orEmpty()
        val prefix = segments.getOrNull(1).orEmpty()
        require(envPrefix in ALLOWED_ENV_PREFIXES) {
            "Invalid image key environment prefix: '$envPrefix' (allowed: $ALLOWED_ENV_PREFIXES)"
        }
        require(prefix in ALLOWED_PREFIXES) {
            "Invalid image key prefix: '$prefix' (allowed: $ALLOWED_PREFIXES)"
        }
    }

    private fun isAbsoluteUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
}
