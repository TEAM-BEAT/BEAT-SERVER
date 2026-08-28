package com.beat.apps.api.booking.api.request

import com.beat.apps.api.performance.api.type.BankNameType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "유료 공연 예매 환불 요청")
data class BookingRefundRequest(
    @field:Schema(
        description = "환불을 요청할 예매의 식별자입니다.",
        example = "1001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingId: Long,
    @field:Schema(
        description = "환불받을 은행입니다. NONE은 환불 계좌로 사용할 수 없습니다.",
        allowableValues =
            [
                "NH_NONGHYUP",
                "KAKAOBANK",
                "KB_KOOKMIN",
                "TOSSBANK",
                "SHINHAN",
                "WOORI",
                "IBK_GIUP",
                "HANA",
                "SAEMAUL",
                "BUSAN",
                "IMBANK_DAEGU",
                "SINHYEOP",
                "WOOCHAEGUK",
                "SCJEIL",
                "SUHYEOP",
            ],
        example = "KAKAOBANK",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bankName: BankNameType,
    @field:NotBlank(message = "환불받을 계좌번호는 공백일 수 없습니다.")
    @field:Schema(
        description = "환불받을 계좌번호입니다. 공백일 수 없습니다.",
        example = "123456789012",
        minLength = 1,
        pattern = ".*\\S.*",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accountNumber: String,
    @field:NotBlank(message = "환불받을 계좌의 예금주명은 공백일 수 없습니다.")
    @field:Schema(
        description = "환불받을 계좌의 예금주명입니다. 공백일 수 없습니다.",
        example = "홍길동",
        minLength = 1,
        pattern = ".*\\S.*",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accountHolder: String,
)
