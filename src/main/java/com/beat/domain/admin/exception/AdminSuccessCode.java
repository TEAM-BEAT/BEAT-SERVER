package com.beat.domain.admin.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminSuccessCode implements BaseSuccessCode {
    FETCH_ALL_USERS_SUCCESS(200, "관리자 권한으로 모든 유저 조회에 성공하였습니다."),
    CAROUSEL_PRESIGNED_URL_ISSUED(200, "캐러셀 Presigned URL 발급 성공"),
    BANNER_PRESIGNED_URL_ISSUED(200, "배너 Presigned URL 발급 성공")
    ;

    private final int status;
    private final String message;
}