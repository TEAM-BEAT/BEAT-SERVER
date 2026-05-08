package com.beat.global.support.response

import com.beat.global.support.exception.base.BaseSuccessCode

data class SuccessResponse<T>(
    val status: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        @JvmStatic
        fun <T> of(
            baseSuccessCode: BaseSuccessCode,
            data: T,
        ): SuccessResponse<T> =
            SuccessResponse(baseSuccessCode.getStatus(), baseSuccessCode.getMessage(), data)

        @JvmStatic
        fun <T> from(baseSuccessCode: BaseSuccessCode): SuccessResponse<T> =
            SuccessResponse(baseSuccessCode.getStatus(), baseSuccessCode.getMessage(), null)
    }
}
