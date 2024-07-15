package com.beat.domain.performance.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PerformanceSuccessCode implements BaseSuccessCode {
    PERFORMANCE_RETRIEVE_SUCCESS(200, "공연 상세 정보 조회가 성공적으로 완료되었습니다."),
    BOOKING_PERFORMANCE_RETRIEVE_SUCCESS(200, "예매 관련 공연 정보 조회가 성공적으로 완료되었습니다."),
    HOME_PERFORMANCE_RETRIEVE_SUCCESS(200, "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다."),
    MAKER_PERFORMANCE_RETRIEVE_SUCCESS(200, "회원이 등록한 공연 목록의 조회가 성공적으로 완료되었습니다.")
    ;

    private final int status;
    private final String message;
}
