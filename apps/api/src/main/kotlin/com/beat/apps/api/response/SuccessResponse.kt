package com.beat.apps.api.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "API 성공 결과를 담는 공통 응답 envelope")
data class SuccessResponse<T>(
    @field:Schema(
        description = "HTTP 성공 상태 코드",
        example = "200",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: Int,
    @field:Schema(
        description = "클라이언트에 전달하는 성공 메시지",
        example = "요청이 성공적으로 처리되었습니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val message: String,
    @field:Schema(
        description = "성공 결과 데이터입니다. 데이터가 없는 응답에서도 JSON 키가 포함되며 값은 null일 수 있습니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val data: T,
) {
    companion object {
        fun <T> of(
            successCode: SuccessCode,
            data: T,
        ): SuccessResponse<T> = SuccessResponse(successCode.status, successCode.message, data)

        fun <T> from(successCode: SuccessCode): SuccessResponse<T?> =
            SuccessResponse(successCode.status, successCode.message, null)
    }
}
