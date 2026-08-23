package com.beat.apis.file.api

import com.beat.apis.file.api.response.PerformanceMakerPresignedUrlFindAllResponse
import com.beat.apis.response.ErrorResponse
import com.beat.apis.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
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
        summary = "공연 이미지 업로드 Presigned URL 발급",
        description = "공연 등록 시 업로드할 이미지에 대한 presigned URL을 발급 받는 GET API",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "공연 메이커를 위한 Presigned URL 발급 성공."),
            ApiResponse(
                responseCode = "500",
                description = "S3 PreSigned url을 받아오기에 실패했습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun generateAllPresignedUrls(
        @RequestParam posterImage: String,
        @RequestParam(required = false) castImages: List<String>?,
        @RequestParam(required = false) staffImages: List<String>?,
        @RequestParam(required = false) performanceImages: List<String>?,
    ): ResponseEntity<SuccessResponse<PerformanceMakerPresignedUrlFindAllResponse>>
}
