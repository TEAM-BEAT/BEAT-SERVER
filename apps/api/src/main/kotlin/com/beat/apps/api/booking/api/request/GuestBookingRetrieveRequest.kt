package com.beat.apps.api.booking.api.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "비회원 예매 내역 조회 요청")
data class GuestBookingRetrieveRequest(
    @field:Schema(
        description = "예매 시 입력한 예매자 이름입니다. 한글 또는 영문 문자만 허용됩니다.",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookerName: String,
    @field:Schema(
        description = "예매 시 입력한 예매자 생년월일입니다. 6자리 숫자로 입력합니다.",
        example = "900101",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val birthDate: String,
    @field:Schema(
        description = "예매 시 입력한 예매자 전화번호입니다. 숫자 3-4-4 하이픈 형식이어야 합니다.",
        example = "010-1234-5678",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookerPhoneNumber: String,
    @field:Schema(
        description = "예매 시 설정한 비밀번호입니다. 4자리 숫자로 입력합니다.",
        example = "1234",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val password: String,
)
