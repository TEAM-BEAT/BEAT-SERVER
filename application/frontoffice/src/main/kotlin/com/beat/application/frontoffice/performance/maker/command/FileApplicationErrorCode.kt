package com.beat.application.frontoffice.performance.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class FileApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    INVALID_FILE_NAME(
        "FILE_INVALID_FILE_NAME",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "파일 이름이 올바르지 않습니다.",
    )
}
