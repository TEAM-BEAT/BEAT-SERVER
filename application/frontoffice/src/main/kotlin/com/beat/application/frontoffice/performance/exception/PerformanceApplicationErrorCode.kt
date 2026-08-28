package com.beat.application.frontoffice.performance.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class PerformanceApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    PRICE_UPDATE_NOT_ALLOWED(
        "PERFORMANCE_PRICE_UPDATE_NOT_ALLOWED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "예매자가 존재하여 가격을 수정할 수 없습니다.",
    ),
    PAST_SCHEDULE_NOT_ALLOWED(
        "PERFORMANCE_PAST_SCHEDULE_NOT_ALLOWED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "과거 날짜 회차를 포함한 공연을 생성할 수 없습니다.",
    ),
    SCHEDULE_MODIFICATION_NOT_ALLOWED_FOR_ENDED_SCHEDULE(
        "PERFORMANCE_ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "종료된 회차를 수정할 수 없습니다.",
    ),
    PERFORMANCE_DELETE_FAILED(
        "PERFORMANCE_DELETE_NOT_ALLOWED",
        FrontofficeApplicationErrorType.FORBIDDEN,
        "예매자가 1명 이상 있을 경우, 공연을 삭제할 수 없습니다.",
    ),
    NOT_PERFORMANCE_OWNER(
        "PERFORMANCE_FORBIDDEN",
        FrontofficeApplicationErrorType.FORBIDDEN,
        "해당 공연의 메이커가 아닙니다.",
    ),
    PERFORMANCE_NOT_FOUND(
        "PERFORMANCE_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "해당 공연 정보를 찾을 수 없습니다.",
    ),
    MEMBER_NOT_FOUND(
        "PERFORMANCE_MEMBER_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "회원이 없습니다",
    ),
    SCHEDULE_LIST_NOT_FOUND(
        "PERFORMANCE_SCHEDULE_LIST_REQUIRED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "스케쥴 리스트에 스케쥴이 없습니다.",
    ),
    DUPLICATE_MODIFICATION_ID(
        "PERFORMANCE_DUPLICATE_MODIFICATION_ID",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "동일한 수정 대상이 중복되었습니다.",
    ),
    INVALID_MODIFICATION_REQUEST(
        "PERFORMANCE_INVALID_MODIFICATION_REQUEST",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "공연 수정 요청이 올바르지 않습니다.",
    ),
    INVALID_IMAGE_KEY(
        "PERFORMANCE_INVALID_IMAGE_KEY",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "이미지 경로가 올바르지 않습니다.",
    ),
}
