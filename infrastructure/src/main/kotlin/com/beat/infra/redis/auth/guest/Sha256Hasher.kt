package com.beat.infra.redis.auth.guest

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Base64

internal object Sha256Hasher {

    private const val ALGORITHM = "SHA-256"

    fun hashToBase64Url(value: String): String {
        val digest = try {
            MessageDigest.getInstance(ALGORITHM)
        } catch (exception: NoSuchAlgorithmException) {
            throw IllegalStateException("$ALGORITHM is unavailable", exception)
        }
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(digest.digest(value.toByteArray(StandardCharsets.UTF_8)))
    }
}
