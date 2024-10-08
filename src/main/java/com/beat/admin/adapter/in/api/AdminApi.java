package com.beat.admin.adapter.in.api;

import com.beat.admin.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.application.dto.response.UserFindAllResponse;
import com.beat.admin.application.dto.request.CarouselHandleRequest;
import com.beat.admin.application.dto.response.CarouselHandleAllResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Admin", description = "관리자 제어 API")
public interface AdminApi {

    @Operation(summary = "유저 정보 조회", description = "관리자가 유저들의 정보를 조회하는 GET API")
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

    @Operation(summary = "캐러셀에 업로드 할 이미지에 대한 presigned URL 발급", description = "관리자가 캐러셀에 업로드 할 이미지에 대한 presigned URL을 발급 받는 GET API")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "캐러셀 Presigned URL 발급 성공",
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

    @Operation(summary = "배너에 업로드 할 이미지에 대한 presigned URL 발급", description = "관리자가 배너에 업로드 할 이미지에 대한 presigned URL을 발급 받는 GET API")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "배너 Presigned URL 발급 성공",
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

    @Operation(summary = "캐러셀에 등록된 모든 공연 정보 조회", description = "관리자가 현재 캐러셀에 등록된 모든 공연 정보를 조회하는 GET API")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관리자 권한으로 현재 캐러셀에 등록된 모든 공연 조회에 성공하였습니다.",
                            content = @Content(schema = @Schema(implementation = SuccessResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "회원이 없습니다.",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<SuccessResponse<CarouselFindAllResponse>> readAllCarouselImages(
            @CurrentMember Long memberId
    );

    @Operation(summary = "캐러셀 이미지 수정", description = "관리자가 캐러셀 이미지를 수정하는 PUT API")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "캐러셀 이미지 수정 성공",
                content = @Content(schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "회원이 없습니다.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "해당 홍보 정보를 찾을 수 없습니다.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "해당 공연 정보를 찾을 수 없습니다.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
        }
    )
    ResponseEntity<SuccessResponse<CarouselHandleAllResponse>> processCarouselImages(
        @CurrentMember Long memberId,
        @RequestBody CarouselHandleRequest request
    );
}
