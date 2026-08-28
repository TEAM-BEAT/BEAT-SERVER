package com.beat.apps.api.member.api.response

import com.beat.application.frontoffice.member.command.LoginSuccessResult
import io.swagger.v3.oas.annotations.media.Schema

@ConsistentCopyVisibility
@Schema(description = "소셜 로그인 또는 회원가입 성공 응답")
data class MemberLoginResponse
private constructor(
    @field:Schema(
        description = "로그인에 사용할 access token입니다.",
        example = "access-token-example",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accessToken: String,
    @field:Schema(
        description = "로그인한 회원의 닉네임입니다.",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val nickname: String,
    @field:Schema(
        description = "로그인한 회원의 권한명입니다.",
        example = "ROLE_MEMBER",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val role: String,
) {
    companion object {
        fun from(result: LoginSuccessResult): MemberLoginResponse =
            MemberLoginResponse(
                accessToken = result.accessToken,
                nickname = result.nickname,
                role = result.role,
            )
    }
}
