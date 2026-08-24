package com.beat.infrastructure.external.social.kakao.response

import com.fasterxml.jackson.annotation.JsonProperty

internal data class KakaoAccount(
    @param:JsonProperty("profile_nickname_needs_agreement")
    val profileNicknameNeedsAgreement: Boolean?,
    @param:JsonProperty("profile")
    val profile: KakaoUserProfile?,
    @param:JsonProperty("email_needs_agreement")
    val emailNeedsAgreement: Boolean?,
    @param:JsonProperty("is_email_valid")
    val emailValid: Boolean?,
    @param:JsonProperty("is_email_verified")
    val emailVerified: Boolean?,
    @param:JsonProperty("email")
    val email: String?,
)
