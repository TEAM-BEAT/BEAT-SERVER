package com.beat.domain.admin.api;

import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin", description = "관리자 제어 API")
public interface AdminApi {

    @Operation(summary = "유저 정보 조회")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관리자 권한으로 모든 유저 조회에 성공하였습니다.",
                            content = @Content(schema = @Schema(implementation = SuccessResponse.class))
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
