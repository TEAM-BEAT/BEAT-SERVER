package com.beat.domain.member.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {
	/*
	200 Ok
	*/
	SIGN_UP_SUCCESS(200, "로그인 성공"),
	ISSUE_ACCESS_TOKEN_SUCCESS(200, "엑세스토큰 발급 성공"),
	ISSUE_ACCESS_TOKEN_USING_REFRESH_TOKEN(200, "리프레쉬 토큰으로 액세스 토큰 재발급 성공"),
	SIGN_OUT_SUCCESS(200, "로그아웃 성공"),
	USER_DELETE_SUCCESS(200, "회원 탈퇴 성공");

	private final int status;
	private final String message;
}
