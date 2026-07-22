package com.beat.apis.file.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class FileApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    INVALID_FILE_NAME("FILE_INVALID_FILE_NAME", ApplicationErrorType.INVALID_INPUT, "파일 이름이 올바르지 않습니다."),
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
