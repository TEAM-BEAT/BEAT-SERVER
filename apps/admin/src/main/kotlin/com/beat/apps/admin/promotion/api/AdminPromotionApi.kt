package com.beat.apps.admin.promotion.api

import com.beat.apps.admin.promotion.api.request.CarouselHandleRequest
import com.beat.apps.admin.promotion.api.response.BannerPresignedUrlFindResponse
import com.beat.apps.admin.promotion.api.response.CarouselFindAllResponse
import com.beat.apps.admin.promotion.api.response.CarouselHandleAllResponse
import com.beat.apps.admin.promotion.api.response.CarouselPresignedUrlFindAllResponse
import com.beat.apps.admin.response.ErrorResponse
import com.beat.apps.admin.response.SuccessResponse
import com.beat.support.security.CurrentMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Admin", description = "관리자 제어 API")
interface AdminPromotionApi {

    @Operation(
        operationId = "createAllCarouselPresignedUrls",
        summary = "캐러셀 이미지 업로드용 Presigned URL 일괄 발급",
        description = "관리자가 캐러셀 이미지 파일명 목록을 전달하면 각 이미지의 S3 업로드용 Presigned URL을 발급합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "캐러셀 Presigned URL 발급 성공"),
                ApiResponse(
                    responseCode = "404",
                    description = "회원이 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun createAllCarouselPresignedUrls(
        @CurrentMember memberId: Long,
        @Parameter(
            description = "캐러셀에 업로드할 이미지 파일명 목록",
            example = "carousel.png",
            required = true,
        )
        @RequestParam
        carouselImages: List<String>,
    ): ResponseEntity<SuccessResponse<CarouselPresignedUrlFindAllResponse>>

    @Operation(
        operationId = "createBannerPresignedUrl",
        summary = "배너 이미지 업로드용 Presigned URL 발급",
        description = "관리자가 배너 이미지 파일명을 전달하면 S3 업로드용 Presigned URL과 이미지 키를 발급합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "배너 Presigned URL 발급 성공"),
                ApiResponse(
                    responseCode = "404",
                    description = "회원이 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun createBannerPresignedUrl(
        @CurrentMember memberId: Long,
        @Parameter(
            description = "배너에 업로드할 이미지 파일명",
            example = "banner.png",
            required = true,
        )
        @RequestParam
        bannerImage: String,
    ): ResponseEntity<SuccessResponse<BannerPresignedUrlFindResponse>>

    @Operation(
        operationId = "readAllCarouselImages",
        summary = "캐러셀 프로모션 전체 조회",
        description = "관리자가 현재 캐러셀 번호 순으로 등록된 프로모션 전체를 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "관리자 권한으로 현재 캐러셀에 등록된 모든 공연 조회에 성공하였습니다.",
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원이 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun readAllCarouselImages(
        @CurrentMember memberId: Long
    ): ResponseEntity<SuccessResponse<CarouselFindAllResponse>>

    @Operation(
        operationId = "processCarouselImages",
        summary = "캐러셀 프로모션 일괄 생성·수정",
        description = "관리자가 요청 항목의 유형에 따라 캐러셀 프로모션을 생성하거나 수정합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "캐러셀 이미지 수정 성공"),
                ApiResponse(
                    responseCode = "400",
                    description =
                        "요청 형식이 잘못되었거나 노출 가능한 캐러셀 프로모션 수를 초과했거나 중복된 carousel 번호 또는 promotion id가 포함되었거나, 존재하지 않는 업로드 이미지가 지정되었습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "관리자 회원, 수정 대상 홍보 정보 또는 연결된 공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun processCarouselImages(
        @CurrentMember memberId: Long,
        @SwaggerRequestBody(
            description = "캐러셀 프로모션 생성 또는 수정 요청 본문",
            required = true,
        )
        @Valid
        @RequestBody
        request: CarouselHandleRequest,
    ): ResponseEntity<SuccessResponse<CarouselHandleAllResponse>>
}
