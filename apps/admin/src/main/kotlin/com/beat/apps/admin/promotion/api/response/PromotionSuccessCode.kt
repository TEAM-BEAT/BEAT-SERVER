package com.beat.apps.admin.promotion.api.response

import com.beat.apps.admin.response.SuccessCode

enum class PromotionSuccessCode(
    override val status: Int,
    override val message: String,
) : SuccessCode {
    CAROUSEL_PRESIGNED_URL_ISSUED(200, "캐러셀 Presigned URL 발급 성공"),
    BANNER_PRESIGNED_URL_ISSUED(200, "배너 Presigned URL 발급 성공"),
    FETCH_ALL_CAROUSEL_PROMOTIONS_SUCCESS(200, "관리자 권한으로 현재 캐러셀에 등록된 모든 공연 조회에 성공하였습니다."),
    UPDATE_ALL_CAROUSEL_PROMOTIONS_SUCCESS(200, "캐러셀 이미지 수정 성공"),
    ;

}
