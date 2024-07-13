package com.beat.global.auth.jwt.provider;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum JwtValidationType {
    VALID_JWT("VALID_JWT"),              // 유효한 JWT
    INVALID_JWT_SIGNATURE("INVALID_JWT_SIGNATURE"),      // 유효하지 않은 서명
    INVALID_JWT_TOKEN("INVALID_JWT_TOKEN"),          // 유효하지 않은 토큰
    EXPIRED_JWT_TOKEN("EXPIRED_JWT_TOKEN"),          // 만료된 토큰
    UNSUPPORTED_JWT_TOKEN("UNSUPPORTED_JWT_TOKEN"),      // 지원하지 않는 형식의 토큰
    EMPTY_JWT("EMPTY_JWT");                   // 빈 JWT

    private String validationType;
}
