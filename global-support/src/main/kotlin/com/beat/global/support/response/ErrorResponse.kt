package com.beat.global.support.response

import com.beat.global.support.exception.base.BaseErrorCode

data class ErrorResponse(
    val status: Int,
    val message: String,
) {
    companion object {
        @JvmStatic
        fun of(
            status: Int,
            message: String,
        ): ErrorResponse = ErrorResponse(status, message)

        @JvmStatic
        fun from(baseErrorCode: BaseErrorCode): ErrorResponse =
            ErrorResponse(baseErrorCode.getStatus(), baseErrorCode.getMessage())
    }
}
