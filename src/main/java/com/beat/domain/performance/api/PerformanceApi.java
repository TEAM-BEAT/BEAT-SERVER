package com.beat.domain.performance.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.beat.domain.performance.application.dto.bookingPerformanceDetail.BookingPerformanceDetailResponse;
import com.beat.domain.performance.application.dto.create.PerformanceRequest;
import com.beat.domain.performance.application.dto.create.PerformanceResponse;
import com.beat.domain.performance.application.dto.makerPerformance.MakerPerformanceResponse;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyDetailResponse;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyRequest;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyResponse;
import com.beat.domain.performance.application.dto.performanceDetail.PerformanceDetailResponse;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.swagger.annotation.DisableSwaggerSecurity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Performance", description = "공연 관련 API")
public interface PerformanceApi {

	@Operation(summary = "공연 생성 API", description = "공연을 생성하는 POST API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "201",
				description = "공연이 성공적으로 생성되었습니다."
			),
			@ApiResponse(
				responseCode = "400",
				description = "필수 데이터가 누락되었습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<PerformanceResponse>> createPerformance(
		@CurrentMember Long memberId,
		@RequestBody PerformanceRequest performanceRequest
	);

	@Operation(summary = "공연 정보 수정 API", description = "공연 정보를 수정하는 PUT API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "공연 정보 수정 성공"
			),
			@ApiResponse(
				responseCode = "400",
				description = "공연 회차는 최대 10개까지 추가할 수 있습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "400",
				description = "티켓 가격은 음수일 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "400",
				description = "예매자가 존재하여 가격을 수정할 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "403",
				description = "해당 공연의 소유자가 아닙니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<PerformanceModifyResponse>> updatePerformance(
		@CurrentMember Long memberId,
		@RequestBody PerformanceModifyRequest performanceModifyRequest
	);

	@Operation(summary = "공연 수정 페이지 정보 조회 API", description = "공연 정보를 조회하는 GET API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "공연 수정 페이지 정보 조회 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<PerformanceModifyDetailResponse>> getPerformanceForEdit(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId
	);

	@DisableSwaggerSecurity
	@Operation(summary = "공연 상세정보 조회 API", description = "공연 상세페이지의 공연 상세정보를 조회하는 GET API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "공연 상세정보 조회 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<PerformanceDetailResponse>> getPerformanceDetail(
		@PathVariable Long performanceId
	);

	@DisableSwaggerSecurity
	@Operation(summary = "예매하기 관련 공연 정보 조회 API", description = "예매하기 페이지에서 필요한 예매 관련 공연 정보를 조회하는 GET API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "예매하기 관련 공연 정보 조회 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<BookingPerformanceDetailResponse>> getBookingPerformanceDetail(
		@PathVariable Long performanceId
	);

	@Operation(summary = "회원이 등록한 공연 목록 조회 API", description = "회원이 등록한 공연 목록을 조회하는 GET API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "회원이 등록한 공연 목록 조회 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "회원 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<MakerPerformanceResponse>> getUserPerformances(
		@CurrentMember Long memberId
	);

	@Operation(summary = "공연 삭제 API", description = "공연을 삭제하는 DELETE API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "공연 삭제 성공"
			),
			@ApiResponse(
				responseCode = "403",
				description = "공연의 소유자가 아니거나 예매자가 있어 삭제할 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<Void>> deletePerformance(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId
	);
}
