package com.beat.global.auth.jwt.dao;


import java.util.Optional;

import com.beat.global.auth.redis.Token;
import org.springframework.data.repository.CrudRepository;

public interface TokenRepository extends CrudRepository<Token, Long> {

    Optional<Token> findByRefreshToken(final String refreshToken);

    Optional<Token> findById(final Long id);
}
