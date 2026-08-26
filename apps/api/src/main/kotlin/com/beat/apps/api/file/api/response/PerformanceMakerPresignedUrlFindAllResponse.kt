package com.beat.apps.api.file.api.response

import com.beat.application.frontoffice.performance.maker.command.ImagePresignedUpload
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 이미지 종류별 presigned 업로드 URL과 S3 object key입니다.")
@ConsistentCopyVisibility
data class PerformanceMakerPresignedUrlFindAllResponse
private constructor(
    @field:Schema(
        description =
            "이미지 종류를 key로 하고, 각 원본 파일명을 key로 하여 PUT presigned URL(uploadUrl)과 S3 object key(imageKey)를 담은 맵입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example =
            "{\"poster\":{\"poster.png\":{\"uploadUrl\":\"https://example.com/poster/uuid-poster.png\",\"imageKey\":\"poster/uuid-poster.png\"}}}",
    )
    val performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>
) {
    companion object {
        fun from(
            performanceMakerPresignedUploads: Map<String, Map<String, ImagePresignedUpload>>
        ): PerformanceMakerPresignedUrlFindAllResponse =
            PerformanceMakerPresignedUrlFindAllResponse(performanceMakerPresignedUploads)
    }
}
