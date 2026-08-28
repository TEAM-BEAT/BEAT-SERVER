package com.beat.apps.api.booking.api.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "비회원 공연 예매 생성 요청")
data class GuestBookingRequest(
    @field:Schema(
        description = "예매할 공연 회차의 식별자입니다.",
        example = "2001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val scheduleId: Long,
    @field:Schema(
        description = "예매할 티켓 수량입니다. 1장부터 10장까지 입력할 수 있습니다.",
        example = "2",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val purchaseTicketCount: Int,
    @field:Schema(
        description = "예매자 이름입니다. 한글 또는 영문 문자만 허용됩니다.",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookerName: String,
    @field:Schema(
        description = "예매자 전화번호입니다. 숫자 3-4-4 하이픈 형식이어야 합니다.",
        example = "010-1234-5678",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookerPhoneNumber: String,
    @field:Schema(
        description = "예매자 생년월일입니다. 6자리 숫자로 입력합니다.",
        example = "900101",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val birthDate: String,
    @field:Schema(
        description = "비회원 예매 조회에 사용할 비밀번호입니다. 4자리 숫자로 입력합니다.",
        example = "1234",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val password: String,
)
