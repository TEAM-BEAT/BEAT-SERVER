package com.beat.domain.schedule.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleSuccessCode implements BaseSuccessCode {
	/*
	200 Ok
	*/
	TICKET_AVAILABILITY_RETRIEVAL_SUCCESS(200, "티켓 수량 조회가 성공적으로 완료되었습니다.");

	private final int status;
	private final String message;
}
