package com.beat.domain.member.application;

import com.beat.domain.member.application.dto.AccessTokenGenerateResponse;
import com.beat.domain.member.application.dto.LoginSuccessResponse;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.jwt.application.TokenService;
import com.beat.global.auth.jwt.exception.TokenErrorCode;
import com.beat.global.auth.jwt.provider.JwtTokenProvider;
import com.beat.global.auth.jwt.provider.JwtValidationType;
import com.beat.global.auth.security.AdminAuthentication;
import com.beat.global.auth.security.MemberAuthentication;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.BeatException;
import com.beat.global.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final JwtTokenProvider jwtTokenProvider;
	private final TokenService tokenService;

	/**
	 * 사용자의 로그인 성공 시 Access Token과 Refresh Token을 생성하고,
	 * 로그인 성공 응답 객체(LoginSuccessResponse)를 반환하는 메서드.
	 *
	 * @param memberId 회원의 고유 ID
	 * @param user 로그인한 사용자의 정보가 포함된 Users 객체
	 * @param memberInfoResponse 로그인 시 외부로부터 전달된 회원 정보
	 * @return 로그인 성공 응답(LoginSuccessResponse)
	 */
	public LoginSuccessResponse generateLoginSuccessResponse(final Long memberId, final Users user,
		final MemberInfoResponse memberInfoResponse) {
		String nickname = memberInfoResponse.nickname();
		Role role = user.getRole();
		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());

		log.info("Starting login success response generation for memberId: {}, nickname: {}, role: {}", memberId,
			nickname, role.getRoleName());

		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(memberId, role,
			authorities);
		String refreshToken = issueAndSaveRefreshToken(memberId, authenticationToken);
		String accessToken = jwtTokenProvider.issueAccessToken(authenticationToken);

		log.info("Login success for authorities: {}, accessToken: {}, refreshToken: {}", authorities, accessToken,
			refreshToken);

		return LoginSuccessResponse.of(accessToken, refreshToken, nickname, role.getRoleName());
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

		Long memberId = jwtTokenProvider.getMemberIdFromJwt(refreshToken);
		verifyMemberIdWithStoredToken(refreshToken, memberId);

		Role role = jwtTokenProvider.getRoleFromJwt(refreshToken);
		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());

		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(memberId, role, authorities);
		log.info("Generated new access token for memberId: {}, role: {}, authorities: {}",
			memberId, role.getRoleName(), authorities);

		return AccessTokenGenerateResponse.from(jwtTokenProvider.issueAccessToken(authenticationToken));
	}

	/**
	 * Refresh Token을 발급하고 저장하는 메서드.
	 * 발급된 Refresh Token을 TokenService에 저장
	 *
	 * @param memberId 회원의 고유 ID
	 * @param authenticationToken 사용자 인증 정보
	 * @return 발급된 Refresh Token
	 */
	private String issueAndSaveRefreshToken(Long memberId, UsernamePasswordAuthenticationToken authenticationToken) {
		String refreshToken = jwtTokenProvider.issueRefreshToken(authenticationToken);
		log.info("Issued new refresh token for memberId: {}", memberId);
		tokenService.saveRefreshToken(memberId, refreshToken);
		return refreshToken;
	}

	/**
	 * 사용자 Role에 따라 적절한 Authentication 객체(Admin 또는 Member)를 생성하는 메서드.
	 *
	 * @param memberId 회원의 고유 ID
	 * @param role 사용자 Role (ADMIN 또는 MEMBER)
	 * @param authorities 사용자에게 부여된 권한 목록
	 * @return 생성된 Admin 또는 Member Authentication 객체
	 */
	private UsernamePasswordAuthenticationToken createAuthenticationToken(Long memberId, Role role,
		Collection<GrantedAuthority> authorities) {
		if (role == Role.ADMIN) {
			log.info("Creating AdminAuthentication for memberId: {}", memberId);
			return new AdminAuthentication(memberId, null, authorities);
		} else {
			log.info("Creating MemberAuthentication for memberId: {}", memberId);
			return new MemberAuthentication(memberId, null, authorities);
		}
	}

	private void validateRefreshToken(String refreshToken) {
		JwtValidationType validationType = jwtTokenProvider.validateToken(refreshToken);

		if (!validationType.equals(JwtValidationType.VALID_JWT)) {
			throw switch (validationType) {
				case EXPIRED_JWT_TOKEN -> new UnauthorizedException(TokenErrorCode.REFRESH_TOKEN_EXPIRED_ERROR);
				case INVALID_JWT_TOKEN -> new BadRequestException(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR);
				case INVALID_JWT_SIGNATURE -> new BadRequestException(TokenErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR);
				case UNSUPPORTED_JWT_TOKEN -> new BadRequestException(TokenErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR);
				case EMPTY_JWT -> new BadRequestException(TokenErrorCode.REFRESH_TOKEN_EMPTY_ERROR);
				default -> new BeatException(TokenErrorCode.UNKNOWN_REFRESH_TOKEN_ERROR);
			};
		}
	}

	private void verifyMemberIdWithStoredToken(String refreshToken, Long memberId) {
		Long storedMemberId = tokenService.findIdByRefreshToken(refreshToken);

		if (!memberId.equals(storedMemberId)) {
			log.error("MemberId mismatch: token does not match the stored refresh token");
			throw new BadRequestException(TokenErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR);
		}
	}

}
