package com.beat.global.auth.jwt.application;

import com.beat.global.auth.jwt.dao.TokenRepository;
import com.beat.global.auth.jwt.exception.TokenErrorCode;
import com.beat.global.auth.redis.Token;
import com.beat.global.common.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    @Transactional
    public void saveRefreshToken(final Long memberId, final String refreshToken) {
        tokenRepository.save(Token.of(memberId, refreshToken));
    }

    public Long findIdByRefreshToken(final String refreshToken) {
        Token token = tokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new NotFoundException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND));

        return token.getId();
    }

    @Transactional
    public void deleteRefreshToken(final Long memberId) {
        Token token = tokenRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND));

        tokenRepository.delete(token);
        log.info("Deleted refresh token: {}", token);
    }
}