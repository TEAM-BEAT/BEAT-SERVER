package com.beat.global.external.s3.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileSuccessCode implements BaseSuccessCode {
    PRESIGNED_URL_GENERATED(200, "Presigned URL 생성 성공");

    private final int status;
    private final String message;
}