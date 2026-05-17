package com.beat.contracts.auth;

public interface RefreshTokenPort {

	void saveRefreshToken(Long memberId, String refreshToken);

	Long findMemberIdByRefreshToken(String refreshToken);

	void deleteRefreshToken(Long memberId);
}
