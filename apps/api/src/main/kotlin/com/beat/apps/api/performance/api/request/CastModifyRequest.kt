package com.beat.apps.api.performance.api.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 수정 시 출연진 한 명의 정보")
data class CastModifyRequest(
    @field:Schema(
        description = "기존 출연진의 식별자이며, 신규 출연진을 추가할 때는 null입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "1",
    )
    val castId: Long?,
    @field:Schema(
        description = "출연진 이름",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "홍길동",
    )
    val castName: String,
    @field:Schema(
        description = "출연진 역할",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "주연",
    )
    val castRole: String,
    @field:Schema(
        description = "업로드된 출연진 사진의 이미지 key 또는 절대 URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "dev/cast/cast-1.jpg",
    )
    val castPhoto: String,
)
