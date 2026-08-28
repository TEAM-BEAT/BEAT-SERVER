package com.beat.apps.api.member.api

import com.beat.apps.api.member.api.request.MemberLoginRequest
import com.beat.apps.api.member.api.response.AccessTokenGenerateResponse
import com.beat.apps.api.member.api.response.MemberLoginResponse
import com.beat.apps.api.response.ErrorResponse
import com.beat.apps.api.response.SuccessResponse
import com.beat.apps.api.swagger.annotation.DisableSwaggerSecurity
import com.beat.support.security.CurrentMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Member", description = "회원 관련 API")
interface MemberApi {

    @DisableSwaggerSecurity
    @Operation(
        operationId = "signUpMember",
        summary = "소셜 로그인·회원가입",
        description =
            "인가 코드와 소셜 로그인 제공자 정보를 사용해 로그인하거나 신규 회원으로 가입합니다. 회원 인증 없이 호출할 수 있으며 성공 시 access token을 응답하고 refreshToken HttpOnly 쿠키를 설정합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "소셜 로그인 또는 회원가입이 완료되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 소셜 로그인 제공자이거나 로그인 요청 값이 유효하지 않은 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "401",
                    description = "소셜 로그인 제공자의 인가 코드 인증에 실패했거나 인가 코드가 만료된 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "로그인 과정에서 연결된 회원 또는 사용자 정보를 찾을 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "502",
                    description = "소셜 로그인 제공자의 응답을 처리하지 못한 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "503",
                    description = "소셜 로그인 제공 서비스를 일시적으로 사용할 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "504",
                    description = "소셜 로그인 제공자의 응답이 제한 시간 안에 도착하지 않은 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun signUp(
        @Parameter(
            name = "authorizationCode",
            `in` = ParameterIn.QUERY,
            description = "소셜 로그인 제공자에서 발급한 인가 코드입니다.",
            example = "sample-authorization-code",
            required = true,
        )
        @RequestParam
        authorizationCode: String,
        @SwaggerRequestBody(
            description = "사용할 소셜 로그인 제공자를 담은 요청 본문입니다.",
            required = true,
        )
        @RequestBody
        loginRequest: MemberLoginRequest,
        @Parameter(hidden = true) httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<MemberLoginResponse>>

    @DisableSwaggerSecurity
    @Operation(
        operationId = "issueAccessTokenUsingRefreshToken",
        summary = "refresh token으로 access token 재발급",
        description =
            "회원 인증 없이 refreshToken 쿠키를 검증해 새로운 access token을 발급합니다. refreshToken 쿠키가 필요합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "access token이 재발급되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description = "refreshToken 쿠키가 유효하지 않거나 검증할 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "401",
                    description = "refreshToken 쿠키가 만료되어 인증할 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "서버 저장소에서 해당 refreshToken을 찾을 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun issueAccessTokenUsingRefreshToken(
        @Parameter(
            name = "refreshToken",
            `in` = ParameterIn.COOKIE,
            description = "access token 재발급에 사용할 refresh token 쿠키입니다.",
            example = "refresh-token-example",
            required = true,
        )
        @CookieValue(value = "refreshToken")
        refreshToken: String
    ): ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>>

    @Operation(
        operationId = "signOutMember",
        summary = "회원 로그아웃",
        description = "Bearer 회원 토큰으로 인증된 회원을 로그아웃 처리합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "회원 로그아웃이 완료되었습니다."),
                ApiResponse(
                    responseCode = "404",
                    description = "로그아웃할 회원 정보를 찾을 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun signOut(
        @Parameter(hidden = true) @CurrentMember memberId: Long
    ): ResponseEntity<SuccessResponse<Void?>>
}
