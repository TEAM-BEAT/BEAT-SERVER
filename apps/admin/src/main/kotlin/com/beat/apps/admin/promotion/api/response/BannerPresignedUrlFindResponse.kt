package com.beat.apps.admin.promotion.api.response

import com.beat.application.admin.promotion.query.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "배너 이미지 업로드용 Presigned URL 조회 응답")
data class BannerPresignedUrlFindResponse(
    @field:Schema(
        description = "배너 이미지 업로드에 사용하는 Presigned URL",
        type = "string",
        format = "uri",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example =
            "https://s3.ap-northeast-2.amazonaws.com/beat-dev/banner/banner.png?X-Amz-Algorithm=AWS4-HMAC-SHA256",
    )
    val bannerPresignedUrl: String,
    @field:Schema(
        description = "배너 업로드 URL과 저장 키",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example =
            """{"uploadUrl":"https://s3.ap-northeast-2.amazonaws.com/beat-dev/banner/banner.png?X-Amz-Algorithm=AWS4-HMAC-SHA256","imageKey":"dev/banner/banner.png"}""",
    )
    val bannerPresignedUpload: BannerPresignedUploadResponse,
) {
    constructor(
        result: BannerPresignedUrlResult
    ) : this(
        bannerPresignedUrl = result.bannerPresignedUrl,
        bannerPresignedUpload =
            BannerPresignedUploadResponse(result.bannerPresignedUrl, result.bannerImageKey),
    )

    @Schema(description = "배너 이미지 업로드 메타데이터")
    data class BannerPresignedUploadResponse(
        @field:Schema(
            description = "배너 이미지 업로드에 사용하는 Presigned URL",
            type = "string",
            format = "uri",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example =
                "https://s3.ap-northeast-2.amazonaws.com/beat-dev/banner/banner.png?X-Amz-Algorithm=AWS4-HMAC-SHA256",
        )
        val uploadUrl: String,
        @field:Schema(
            description = "S3에 저장된 배너 이미지 키",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "dev/banner/banner.png",
        )
        val imageKey: String,
    )
}
