package com.beat.apps.admin.promotion.api.response

import com.beat.application.admin.promotion.command.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.application.admin.promotion.command.PromotionImageUpload
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "캐러셀 이미지 업로드용 Presigned URL 전체 조회 응답")
data class CarouselPresignedUrlFindAllResponse(
    @field:Schema(
        description = "이미지 파일명별 캐러셀 이미지 업로드 Presigned URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example =
            """{"carousel.png":"https://s3.ap-northeast-2.amazonaws.com/beat-dev/carousel/carousel.png?X-Amz-Algorithm=AWS4-HMAC-SHA256"}""",
    )
    val carouselPresignedUrls: Map<String, String>,
    @field:Schema(
        description = "이미지 파일명별 캐러셀 이미지 업로드 메타데이터",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example =
            """{"carousel.png":{"uploadUrl":"https://s3.ap-northeast-2.amazonaws.com/beat-dev/carousel/carousel.png?X-Amz-Algorithm=AWS4-HMAC-SHA256","imageKey":"dev/carousel/carousel.png"}}""",
    )
    val carouselPresignedUploads: Map<String, CarouselPresignedUploadResponse>,
) {
    constructor(
        result: CarouselPresignedUrlsResult
    ) : this(
        carouselPresignedUrls = result.carouselPresignedUploads.mapValues { it.value.uploadUrl },
        carouselPresignedUploads =
            result.carouselPresignedUploads.mapValues {
                CarouselPresignedUploadResponse(it.value)
            },
    )

    @Schema(description = "캐러셀 이미지 업로드 메타데이터")
    data class CarouselPresignedUploadResponse(
        @field:Schema(
            description = "캐러셀 이미지 업로드에 사용하는 Presigned URL",
            type = "string",
            format = "uri",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example =
                "https://s3.ap-northeast-2.amazonaws.com/beat-dev/carousel/carousel.png?X-Amz-Algorithm=AWS4-HMAC-SHA256",
        )
        val uploadUrl: String,
        @field:Schema(
            description = "S3에 저장된 캐러셀 이미지 키",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "dev/carousel/carousel.png",
        )
        val imageKey: String,
    ) {
        constructor(upload: PromotionImageUpload) : this(upload.uploadUrl, upload.imageKey)
    }
}
