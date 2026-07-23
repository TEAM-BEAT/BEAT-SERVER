package com.beat.gateway.guest.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.OptionalLong;

import com.beat.contracts.auth.guest.GuestSessionPort;
import com.beat.gateway.guest.internal.store.GuestSession;
import com.beat.gateway.guest.internal.store.GuestSessionRepository;

public final class GuestSessionService implements GuestSessionPort {

	private static final int TOKEN_BYTE_LENGTH = 32;

	private final GuestSessionRepository guestSessionRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public GuestSessionService(GuestSessionRepository guestSessionRepository) {
		this.guestSessionRepository = guestSessionRepository;
	}

	@Override
	public String issue(long userId) {
		byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
		secureRandom.nextBytes(tokenBytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		guestSessionRepository.save(GuestSession.of(hash(token), userId));
		return token;
	}

	@Override
	public OptionalLong findUserId(String token) {
		if (token == null || token.isBlank()) {
			return OptionalLong.empty();
		}
		return guestSessionRepository.findById(hash(token))
			.map(GuestSession::getUserId)
			.map(OptionalLong::of)
			.orElseGet(OptionalLong::empty);
	}

	private String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
