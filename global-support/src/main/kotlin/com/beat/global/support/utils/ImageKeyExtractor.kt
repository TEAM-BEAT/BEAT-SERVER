package com.beat.global.support.utils

import java.net.URI

/**
 * Pure utility: extract the storage key (URI path without leading slash) from
 * a full S3/CDN URL. Idempotent — applying twice yields the same key. Returns
 * the input unchanged when it is null/blank or already a bare key.
 */
object ImageKeyExtractor {

    @JvmStatic
    fun extract(value: String?): String? {
        if (value.isNullOrBlank() || !isAbsoluteUrl(value)) {
            return value
        }
        return try {
            val path = URI.create(value).path
            when {
                path.isNullOrEmpty() -> value
                path.startsWith("/") -> path.drop(1)
                else -> path
            }
        } catch (e: IllegalArgumentException) {
            value
        }
    }

    private fun isAbsoluteUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
}
