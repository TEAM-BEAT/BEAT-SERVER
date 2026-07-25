package com.beat.global.support.response

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
    }
}
