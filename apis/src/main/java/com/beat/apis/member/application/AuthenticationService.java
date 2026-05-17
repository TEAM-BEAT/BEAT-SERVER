package com.beat.apis.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.member.application.dto.response.AccessTokenGenerateResponse;
import com.beat.apis.member.application.dto.response.LoginSuccessResponse;
import com.beat.contracts.auth.JwtSubject;
import com.beat.contracts.auth.JwtTokenPort;
import com.beat.contracts.auth.JwtTokenType;
import com.beat.contracts.auth.RefreshTokenPort;
import com.beat.contracts.auth.TokenErrorCode;
import com.beat.contracts.auth.TokenValidationResult;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.domain.user.domain.Role;
import com.beat.global.support.exception.BadRequestException;
import com.beat.global.support.exception.BeatException;
import com.beat.global.support.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final JwtTokenPort jwtTokenPort;
	private final RefreshTokenPort refreshTokenPort;

	/**
	 * 사용자의 로그인 성공 시 Access Token과 Refresh Token을 생성하고,
	 * 로그인 성공 응답 객체(LoginSuccessResponse)를 반환하는 메서드.
	 *
	 * @param memberId 회원의 고유 ID
	 * @param roleName 사용자 역할명
	 * @param socialMemberInfo 로그인 시 외부로부터 전달된 회원 정보
	 * @return 로그인 성공 응답(LoginSuccessResponse)
	 */
	public LoginSuccessResponse generateLoginSuccessResponse(final Long memberId, final String roleName,
		final SocialMemberInfo socialMemberInfo) {
		String nickname = socialMemberInfo.nickname();
		Role role = mapRole(roleName);
		String normalizedRoleName = role.getRoleName();

		log.info("Starting login success response generation for memberId: {}, nickname: {}, role: {}", memberId,
			nickname, normalizedRoleName);

		JwtSubject jwtSubject = createJwtSubject(memberId, role);
		String refreshToken = issueAndSaveRefreshToken(memberId, jwtSubject);
		String accessToken = jwtTokenPort.issueAccessToken(jwtSubject);

		log.info("Login success for role: {}, hasAccessToken={}, hasRefreshToken={}",
			normalizedRoleName,
			accessToken != null && !accessToken.isBlank(),
			refreshToken != null && !refreshToken.isBlank()
		);

		return LoginSuccessResponse.of(accessToken, refreshToken, nickname, normalizedRoleName);
	}

	/**
	 * 쿠키에서 "refreshToken" 값을 가져와 유효성을 검증하고,
	 * 유효한 Refresh Token일 경우 새로운 Access Token을 생성합니다.
	 *
	 * Refresh Token에서 사용자 ID와 Role 정보를 추출한 후,
	 * Role에 따라 Admin 또는 Member 권한으로 새로운 Access Token을 발급합니다.
	 *
	 * @param refreshToken "사용자의 Refresh Token"
	 * @return 새로운 Access Token 정보가 포함된 AccessTokenGenerateResponse 객체
	 */
	@Transactional
	public AccessTokenGenerateResponse generateAccessTokenFromRefreshToken(final String refreshToken) {
		validateRefreshToken(refreshToken);

		Long memberId = jwtTokenPort.getMemberId(refreshToken, JwtTokenType.REFRESH);
		validateMemberId(memberId);
		verifyMemberIdWithStoredToken(refreshToken, memberId);

		Role role = mapRole(jwtTokenPort.getRoleName(refreshToken, JwtTokenType.REFRESH));

		log.info("Generated new access token for memberId: {}, role: {}",
			memberId, role.getRoleName());

		return AccessTokenGenerateResponse.from(jwtTokenPort.issueAccessToken(createJwtSubject(memberId, role)));
	}

	/**
	 * Refresh Token을 발급하고 저장하는 메서드.
	 * 발급된 Refresh Token을 TokenService에 저장
	 *
	 * @param memberId 회원의 고유 ID
	 * @return 발급된 Refresh Token
	 */
	private String issueAndSaveRefreshToken(Long memberId, JwtSubject jwtSubject) {
		String refreshToken = jwtTokenPort.issueRefreshToken(jwtSubject);
		log.info("Issued new refresh token for memberId: {}", memberId);
		refreshTokenPort.saveRefreshToken(memberId, refreshToken);
		return refreshToken;
	}

	private JwtSubject createJwtSubject(Long memberId, Role role) {
		return new JwtSubject(memberId, role.getRoleName());
	}

	private void validateRefreshToken(String refreshToken) {
		TokenValidationResult validationResult = jwtTokenPort.validateRefreshToken(refreshToken);

		if (validationResult != TokenValidationResult.VALID) {
			throw switch (validationResult) {
				case EXPIRED -> new UnauthorizedException(TokenErrorCode.REFRESH_TOKEN_EXPIRED_ERROR);
				case INVALID_TOKEN -> new BadRequestException(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR);
				case INVALID_SIGNATURE -> new BadRequestException(TokenErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR);
				case UNSUPPORTED -> new BadRequestException(TokenErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR);
				case EMPTY -> new BadRequestException(TokenErrorCode.REFRESH_TOKEN_EMPTY_ERROR);
				default -> new BeatException(TokenErrorCode.UNKNOWN_REFRESH_TOKEN_ERROR);
			};
		}
	}

	private void verifyMemberIdWithStoredToken(String refreshToken, Long memberId) {
		Long storedMemberId = refreshTokenPort.findMemberIdByRefreshToken(refreshToken);

		if (!memberId.equals(storedMemberId)) {
			log.error("MemberId mismatch: token does not match the stored refresh token");
			throw new BadRequestException(TokenErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR);
		}
	}

	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			log.error("Refresh token memberId claim is missing");
			throw new BadRequestException(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR);
		}
	}

	@Transactional
	public void signOut(Long memberId) {
		refreshTokenPort.deleteRefreshToken(memberId);
	}

	private Role mapRole(String roleName) {
		if (roleName == null || roleName.isBlank()) {
			log.error("Refresh token role claim is missing");
			throw new BadRequestException(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR);
		}

		try {
			String enumValue = roleName.replace("ROLE_", "");
			return Role.valueOf(enumValue.toUpperCase());
		} catch (IllegalArgumentException exception) {
			log.error("Refresh token role claim is invalid: {}", roleName, exception);
			throw new BadRequestException(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR);
		}
	}

}
