package com.beat.observability.sentry

internal object SentrySensitiveDataPolicy {

    const val REDACTED = "[Filtered]"

    fun isSensitiveKey(key: String): Boolean = sensitiveKeyPattern.containsMatchIn(key)

    fun scrubString(value: String?): String? {
        if (value == null) {
            return null
        }
        return secretValuePatterns.fold(value) { scrubbed, pattern ->
            pattern.replace(scrubbed, "$1$REDACTED")
        }
    }

    fun isForbiddenMetricTag(key: String): Boolean {
        val normalizedKey = normalizeKey(key)
        return normalizedKey in forbiddenExactMetricTagKeys ||
            normalizedKey.endsWith("uri") ||
            normalizedKey.endsWith("url") ||
            forbiddenMetricTagFragments.any(normalizedKey::contains)
    }

    private fun normalizeKey(key: String): String =
        key.lowercase().filter(Char::isLetterOrDigit)

    private val sensitiveKeyPattern = Regex(
        pattern = listOf(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "access[-_]?token",
            "refresh[-_]?token",
            "password",
            "secret",
            "token",
            "jwt",
            "db[-_]?url",
            "aws[-_]?.*key",
            "s3[-_]?.*secret",
            "sentry[-_]?auth[-_]?token",
        ).joinToString("|"),
        option = RegexOption.IGNORE_CASE,
    )

    private val secretValuePatterns = listOf(
        Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+"),
        Regex("(?i)((?:access|refresh)?token[=:]\\s*)[^\\s,&}]+"),
        Regex("(?i)(password[=:]\\s*)[^\\s,&}]+"),
        Regex("(?i)(secret[=:]\\s*)[^\\s,&}]+"),
        Regex("(?i)(jwt[=:]\\s*)[^\\s,&}]+"),
    )

    private val forbiddenExactMetricTagKeys = setOf(
        "userid",
        "clientip",
        "request",
        "rawuri",
        "uri",
        "url",
    )

    private val forbiddenMetricTagFragments = setOf(
        "authorization",
        "cookie",
        "token",
        "secret",
        "password",
        "jwt",
    )
}
