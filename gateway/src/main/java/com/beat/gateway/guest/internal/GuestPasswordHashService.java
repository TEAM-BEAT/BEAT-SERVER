package com.beat.gateway.guest.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.beat.contracts.auth.guest.GuestPasswordHashPort;

public final class GuestPasswordHashService implements GuestPasswordHashPort {

	private static final String BCRYPT_PREFIX = "$2";

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Override
	public String encode(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String storedPassword) {
		if (storedPassword == null || storedPassword.isBlank()) {
			return false;
		}
		if (isBcrypt(storedPassword)) {
			return passwordEncoder.matches(rawPassword, storedPassword);
		}
		return MessageDigest.isEqual(
			rawPassword.getBytes(StandardCharsets.UTF_8),
			storedPassword.getBytes(StandardCharsets.UTF_8)
		);
	}

	@Override
	public boolean needsUpgrade(String storedPassword) {
		return !isBcrypt(storedPassword) || passwordEncoder.upgradeEncoding(storedPassword);
	}

	private boolean isBcrypt(String storedPassword) {
		return storedPassword.startsWith(BCRYPT_PREFIX);
	}
}
