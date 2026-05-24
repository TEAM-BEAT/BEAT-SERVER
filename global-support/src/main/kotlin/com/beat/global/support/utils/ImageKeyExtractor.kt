package com.beat.global.support.utils

import java.net.URI

/**
 * Pure utility: extract the storage key (URI path without leading slash) from
 * a full S3/CDN URL and verify it lives under a known image prefix. Idempotent —
 * applying twice yields the same key. Returns the input unchanged when it is
 * null/blank. Throws [IllegalArgumentException] when the resulting key does not
 * start with one of the prefixes the S3 PresignedUrl flow can produce, blocking
 * arbitrary external URLs from being persisted (mirrors the CloudFront
 * viewer-request `ALLOWED_PREFIXES` whitelist).
 */
object ImageKeyExtractor {

    private val ALLOWED_PREFIXES: Set<String> =
        setOf("poster", "cast", "staff", "performance", "carousel", "banner")

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
        val prefix = key.substringBefore('/', missingDelimiterValue = "")
        require(prefix in ALLOWED_PREFIXES) {
            "Invalid image key prefix: '$prefix' (allowed: $ALLOWED_PREFIXES)"
        }
    }

    private fun isAbsoluteUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
}
