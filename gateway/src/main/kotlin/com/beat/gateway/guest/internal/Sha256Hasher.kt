package com.beat.gateway.guest.internal

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Base64

/**
 * guest capability가 공유하는 단방향 해시 유틸.
 *
 * 세션 토큰과 throttle key material 모두 원본을 Redis에 남기지 않기 위해
 * SHA-256 + Base64URL(padding 없음)로 변환한다.
 */
object Sha256Hasher {

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
