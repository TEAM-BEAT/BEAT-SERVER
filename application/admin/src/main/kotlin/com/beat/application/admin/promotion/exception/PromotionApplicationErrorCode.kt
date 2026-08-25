package com.beat.application.admin.promotion.exception

import com.beat.application.admin.exception.AdminApplicationErrorCode
import com.beat.application.admin.exception.AdminApplicationErrorType

enum class PromotionApplicationErrorCode(
    override val code: String,
    override val type: AdminApplicationErrorType,
    override val message: String,
) : AdminApplicationErrorCode {
    INVALID_REQUEST_FORMAT(
        "ADMIN_INVALID_REQUEST_FORMAT",
        AdminApplicationErrorType.INVALID_INPUT,
        "잘못된 요청 형식입니다.",
    ),
    INVALID_IMAGE_UPLOAD(
        "ADMIN_INVALID_IMAGE_UPLOAD",
        AdminApplicationErrorType.INVALID_INPUT,
        "업로드된 이미지가 없거나 유효하지 않습니다.",
    ),
    MEMBER_NOT_FOUND(
        "ADMIN_PROMOTION_MEMBER_NOT_FOUND",
        AdminApplicationErrorType.NOT_FOUND,
        "회원이 없습니다",
    ),
    PERFORMANCE_NOT_FOUND(
        "ADMIN_PERFORMANCE_NOT_FOUND",
        AdminApplicationErrorType.NOT_FOUND,
        "해당 공연 정보를 찾을 수 없습니다.",
    ),
    PROMOTION_NOT_FOUND(
        "ADMIN_PROMOTION_NOT_FOUND",
        AdminApplicationErrorType.NOT_FOUND,
        "해당 홍보 정보를 찾을 수 없습니다.",
    ),
}
