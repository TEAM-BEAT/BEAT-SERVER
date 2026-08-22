package com.beat.infra.external.social.kakao.response

import com.fasterxml.jackson.annotation.JsonProperty

internal data class KakaoUserResponse(
    @param:JsonProperty("id")
    val id: Long?,
    @param:JsonProperty("connected_at")
    val connectedAt: String?,
    @param:JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount?,
)
