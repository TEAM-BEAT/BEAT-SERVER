package com.beat.apps.api.performance.api.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 수정 시 스태프 한 명의 정보")
data class StaffModifyRequest(
    @field:Schema(
        description = "기존 스태프의 식별자이며, 신규 스태프를 추가할 때는 null입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "1",
    )
    val staffId: Long?,
    @field:Schema(
        description = "스태프 이름",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "김기획",
    )
    val staffName: String,
    @field:Schema(
        description = "스태프 역할",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "연출",
    )
    val staffRole: String,
    @field:Schema(
        description = "업로드된 스태프 사진의 이미지 key 또는 절대 URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "dev/staff/staff-1.jpg",
    )
    val staffPhoto: String,
)
