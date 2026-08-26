package com.beat.apps.api.member.api.type

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "지원하는 소셜 로그인 제공자", example = "KAKAO")
enum class SocialTypeRequest {
    KAKAO
}
