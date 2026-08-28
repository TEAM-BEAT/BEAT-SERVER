package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.command.result.BookingRefundResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.performance.api.type.BankNameType
import io.swagger.v3.oas.annotations.media.Schema

@ConsistentCopyVisibility
@Schema(description = "예매 환불 요청 처리 결과")
data class BookingRefundResponse
private constructor(
    @field:Schema(
        description = "환불 요청이 처리된 예매의 식별자입니다.",
        example = "1001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingId: Long,
    @field:Schema(
        description = "환불 요청 처리 후 예매 상태입니다.",
        example = "REFUND_REQUESTED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingStatus: BookingStatusType,
    @field:Schema(
        description = "환불 계좌의 은행입니다.",
        example = "KAKAOBANK",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bankName: BankNameType,
    @field:Schema(
        description = "환불 계좌번호입니다.",
        example = "123456789012",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accountNumber: String,
    @field:Schema(
        description = "환불 계좌의 예금주명입니다.",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accountHolder: String,
) {
    companion object {
        fun from(result: BookingRefundResult): BookingRefundResponse =
            BookingRefundResponse(
                bookingId = result.bookingId,
                bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
                bankName = BankNameType.valueOf(result.bankName),
                accountNumber = result.accountNumber,
                accountHolder = result.accountHolder,
            )
    }
}
