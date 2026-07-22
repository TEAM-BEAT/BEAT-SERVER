package com.beat.apis.member.api.response

import com.beat.apis.member.application.result.LoginSuccessResult

@ConsistentCopyVisibility
data class MemberLoginResponse private constructor(
    val accessToken: String?,
    val nickname: String?,
    val role: String?,
) {
    companion object {
        fun from(result: LoginSuccessResult): MemberLoginResponse = MemberLoginResponse(
            accessToken = result.accessToken,
            nickname = result.nickname,
            role = result.role,
        )
    }
}
