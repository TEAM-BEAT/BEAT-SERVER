package com.beat.domain.admin.api;

import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.external.s3.application.dto.BannerPresignedUrlFindResponse;
import com.beat.global.external.s3.application.dto.CarouselPresignedUrlFindAllResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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

    @Operation(summary = "캐러셀 이미지 프리사인드 URL 생성", description = "관리자가 캐러셀 이미지의 프리사인드 URL을 생성합니다.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프리사인드 URL 생성에 성공하였습니다.",
                            content = @Content(schema = @Schema(implementation = SuccessResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "회원이 없습니다.",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<SuccessResponse<CarouselPresignedUrlFindAllResponse>> createAllCarouselPresignedUrls(
            @CurrentMember Long memberId,
            @RequestParam List<String> carouselImages
    );

    @Operation(summary = "배너 이미지 프리사인드 URL 생성", description = "관리자가 배너 이미지의 프리사인드 URL을 생성합니다.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프리사인드 URL 생성에 성공하였습니다.",
                            content = @Content(schema = @Schema(implementation = SuccessResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "회원이 없습니다.",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<SuccessResponse<BannerPresignedUrlFindResponse>> createBannerPresignedUrl(
            @CurrentMember Long memberId,
            @RequestParam String bannerImage
    );
}
