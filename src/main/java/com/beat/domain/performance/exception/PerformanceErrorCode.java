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
	PRICE_UPDATE_NOT_ALLOWED(400, "예매자가 존재하여 가격을 수정할 수 없습니다."),
	NEGATIVE_TICKET_PRICE(400, "티켓 가격은 음수일 수 없습니다."),
	NO_PERFORMANCE_FOUND(404, "공연을 찾을 수 없습니다."),
	PERFORMANCE_DELETE_FAILED(403, "예매자가 1명 이상 있을 경우, 공연을 삭제할 수 없습니다."),
	NOT_PERFORMANCE_OWNER(403, "해당 공연의 메이커가 아닙니다."),
	MAX_SCHEDULE_LIMIT_EXCEEDED(400, "공연 회차는 최대 10개까지 추가할 수 있습니다."),
	INVALID_PERFORMANCE_DESCRIPTION_LENGTH(400, "공연 소개 글자수가 500자를 초과했습니다."),
	INVALID_ATTENTION_NOTE_LENGTH(400, "공연 유의사항 글자수가 500자를 초과했습니다."),
	INTERNAL_SERVER_ERROR(500, "서버 내부 오류입니다."),
	PAST_SCHEDULE_NOT_ALLOWED(400, "과거 날짜 회차를 포함한 공연을 생성할 수 없습니다."),
	SCHEDULE_MODIFICATION_NOT_ALLOWED_FOR_ENDED_SCHEDULE(400, "종료된 회차를 수정할 수 없습니다."),
	INVALID_TICKET_COUNT(400, "판매된 티켓 수보다 적은 수로 판매할 티켓 매수를 수정할 수 없습니다.");

	private final int status;
	private final String message;
}