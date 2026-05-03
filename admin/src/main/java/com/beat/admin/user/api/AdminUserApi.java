package com.beat.admin.user.api;

import org.springframework.http.ResponseEntity;

import com.beat.admin.user.application.dto.response.UserFindAllResponse;
import com.beat.gateway.security.servlet.CurrentMember;
import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin", description = "관리자 제어 API")
public interface AdminUserApi {

	@Operation(summary = "유저 정보 조회", description = "관리자가 유저들의 정보를 조회하는 GET API")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "관리자 권한으로 모든 유저 조회에 성공하였습니다."
			),
			@ApiResponse(
				responseCode = "404",
				description = "회원이 없습니다",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<UserFindAllResponse>> readAllUsers(
		@CurrentMember Long memberId
	);
}
