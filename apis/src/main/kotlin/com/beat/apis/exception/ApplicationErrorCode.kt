package com.beat.apis.exception

interface ApplicationErrorCode {
    fun getCode(): String

    fun getType(): ApplicationErrorType

    fun getMessage(): String
}
