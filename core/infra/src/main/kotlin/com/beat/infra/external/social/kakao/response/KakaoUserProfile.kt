package com.beat.infra.external.social.kakao.response

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoUserProfile(
    @param:JsonProperty("nickname")
    val nickname: String?,
    @param:JsonProperty("thumbnail_image_url")
    val thumbnailImageUrl: String?,
    @param:JsonProperty("profile_image_url")
    val profileImageUrl: String?,
    @param:JsonProperty("is_default_image")
    val defaultImage: Boolean?,
)
