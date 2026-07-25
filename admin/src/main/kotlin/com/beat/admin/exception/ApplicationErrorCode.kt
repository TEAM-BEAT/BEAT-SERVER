package com.beat.admin.exception

interface ApplicationErrorCode {
    fun getCode(): String

    fun getType(): ApplicationErrorType

    fun getMessage(): String
}
