package com.beat.domain.member.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {
    MEMBER_NOT_FOUND(404, "회원이 없습니다"),
    SOCIAL_TYPE_BAD_REQUEST(400, "로그인 요청이 유효하지 않습니다.");

    private final int status;
    private final String message;
}
