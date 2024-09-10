package com.beat.domain.member.api;

import com.beat.domain.member.application.AuthenticationService;
import com.beat.domain.member.application.MemberService;
import com.beat.domain.member.application.SocialLoginService;
import com.beat.domain.member.dto.*;
import com.beat.domain.member.exception.MemberSuccessCode;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.auth.jwt.application.TokenService;
import com.beat.global.common.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController {
    private final TokenService tokenService;
    private final AuthenticationService authenticationService;
    private final SocialLoginService socialLoginService;

    private final static int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    private final static String REFRESH_TOKEN = "refreshToken";

    @Operation(summary = "로그인/회원가입 API", description = "로그인/회원가입하는 POST API입니다.")
    @PostMapping("/sign-up")
    public ResponseEntity<SuccessResponse<LoginSuccessResponse>> signUp(
            @RequestParam final String authorizationCode,
            @RequestBody final MemberLoginRequest loginRequest,
            HttpServletResponse response
    ) {
        LoginSuccessResponse loginSuccessResponse = socialLoginService.handleSocialLogin(authorizationCode, loginRequest);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, loginSuccessResponse.refreshToken())
                .maxAge(COOKIE_MAX_AGE)
                .path("/")
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();
        response.setHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(MemberSuccessCode.SIGN_UP_SUCCESS, LoginSuccessResponse.of(loginSuccessResponse.accessToken(), null, loginSuccessResponse.nickname(), loginSuccessResponse.role())));
    }

    @Operation(summary = "access token 재발급 API", description = "refresh token으로 access token을 재발급하는 GET API입니다.")
    @GetMapping("/refresh-token")
    public ResponseEntity<SuccessResponse<AccessTokenGetSuccess>> refreshToken(
            @RequestParam final String refreshToken
    ) {
        AccessTokenGetSuccess accessTokenGetSuccess = authenticationService.generateAccessTokenFromRefreshToken(refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(MemberSuccessCode.ISSUE_REFRESH_TOKEN_SUCCESS, accessTokenGetSuccess));
    }

    @Operation(summary = "로그아웃 API", description = "로그아웃하는 POST API입니다.")
    @PostMapping("/sign-out")
    public ResponseEntity<SuccessResponse<Void>> signOut(
            final Principal principal
    ) {
        tokenService.deleteRefreshToken(Long.valueOf(principal.getName()));
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.from(MemberSuccessCode.SIGN_OUT_SUCCESS));
    }
}