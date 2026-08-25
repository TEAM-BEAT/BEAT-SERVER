package com.beat.domain.booking.fixture

import com.beat.domain.booking.model.Booking
import java.time.LocalDateTime

fun bookingFixture(
    purchaseTicketCount: Int = 1,
    bookerName: String = "booker",
    bookerPhoneNumber: String = "010-1234-5678",
    birthDate: String? = "990101",
    password: String? = "1234",
    scheduleId: Long = 2L,
    userId: Long = 3L,
    createdAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 12, 0),
    totalPaymentAmount: Int = 10_000,
): Booking =
    Booking.create(
        purchaseTicketCount = purchaseTicketCount,
        bookerName = bookerName,
        bookerPhoneNumber = bookerPhoneNumber,
        birthDate = birthDate,
        password = password,
        scheduleId = scheduleId,
        userId = userId,
        createdAt = createdAt,
        totalPaymentAmount = totalPaymentAmount,
    )
