package com.beat.apps.api.member.api.request

import com.beat.apps.api.member.api.type.SocialTypeRequest
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "소셜 로그인 및 회원가입 요청")
data class MemberLoginRequest(
    @field:Schema(
        description = "사용할 소셜 로그인 제공자입니다.",
        example = "KAKAO",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val socialType: SocialTypeRequest
)
