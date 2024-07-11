package com.beat.domain.schedule.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseErrorCode {
    INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
    NO_SCHEDULE_FOUND(404, "해당 회차를 찾을 수 없습니다."),
    INSUFFICIENT_TICKETS(409, "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요.")
    ;

    private final int status;
    private final String message;
}
