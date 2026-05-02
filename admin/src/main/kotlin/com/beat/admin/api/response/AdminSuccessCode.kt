package com.beat.admin.api.response

import com.beat.global.common.exception.base.BaseSuccessCode

@Suppress("ktlint:standard:enum-entry-name-case")
enum class AdminSuccessCode(
    private val status: Int,
    private val message: String,
) : BaseSuccessCode {
    FETCH_ALL_USERS_SUCCESS(200, "관리자 권한으로 모든 유저 조회에 성공하였습니다."),
    CAROUSEL_PRESIGNED_URL_ISSUED(200, "캐러셀 Presigned URL 발급 성공"),
    BANNER_PRESIGNED_URL_ISSUED(200, "배너 Presigned URL 발급 성공"),
    FETCH_ALL_CAROUSEL_PROMOTIONS_SUCCESS(200, "관리자 권한으로 현재 캐러셀에 등록된 모든 공연 조회에 성공하였습니다."),
    UPDATE_ALL_CAROUSEL_PROMOTIONS_SUCCESS(200, "캐러셀 이미지 수정 성공"),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
