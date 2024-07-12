package com.beat.global.auth.jwt.application;

import com.beat.global.auth.jwt.dao.TokenRepository;
import com.beat.global.auth.jwt.exception.TokenErrorCode;
import com.beat.global.auth.redis.Token;
import com.beat.global.common.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    @Transactional
    public void saveRefreshToken(final Long userId, final String refreshToken) {
        tokenRepository.save(
                Token.of(userId, refreshToken)
        );
    }

    public Long findIdByRefreshToken(final String refreshToken) {
        Token token = tokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(
                        () -> new NotFoundException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND)
                );
        return token.getId();
    }

    @Transactional
    public void deleteRefreshToken(final Long userId) {
        Token token = tokenRepository.findById(userId)
                .orElseThrow(
                        () -> new NotFoundException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND)
                );
        tokenRepository.delete(token);
    }
}