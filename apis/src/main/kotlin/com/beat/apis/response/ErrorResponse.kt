package com.beat.apis.response

data class ErrorResponse(
    val status: Int,
    val message: String,
) {
    companion object {
        fun of(
            status: Int,
            message: String,
        ): ErrorResponse = ErrorResponse(status, message)
    }
}
