package com.beat.apps.api.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "API 오류 결과를 담는 공통 응답 envelope")
data class ErrorResponse(
    @field:Schema(
        description = "HTTP 오류 상태 코드",
        example = "400",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: Int,
    @field:Schema(
        description = "클라이언트에 전달하는 오류 메시지",
        example = "잘못된 요청입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val message: String,
) {
    companion object {
        fun of(
            status: Int,
            message: String,
        ): ErrorResponse = ErrorResponse(status, message)
    }
}
