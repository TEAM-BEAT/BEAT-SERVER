package com.beat.infrastructure.external.social.kakao.response

import com.fasterxml.jackson.annotation.JsonProperty

internal data class KakaoAccessTokenResponse(
    @param:JsonProperty("token_type")
    val tokenType: String?,
    @param:JsonProperty("access_token")
    val accessToken: String?,
    @param:JsonProperty("expires_in")
    val expiresIn: Int?,
    @param:JsonProperty("refresh_token")
    val refreshToken: String?,
    @param:JsonProperty("refresh_token_expires_in")
    val refreshTokenExpiresIn: Int?,
)
