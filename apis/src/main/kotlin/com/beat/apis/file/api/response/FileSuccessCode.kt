package com.beat.apis.file.api.response

import com.beat.global.support.response.SuccessCode

enum class FileSuccessCode(
    private val status: Int,
    private val message: String,
) : SuccessCode {
    PERFORMANCE_MAKER_PRESIGNED_URL_ISSUED(200, "공연 메이커를 위한 Presigned URL 발급 성공"),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
