package com.beat.gateway.jwt.internal;

import com.beat.contracts.auth.RefreshTokenPort;
import com.beat.contracts.auth.TokenErrorCode;
import com.beat.gateway.jwt.internal.store.RefreshToken;
import com.beat.gateway.jwt.internal.store.RefreshTokenRepository;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefreshTokenService implements RefreshTokenPort {

	private final RefreshTokenRepository refreshTokenRepository;

	@Override
	public void saveRefreshToken(final Long memberId, final String refreshToken) {
		refreshTokenRepository.save(RefreshToken.of(memberId, refreshToken));
	}

	@Override
	public Long findMemberIdByRefreshToken(final String refreshToken) {
		RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken)
			.orElseThrow(() -> new NotFoundException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND));

		return token.getId();
	}

	@Override
	public void deleteRefreshToken(final Long memberId) {
		RefreshToken token = refreshTokenRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND));

		refreshTokenRepository.delete(token);
		log.info("Deleted refresh token: {}", token);
	}
}
