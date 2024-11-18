package com.beat.domain.cast.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CastErrorCode implements BaseErrorCode {
	/*
	403 Forbidden
	*/
	CAST_NOT_BELONG_TO_PERFORMANCE(403, "해당 등장인물은 해당 공연에 속해 있지 않습니다."),

	/*
	404 NotFound
	*/
	CAST_NOT_FOUND(404, "등장인물이 존재하지 않습니다.");

	private final int status;
	private final String message;
}
