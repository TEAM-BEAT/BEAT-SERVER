package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.CastResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 결과의 출연진 정보")
@ConsistentCopyVisibility
data class CastResponse
private constructor(
    @field:Schema(
        description = "출연진 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val castId: Long?,
    @field:Schema(
        description = "출연진 이름",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "홍길동",
    )
    val castName: String?,
    @field:Schema(
        description = "출연진 역할",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "주연",
    )
    val castRole: String?,
    @field:Schema(
        description = "출연진 사진의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/cast/cast-1.jpg",
    )
    @field:CdnImageUrl
    val castPhoto: String?,
) {
    companion object {
        fun from(result: CastResult): CastResponse =
            CastResponse(result.id, result.name, result.role, result.photo)
    }
}
