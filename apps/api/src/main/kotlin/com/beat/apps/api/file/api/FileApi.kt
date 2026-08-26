package com.beat.apps.api.file.api

import com.beat.apps.api.file.api.response.PerformanceMakerPresignedUrlFindAllResponse
import com.beat.apps.api.response.ErrorResponse
import com.beat.apps.api.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Image - Performance PreSigned Url", description = "Performance PreSigned Url 발급 API")
interface FileApi {

    @Operation(
        operationId = "fileGeneratePerformanceImagePresignedUrls",
        summary = "공연 이미지 업로드 Presigned URL 발급",
        description = "공연 등록에 사용할 이미지 파일명으로 S3 PUT presigned URL과 object key를 발급합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "공연 메이커를 위한 Presigned URL 발급 성공."),
                ApiResponse(
                    responseCode = "500",
                    description = "S3 PreSigned url을 받아오기에 실패했습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun generateAllPresignedUrls(
        @Parameter(
            description = "업로드할 포스터 이미지의 원본 파일명입니다. URL이나 S3 object key가 아닌 파일명만 전달합니다.",
            example = "poster.png",
            required = true,
        )
        @RequestParam
        posterImage: String,
        @Parameter(
            description = "업로드할 출연진 이미지의 원본 파일명 목록입니다. URL이나 S3 object key가 아닌 파일명만 전달합니다.",
            example = "cast.png",
            required = false,
        )
        @RequestParam(required = false)
        castImages: List<String>?,
        @Parameter(
            description = "업로드할 스태프 이미지의 원본 파일명 목록입니다. URL이나 S3 object key가 아닌 파일명만 전달합니다.",
            example = "staff.png",
            required = false,
        )
        @RequestParam(required = false)
        staffImages: List<String>?,
        @Parameter(
            description = "업로드할 공연 상세 이미지의 원본 파일명 목록입니다. URL이나 S3 object key가 아닌 파일명만 전달합니다.",
            example = "performance.png",
            required = false,
        )
        @RequestParam(required = false)
        performanceImages: List<String>?,
    ): ResponseEntity<SuccessResponse<PerformanceMakerPresignedUrlFindAllResponse>>
}
