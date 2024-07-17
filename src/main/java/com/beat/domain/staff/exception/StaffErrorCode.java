package com.beat.domain.staff.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StaffErrorCode implements BaseErrorCode {
    STAFF_NOT_FOUND(404, "스태프가 존재하지 않습니다.")
    ;

    private final int status;
    private final String message;
}
