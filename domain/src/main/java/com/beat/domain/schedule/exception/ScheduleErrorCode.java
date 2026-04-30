package com.beat.domain.schedule.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum ScheduleErrorCode implements BaseErrorCode {
	/*
	400 BadRequest
	*/
	INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),

	/*
	409 Conflict
	*/
	INSUFFICIENT_TICKETS(409, "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요."),
	EXCESS_TICKET_DELETE(409, "예매된 티켓 수 이상을 삭제할 수 없습니다.");

	private final int status;
	private final String message;

	ScheduleErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
