package com.beat.global.support.response

data class SuccessResponse<T>(
    val status: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        @JvmStatic
        fun <T> of(
            successCode: SuccessCode,
            data: T,
        ): SuccessResponse<T> =
            SuccessResponse(successCode.getStatus(), successCode.getMessage(), data)

        @JvmStatic
        fun <T> from(successCode: SuccessCode): SuccessResponse<T> =
            SuccessResponse(successCode.getStatus(), successCode.getMessage(), null)
    }
}
