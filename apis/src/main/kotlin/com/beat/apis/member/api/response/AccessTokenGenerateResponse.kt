package com.beat.apis.member.api.response

import com.beat.apis.member.application.result.AccessTokenResult

@ConsistentCopyVisibility
data class AccessTokenGenerateResponse private constructor(
    val accessToken: String?,
) {
    companion object {
        fun from(result: AccessTokenResult): AccessTokenGenerateResponse =
            AccessTokenGenerateResponse(result.accessToken)
    }
}
