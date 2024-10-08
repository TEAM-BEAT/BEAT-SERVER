package com.beat.domain.member.api;

import java.security.Principal;

import org.springframework.http.ResponseEntity;

import com.beat.domain.member.dto.AccessTokenGetSuccess;
import com.beat.domain.member.dto.LoginSuccessResponse;
import com.beat.global.auth.client.dto.MemberLoginRequest;
import com.beat.global.common.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "Member", description = "회원 관련 API")
public interface MemberApi {

	@Operation(summary = "로그인/회원가입 API", description = "로그인/회원가입하는 POST API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 또는 회원가입 성공",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<LoginSuccessResponse>> signUp(
		String authorizationCode,
		MemberLoginRequest loginRequest,
		HttpServletResponse response);

	@Operation(summary = "access token 재발급 API", description = "refresh token으로 access token을 재발급하는 GET API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "access token 재발급 성공",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<AccessTokenGetSuccess>> refreshToken(
		String refreshToken);

	@Operation(summary = "로그아웃 API", description = "로그아웃하는 POST API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그아웃 성공",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<Void>> signOut(Principal principal);
}
