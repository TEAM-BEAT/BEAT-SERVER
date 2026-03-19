package com.beat.domain.user.api;

import org.springframework.web.bind.annotation.GetMapping;

import com.beat.global.swagger.annotation.DisableSwaggerSecurity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health-Check", description = "헬스 체크 API")
public interface HealthCheckApi {

	@DisableSwaggerSecurity
	@Operation(summary = "헬스 체크 조회 API", description = "서버 상태를 확인하기 위한 헬스 체크 API로, 정상적으로 동작할 경우 'OK' 문자열을 반환합니다.")
	@ApiResponses(
		value = {
			@ApiResponse(responseCode = "200", description = "서버가 정상적으로 동작 중입니다.")
		}
	)
	String healthcheck();
}
