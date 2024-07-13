package com.beat.domain.member.dto;

public record LoginSuccessResponse(
        String accessToken,
        String refreshToken,
        String nickname
) {
    public static LoginSuccessResponse of(
            final String accessToken,
            final String refreshToken,
            final String nickname
    ) {
        return new LoginSuccessResponse(accessToken, refreshToken, nickname);
    }
}
