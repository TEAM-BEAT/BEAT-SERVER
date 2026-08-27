package com.beat.apps.api.member.api

import com.beat.apps.api.member.api.request.MemberLoginRequest
import com.beat.apps.api.member.api.response.AccessTokenGenerateResponse
import com.beat.apps.api.member.api.response.MemberLoginResponse
import com.beat.apps.api.member.api.response.MemberSuccessCode
import com.beat.apps.api.member.facade.MemberFacade
import com.beat.apps.api.response.SuccessResponse
import com.beat.support.security.CurrentMember
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class MemberController(private val memberFacade: MemberFacade) : MemberApi {

    @PostMapping("/sign-up")
    override fun signUp(
        @RequestParam authorizationCode: String,
        @Valid @RequestBody loginRequest: MemberLoginRequest,
        httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<MemberLoginResponse>> {
        val loginSession = memberFacade.handleSocialLogin(authorizationCode, loginRequest)

        val cookie =
            ResponseCookie.from(REFRESH_TOKEN, loginSession.refreshToken)
                .maxAge(COOKIE_MAX_AGE.toLong())
                .path("/")
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build()
        httpServletResponse.setHeader("Set-Cookie", cookie.toString())

        return ResponseEntity.ok()
            .body(SuccessResponse.of(MemberSuccessCode.SIGN_UP_SUCCESS, loginSession.response))
    }

    @GetMapping("/refresh-token")
    override fun issueAccessTokenUsingRefreshToken(
        @CookieValue(value = REFRESH_TOKEN) refreshToken: String
    ): ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> {
        val response = memberFacade.generateAccessTokenFromRefreshToken(refreshToken)
        return ResponseEntity.ok()
            .body(
                SuccessResponse.of(
                    MemberSuccessCode.ISSUE_ACCESS_TOKEN_USING_REFRESH_TOKEN,
                    response,
                )
            )
    }

    @PostMapping("/sign-out")
    override fun signOut(@CurrentMember memberId: Long): ResponseEntity<SuccessResponse<Void?>> {
        memberFacade.signOut(memberId)
        return ResponseEntity.ok().body(SuccessResponse.from(MemberSuccessCode.SIGN_OUT_SUCCESS))
    }

    private companion object {
        const val COOKIE_MAX_AGE = 7 * 24 * 60 * 60
        const val REFRESH_TOKEN = "refreshToken"
    }
}
