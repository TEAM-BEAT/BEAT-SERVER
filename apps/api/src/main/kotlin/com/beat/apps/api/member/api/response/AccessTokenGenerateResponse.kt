package com.beat.apps.api.member.api.response

import com.beat.application.frontoffice.auth.command.AccessTokenResult
import io.swagger.v3.oas.annotations.media.Schema

@ConsistentCopyVisibility
@Schema(description = "refresh token으로 발급한 access token 응답")
data class AccessTokenGenerateResponse
private constructor(
    @field:Schema(
        description = "새로 발급된 access token입니다.",
        example = "access-token-example",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accessToken: String?
) {
    companion object {
        fun from(result: AccessTokenResult): AccessTokenGenerateResponse =
            AccessTokenGenerateResponse(result.accessToken)
    }
}
