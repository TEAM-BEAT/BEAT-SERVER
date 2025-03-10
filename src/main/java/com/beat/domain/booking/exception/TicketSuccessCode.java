package com.beat.domain.booking.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketSuccessCode implements BaseSuccessCode {
	/*
	200 Ok
	*/
	TICKET_RETRIEVE_SUCCESS(200, "예매자 목록 조회가 성공적으로 완료되었습니다."),
	TICKET_UPDATE_SUCCESS(200, "예매자 입금여부 수정이 성공적으로 완료되었습니다."),
	TICKET_REFUND_SUCCESS(200, "예매 환불처리 요청이 성공했습니다."),
	TICKET_DELETE_SUCCESS(200, "예매자 삭제 요청이 성공했습니다."),
	TICKET_SEARCH_SUCCESS(200, "예매자 검색 결과 조회가 성공적으로 완료되었습니다.");

	private final int status;
	private final String message;
}
