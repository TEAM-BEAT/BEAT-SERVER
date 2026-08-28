package com.beat.apps.api.file.api.response

import com.beat.apps.api.response.SuccessCode

enum class FileSuccessCode(
    override val status: Int,
    override val message: String,
) : SuccessCode {
    PERFORMANCE_MAKER_PRESIGNED_URL_ISSUED(200, "공연 메이커를 위한 Presigned URL 발급 성공")
}
