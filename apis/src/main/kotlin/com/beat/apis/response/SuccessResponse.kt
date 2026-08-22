package com.beat.apis.response

data class SuccessResponse<T>(
    val status: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        fun <T> of(
            successCode: SuccessCode,
            data: T,
        ): SuccessResponse<T> =
            SuccessResponse(successCode.status, successCode.message, data)

        fun <T> from(successCode: SuccessCode): SuccessResponse<T> =
            SuccessResponse(successCode.status, successCode.message, null)
    }
}
