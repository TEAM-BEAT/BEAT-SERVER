package com.beat.domain.performance.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PerformanceErrorCode implements BaseErrorCode {
    PERFORMANCE_NOT_FOUND(404, "해당 공연 정보를 찾을 수 없습니다.");

    private final int status;
    private final String message;
}