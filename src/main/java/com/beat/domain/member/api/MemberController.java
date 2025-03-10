package com.beat.domain.member.api;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.domain.member.application.AuthenticationService;
import com.beat.domain.member.application.SocialLoginService;
import com.beat.domain.member.application.dto.response.AccessTokenGenerateResponse;
import com.beat.domain.member.application.dto.response.LoginSuccessResponse;
import com.beat.domain.member.application.dto.response.MemberLoginResponse;
import com.beat.domain.member.exception.MemberSuccessCode;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.auth.jwt.application.TokenService;
import com.beat.global.common.dto.SuccessResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController implements MemberApi {
	private final TokenService tokenService;
	private final AuthenticationService authenticationService;
	private final SocialLoginService socialLoginService;

	private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
	private static final String REFRESH_TOKEN = "refreshToken";

	@Override
	@PostMapping("/sign-up")
	public ResponseEntity<SuccessResponse<MemberLoginResponse>> signUp(
		@RequestParam final String authorizationCode,
		@RequestBody final MemberLoginRequest loginRequest,
		HttpServletResponse httpServletResponse
	) {
		LoginSuccessResponse loginSuccessResponse = socialLoginService.handleSocialLogin(authorizationCode,
			loginRequest);

		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, loginSuccessResponse.refreshToken())
			.maxAge(COOKIE_MAX_AGE)
			.path("/")
			.secure(true)
			.sameSite("None")
			.httpOnly(true)
			.build();
		httpServletResponse.setHeader("Set-Cookie", cookie.toString());

		MemberLoginResponse response = MemberLoginResponse.of(loginSuccessResponse.accessToken(),
			loginSuccessResponse.nickname(),
			loginSuccessResponse.role());

		return ResponseEntity.ok()
			.body(SuccessResponse.of(MemberSuccessCode.SIGN_UP_SUCCESS, response));
	}

	@Override
	@GetMapping("/refresh-token")
	public ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> issueAccessTokenUsingRefreshToken(
		@CookieValue(value = REFRESH_TOKEN) final String refreshToken
	) {
		AccessTokenGenerateResponse response = authenticationService.generateAccessTokenFromRefreshToken(refreshToken);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(MemberSuccessCode.ISSUE_ACCESS_TOKEN_USING_REFRESH_TOKEN, response));
	}

	@Override
	@PostMapping("/sign-out")
	public ResponseEntity<SuccessResponse<Void>> signOut(
		@CurrentMember final Long memberId
	) {
		tokenService.deleteRefreshToken(memberId);
		return ResponseEntity.ok()
			.body(SuccessResponse.from(MemberSuccessCode.SIGN_OUT_SUCCESS));
	}
}
