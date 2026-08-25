package com.beat.apps.api.member.api.response

import com.beat.application.frontoffice.auth.command.AccessTokenResult

@ConsistentCopyVisibility
data class AccessTokenGenerateResponse private constructor(val accessToken: String?) {
    companion object {
        fun from(result: AccessTokenResult): AccessTokenGenerateResponse =
            AccessTokenGenerateResponse(result.accessToken)
    }
}
