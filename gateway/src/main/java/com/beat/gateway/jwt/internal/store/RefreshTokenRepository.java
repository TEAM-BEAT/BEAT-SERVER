package com.beat.gateway.jwt.internal.store;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
