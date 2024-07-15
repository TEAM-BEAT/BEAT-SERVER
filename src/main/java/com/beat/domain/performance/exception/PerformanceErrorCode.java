package com.beat.domain.performance.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PerformanceErrorCode implements BaseErrorCode {
    PERFORMANCE_NOT_FOUND(404, "해당 공연 정보를 찾을 수 없습니다."),
    REQUIRED_DATA_MISSING(400, "필수 데이터가 누락되었습니다."),
    INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
    INVALID_REQUEST_FORMAT(400, "잘못된 요청 형식입니다."),
    NO_PERFORMANCE_FOUND(404, "공연을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류입니다.")
    ;

    private final int status;
    private final String message;
}