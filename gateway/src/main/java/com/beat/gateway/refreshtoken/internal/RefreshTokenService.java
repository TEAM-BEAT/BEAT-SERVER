package com.beat.gateway.refreshtoken.internal;

import java.util.OptionalLong;

import com.beat.contracts.auth.RefreshTokenPort;
import com.beat.gateway.refreshtoken.internal.store.RefreshToken;
import com.beat.gateway.refreshtoken.internal.store.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefreshTokenService implements RefreshTokenPort {

	private final RefreshTokenRepository refreshTokenRepository;

	@Override
	public void saveRefreshToken(final long memberId, final String refreshToken) {
		refreshTokenRepository.save(RefreshToken.of(memberId, refreshToken));
	}

	@Override
	public OptionalLong findMemberIdByRefreshToken(final String refreshToken) {
		return refreshTokenRepository.findByRefreshToken(refreshToken)
			.map(token -> OptionalLong.of(token.getId()))
			.orElseGet(OptionalLong::empty);
	}

	@Override
	public boolean deleteRefreshToken(final long memberId) {
		return refreshTokenRepository.findById(memberId).map(token -> {
			refreshTokenRepository.delete(token);
			log.info("Deleted refresh token for memberId={}", memberId);
			return true;
		}).orElse(false);
	}
}
