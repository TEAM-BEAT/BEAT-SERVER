package com.beat.apps.api.ticket.api.request

import com.beat.apps.api.booking.api.type.BookingStatusType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "입금 여부를 수정할 예매자 한 명의 정보입니다.")
data class TicketUpdateDetail(
    @field:Schema(
        description = "입금 여부를 수정할 예매 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val bookingId: Long,
    @field:Schema(
        description = "예매자 이름입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "booker",
    )
    val bookerName: String?,
    @field:Schema(
        description = "예매자 전화번호입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "010-0000-0000",
    )
    val bookerPhoneNumber: String?,
    @field:Schema(
        description = "예매가 연결된 회차 식별자입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "1",
    )
    val scheduleId: Long?,
    @field:Schema(
        description = "구매한 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "2",
    )
    val purchaseTicketCount: Int?,
    @field:Schema(
        description = "예매 생성 일시입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "2026-04-01T12:00:00",
    )
    val createdAt: LocalDateTime?,
    @field:Schema(
        description = "변경할 예매 상태입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "CHECKING_PAYMENT",
    )
    val bookingStatus: BookingStatusType,
    @field:Schema(
        description = "예매가 연결된 회차 번호입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "FIRST",
    )
    val scheduleNumber: String?,
)
