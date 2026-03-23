package com.beat.domain.schedule.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseErrorCode {
	/*
	400 BadRequest
	*/
	INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),

	/*
	403 Forbidden
	*/
	SCHEDULE_NOT_BELONG_TO_PERFORMANCE(403, "해당 스케줄은 해당 공연에 속해 있지 않습니다."),

	/*
	404 NotFound
	*/
	NO_SCHEDULE_FOUND(404, "해당 회차를 찾을 수 없습니다."),

	/*
	409 Conflict
	*/
	INSUFFICIENT_TICKETS(409, "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요."),
	EXCESS_TICKET_DELETE(409, "예매된 티켓 수 이상을 삭제할 수 없습니다.");

	private final int status;
	private final String message;
}
