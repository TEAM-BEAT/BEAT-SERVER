package com.beat.domain.performance.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PerformanceSuccessCode implements BaseSuccessCode {
	/*
	200 Ok
	*/
	PERFORMANCE_UPDATE_SUCCESS(200, "공연이 성공적으로 수정되었습니다."),
	PERFORMANCE_RETRIEVE_SUCCESS(200, "공연 상세 정보 조회가 성공적으로 완료되었습니다."),
	PERFORMANCE_MODIFY_PAGE_SUCCESS(200, "공연 수정 페이지 조회가 성공적으로 완료되었습니다."),
	PERFORMANCE_DELETE_SUCCESS(200, "공연이 성공적으로 삭제되었습니다."),
	BOOKING_PERFORMANCE_RETRIEVE_SUCCESS(200, "예매 관련 공연 정보 조회가 성공적으로 완료되었습니다."),
	HOME_PERFORMANCE_RETRIEVE_SUCCESS(200, "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다."),
	MAKER_PERFORMANCE_RETRIEVE_SUCCESS(200, "회원이 등록한 공연 목록의 조회가 성공적으로 완료되었습니다."),

	/*
	201 Created
	*/
	PERFORMANCE_CREATE_SUCCESS(201, "공연이 성공적으로 생성되었습니다.");

	private final int status;
	private final String message;
}
