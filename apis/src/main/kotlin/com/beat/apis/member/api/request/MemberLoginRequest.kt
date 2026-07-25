package com.beat.apis.member.api.request

import com.beat.apis.member.api.type.SocialTypeRequest
import jakarta.validation.constraints.NotNull

data class MemberLoginRequest(
    @field:NotNull(message = "소셜 로그인 종류가 입력되지 않았습니다.")
    val socialType: SocialTypeRequest?,
)
