package com.beat.gateway.refreshtoken.internal

import com.beat.gateway.refreshtoken.internal.store.RefreshToken
import com.beat.gateway.refreshtoken.internal.store.RefreshTokenRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.`when` as given

class RefreshTokenServiceTest {

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var refreshTokenService: RefreshTokenService

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        refreshTokenService = RefreshTokenService(refreshTokenRepository)
    }

    @Test
    fun `refresh token을 memberId와 함께 저장한다`() {
        refreshTokenService.saveRefreshToken(1L, "refresh-token")

        verify(refreshTokenRepository).save(
            argThat { token: RefreshToken -> token.id == 1L && token.refreshToken == "refresh-token" },
        )
    }

    @Test
    fun `저장된 refresh token으로 memberId를 조회한다`() {
        given(refreshTokenRepository.findByRefreshToken("refresh-token"))
            .thenReturn(Optional.of(RefreshToken(1L, "refresh-token")))

        assertEquals(1L, refreshTokenService.findMemberIdByRefreshToken("refresh-token").orElseThrow())
    }

    @Test
    fun `존재하지 않는 refresh token 조회는 비어 있는 결과를 반환한다`() {
        given(refreshTokenRepository.findByRefreshToken("missing")).thenReturn(Optional.empty())

        assertFalse(refreshTokenService.findMemberIdByRefreshToken("missing").isPresent)
    }

    @Test
    fun `조회된 refresh token을 삭제한다`() {
        val token = RefreshToken(1L, "refresh-token")
        given(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(token))

        assertTrue(refreshTokenService.deleteRefreshToken(1L))
        verify(refreshTokenRepository).delete(token)
    }

    @Test
    fun `삭제 대상이 없으면 멱등하게 false를 반환한다`() {
        given(refreshTokenRepository.findById(1L)).thenReturn(Optional.empty())

        assertFalse(refreshTokenService.deleteRefreshToken(1L))
        verify(refreshTokenRepository, never()).delete(any())
    }
}
