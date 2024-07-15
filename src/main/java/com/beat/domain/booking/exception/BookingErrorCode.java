package com.beat.domain.booking.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingErrorCode implements BaseErrorCode {
    REQUIRED_DATA_MISSING(400, "필수 데이터가 누락되었습니다."),
    INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
    INVALID_REQUEST_FORMAT(400, "잘못된 요청 형식입니다."),
    NO_BOOKING_FOUND(404, "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요."),
    NO_TICKETS_FOUND(404, "입력하신 정보와 일치하는 예매자 목록이 없습니다."),
    NO_PERFORMANCE_FOUND(404, "공연을 찾을 수 없습니다."),
    NO_SCHEDULE_FOUND(404, "회차를 찾을 수 없습니다.")
    ;

    private final int status;
    private final String message;
}