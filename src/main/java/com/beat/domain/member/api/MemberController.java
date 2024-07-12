package com.beat.domain.member.api;

import com.beat.domain.member.application.MemberService;
import com.beat.domain.member.dto.*;
import com.beat.domain.member.exception.MemberSuccessCode;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.auth.jwt.application.TokenService;
import com.beat.global.common.dto.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final TokenService tokenService;

    @PostMapping("/sign-up")
    public ResponseEntity<SuccessResponse<LoginSuccessResponse>> signUp(
            @RequestParam final String authorizationCode,
            @RequestBody final MemberLoginRequest loginRequest
    ) {
        LoginSuccessResponse loginSuccessResponse = memberService.create(authorizationCode, loginRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(MemberSuccessCode.SIGN_UP_SUCCESS, loginSuccessResponse));
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<SuccessResponse<AccessTokenGetSuccess>> refreshToken(
//            @RequestParam final String refreshToken
            @RequestBody final String refreshToken
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
