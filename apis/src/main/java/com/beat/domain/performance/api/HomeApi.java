package com.beat.domain.performance.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import com.beat.domain.performance.application.dto.home.HomeFindAllResponse;
import com.beat.domain.performance.domain.Genre;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.swagger.annotation.DisableSwaggerSecurity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Home", description = "홈 화면에서 공연 및 홍보목록 조회 API")
public interface HomeApi {

	@DisableSwaggerSecurity
	@Operation(summary = "전체 공연 및 홍보 목록 조회", description = "홈 화면에서 전체 공연 목록 및 홍보 목록을 조회하는 GET API")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "홈 화면 공연 목록 조회가 성공적으로 완료되었습니다."
			)
		}
	)
	ResponseEntity<SuccessResponse<HomeFindAllResponse>> getHomePerformanceList(
		@RequestParam(required = false) Genre genre);
}
