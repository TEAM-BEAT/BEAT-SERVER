package com.beat.domain.member.api;

import com.beat.domain.member.application.MemberService;
import com.beat.domain.member.dto.*;
import com.beat.domain.member.exception.MemberSuccessCode;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.auth.jwt.application.TokenService;
import com.beat.global.common.dto.SuccessResponse;
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
    private final MemberService memberService;
    private final TokenService tokenService;
    private final static int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    private final static String REFRESH_TOKEN = "refreshToken";

    @PostMapping("/sign-up")
    public ResponseEntity<SuccessResponse<LoginSuccessResponse>> signUp(
            @RequestParam final String authorizationCode,
            @RequestBody final MemberLoginRequest loginRequest,
            HttpServletResponse response
    ) {
        LoginSuccessResponse loginSuccessResponse = memberService.create(authorizationCode, loginRequest);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, loginSuccessResponse.refreshToken())
                .maxAge(COOKIE_MAX_AGE)
                .path("/")
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();
        response.setHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(MemberSuccessCode.SIGN_UP_SUCCESS, LoginSuccessResponse.of(loginSuccessResponse.accessToken(), null, loginSuccessResponse.nickname())));
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<SuccessResponse<AccessTokenGetSuccess>> refreshToken(
            @RequestParam final String refreshToken
    ) {
        AccessTokenGetSuccess accessTokenGetSuccess = memberService.refreshToken(refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(MemberSuccessCode.ISSUE_REFRESH_TOKEN_SUCCESS, accessTokenGetSuccess));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<SuccessResponse<Void>> signOut(
            final Principal principal
    ) {
        tokenService.deleteRefreshToken(Long.valueOf(principal.getName()));
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.from(MemberSuccessCode.SIGN_OUT_SUCCESS));
    }
}