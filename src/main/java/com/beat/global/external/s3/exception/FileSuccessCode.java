package com.beat.global.external.s3.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileSuccessCode implements BaseSuccessCode {
    PERFORMANCE_MAKER_PRESIGNED_URL_ISSUED(200, "공연 메이커를 위한 Presigned URL 발급 성공");
    private final int status;
    private final String message;
}