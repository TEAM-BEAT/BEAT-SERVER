package com.beat.infrastructure.persistence.booking.mapper

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.infrastructure.persistence.booking.entity.BookingJpaEntity
import com.beat.infrastructure.persistence.booking.entity.RefundAccountJpaValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class BookingPersistenceMapperTest : FunSpec({
    val mapper = BookingPersistenceMapper()

    test("toDomain은 JPA entity 필드를 보존한다") {
        val createdAt = LocalDateTime.of(2026, 4, 29, 19, 10)
        val cancellationDate = LocalDateTime.of(2026, 4, 30, 19, 10)
        val entity = BookingJpaEntity.rehydrate(
            11L,
            2,
            "booker",
            "010-1234-5678",
            BookingStatus.BOOKING_CANCELLED,
            createdAt,
            cancellationDate,
            "990101",
            "1234",
            RefundAccountJpaValue(BankName.KAKAOBANK, "111-222", "holder"),
            22L,
            33L,
            30_000,
        )

        val booking = mapper.toDomain(entity)

        booking.id shouldBe 11L
        booking.purchaseTicketCount shouldBe 2
        booking.bookerName shouldBe "booker"
        booking.bookerPhoneNumber shouldBe "010-1234-5678"
        booking.bookingStatus shouldBe BookingStatus.BOOKING_CANCELLED
        booking.createdAt shouldBe createdAt
        booking.cancellationDate shouldBe cancellationDate
        booking.bankName shouldBe BankName.KAKAOBANK
        booking.accountNumber shouldBe "111-222"
        booking.accountHolder shouldBe "holder"
        booking.totalPaymentAmount shouldBe 30_000
        booking.scheduleId shouldBe 22L
        booking.userId shouldBe 33L
    }

    test("toEntity는 신규 booking의 생성 id를 null로 유지한다") {
        val booking = Booking.create(
            1,
            "new-booker",
            "010-0000-0000",
            "000101",
            "pw",
            44L,
            55L,
            LocalDateTime.of(2026, 4, 29, 19, 10),
            20_000,
        )

        val entity = mapper.toEntity(booking)

        entity.id shouldBe null
        entity.purchaseTicketCount shouldBe 1
        entity.bookerName shouldBe "new-booker"
        entity.bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
        entity.totalPaymentAmount shouldBe 20_000
        entity.scheduleId shouldBe 44L
        entity.userId shouldBe 55L
    }

    test("왕복 시 환불 필드를 보존한다") {
        val createdAt = LocalDateTime.of(2026, 4, 29, 19, 20)
        val booking = Booking.rehydrate(
            31L,
            3,
            "refund-booker",
            "010-9999-9999",
            BookingStatus.REFUND_REQUESTED,
            createdAt,
            null,
            "991231",
            "pw",
            RefundAccount.of(BankName.TOSSBANK, "999-888", "refund-holder"),
            41L,
            51L,
            45_000,
        )

        val roundTrip = mapper.toDomain(mapper.toEntity(booking))

        roundTrip.id shouldBe booking.id
        roundTrip.bookingStatus shouldBe booking.bookingStatus
        roundTrip.createdAt shouldBe booking.createdAt
        roundTrip.bankName shouldBe booking.bankName
        roundTrip.accountNumber shouldBe booking.accountNumber
        roundTrip.accountHolder shouldBe booking.accountHolder
        roundTrip.totalPaymentAmount shouldBe booking.totalPaymentAmount
        roundTrip.scheduleId shouldBe booking.scheduleId
        roundTrip.userId shouldBe booking.userId
    }
})
