package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.BookingHistoryPerformanceSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistoryReadPort
import com.beat.application.frontoffice.booking.booker.BookingHistoryScheduleSnapshot
import com.beat.application.frontoffice.booking.booker.BookingHistorySnapshot
import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class GuestBookingAccessServiceSpec : FunSpec({
    test("게스트 인증과 예매 조회를 마치면 throttle을 초기화하고 session을 발급한다") {
        val dependencies = AccessDependencies()
        dependencies.stubAuthenticatedUser()
        every { dependencies.historyPort.findByUserId(USER_ID) } returns listOf(bookingReadModel())
        every { dependencies.sessionStore.issue(USER_ID) } returns SESSION_TOKEN

        val result = dependencies.service().authenticateAndFind(COMMAND, CLIENT_ADDRESS)

        result.bookings.single().bookingId shouldBe 1L
        result.sessionToken shouldBe SESSION_TOKEN
        verify { dependencies.throttle.reset(THROTTLE_KEY) }
    }

    test("일치하는 게스트 예매가 없으면 실패를 기록하고 조회와 session 발급을 수행하지 않는다") {
        val dependencies = AccessDependencies()
        every { dependencies.credentialAuthenticator.findUserId(any(), any(), any(), any()) } returns null

        val exception = shouldThrow<FrontofficeApplicationException> {
            dependencies.service().authenticateAndFind(COMMAND, CLIENT_ADDRESS)
        }

        exception.errorCode shouldBe BookingApplicationErrorCode.NO_BOOKING_FOUND
        verify { dependencies.throttle.recordFailure(THROTTLE_KEY) }
        verify(exactly = 0) { dependencies.historyPort.findByUserId(any()) }
        verify(exactly = 0) { dependencies.sessionStore.issue(any()) }
    }

    test("게스트 예매 조회 후 session 발급이 실패해도 조회 결과를 반환한다") {
        val dependencies = AccessDependencies()
        dependencies.stubAuthenticatedUser()
        every { dependencies.historyPort.findByUserId(USER_ID) } returns listOf(bookingReadModel())
        every { dependencies.sessionStore.issue(USER_ID) } throws IllegalStateException("redis unavailable")

        val result = dependencies.service().authenticateAndFind(COMMAND, CLIENT_ADDRESS)

        result.bookings.single().bookingId shouldBe 1L
        result.sessionToken shouldBe null
    }
})

private class AccessDependencies {
    val credentialAuthenticator = mockk<GuestBookingCredentialAuthenticator>(relaxed = true)
    val throttle = mockk<GuestAccessThrottle>(relaxed = true)
    val historyPort = mockk<BookingHistoryReadPort>(relaxed = true)
    val sessionStore = mockk<GuestSessionStore>(relaxed = true)

    fun service(): GuestBookingAccessService = GuestBookingAccessService(
        credentialAuthenticator,
        throttle,
        historyPort,
        GuestBookingSessionManager(sessionStore),
        FIXED_CLOCK,
    )

    fun stubAuthenticatedUser() {
        every { credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234") } returns USER_ID
    }
}

private fun bookingReadModel(): BookingHistorySnapshot = BookingHistorySnapshot(
    userId = USER_ID,
    bookingId = 1L,
    purchaseTicketCount = 1,
    bookerName = "booker",
    bookingStatus = "CHECKING_PAYMENT",
    createdAt = LocalDateTime.of(2026, 1, 1, 12, 0),
    totalPaymentAmount = 10_000,
    schedule = BookingHistoryScheduleSnapshot(
        scheduleId = 2L,
        performanceId = 3L,
        performanceDate = LocalDateTime.of(2026, 1, 10, 18, 0),
        scheduleNumber = "FIRST",
    ),
    performance = BookingHistoryPerformanceSnapshot(
        performanceId = 3L,
        performanceTitle = "공연",
        performanceVenue = "공연장",
        performanceContact = "010-0000-0000",
        bankName = "KAKAOBANK",
        accountNumber = "123-456",
        accountHolder = "예금주",
        posterImage = "poster.png",
        ticketPrice = 10_000,
    ),
)

private val COMMAND = GuestBookingAuthenticationCommand.of(
    bookerName = "booker",
    birthDate = "990101",
    bookerPhoneNumber = "010-0000-0000",
    password = "1234",
)
private const val USER_ID = 7L
private const val CLIENT_ADDRESS = "127.0.0.1"
private const val SESSION_TOKEN = "guest-session"
private const val THROTTLE_KEY = "$CLIENT_ADDRESS|booker|010-0000-0000|990101"
private val FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
