package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.fixture.frontofficeMemberFixture
import com.beat.application.frontoffice.fixture.frontofficePerformanceFixture
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.springframework.context.ApplicationEventPublisher

class BookingCreationFlowSpec :
    FunSpec({
        isolationMode = IsolationMode.SingleInstance

        test("게스트 예매는 Performance와 Schedule을 authoritative lock 순서로 확인하고 snapshot을 저장한다") {
            val dependencies = CreationDependencies()
            val schedule = schedule()
            val savedBookingSlot = slot<Booking>()
            every { dependencies.bookingRepository.save(capture(savedBookingSlot)) } answers
                {
                    savedBookingSlot.captured
                }
            stubGuestCreation(dependencies, schedule)
            every { dependencies.guestSessionStore.issue(30L) } returns "guest-session"

            val result = dependencies.guestService().createGuestBooking(guestCommand())

            verifyOrder {
                dependencies.scheduleRepository.findPerformanceIdById(1L)
                dependencies.performanceRepository.lockById(20L)
                dependencies.scheduleRepository.lockById(1L)
            }
            val savedBooking = savedBookingSlot.captured
            savedBooking.bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
            savedBooking.password shouldBe "encoded-password"
            result.booking.totalPaymentAmount shouldBe 10_000
            result.sessionToken shouldBe "guest-session"
        }

        test("게스트 session 발급이 실패해도 생성된 예매 결과를 반환한다") {
            val dependencies = CreationDependencies()
            val schedule = schedule()
            stubGuestCreation(dependencies, schedule)
            every { dependencies.guestSessionStore.issue(30L) } throws
                IllegalStateException("redis unavailable")

            val result = dependencies.guestService().createGuestBooking(guestCommand())

            result.booking.userId shouldBe 30L
            result.sessionToken shouldBe null
        }

        test("회원 예매는 회원의 authoritative user identity와 Performance 가격 snapshot을 저장한다") {
            val dependencies = CreationDependencies()
            val schedule = schedule()
            val savedBookingSlot = slot<Booking>()
            every { dependencies.bookingRepository.save(capture(savedBookingSlot)) } answers
                {
                    savedBookingSlot.captured
                }
            every { dependencies.memberRepository.findById(10L) } returns member()
            every { dependencies.scheduleRepository.findPerformanceIdById(1L) } returns 20L
            every { dependencies.performanceRepository.lockById(20L) } returns
                dependencies.performance
            every { dependencies.scheduleRepository.lockById(1L) } returns schedule
            every { dependencies.scheduleRepository.isBeforeBookingCloseAt(1L) } returns true
            val result = dependencies.memberService().createMemberBooking(10L, memberCommand())

            verifyOrder {
                dependencies.scheduleRepository.findPerformanceIdById(1L)
                dependencies.performanceRepository.lockById(20L)
                dependencies.scheduleRepository.lockById(1L)
            }
            val savedBooking = savedBookingSlot.captured
            savedBooking.bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
            savedBooking.userId shouldBe 30L
            result.userId shouldBe 30L
            result.totalPaymentAmount shouldBe 10_000
        }

        test("회원 예매는 database 기준 마감 이후 생성할 수 없다") {
            val dependencies = CreationDependencies()
            every { dependencies.memberRepository.findById(10L) } returns member()
            every { dependencies.scheduleRepository.findPerformanceIdById(1L) } returns 20L
            every { dependencies.performanceRepository.lockById(20L) } returns
                dependencies.performance
            every { dependencies.scheduleRepository.lockById(1L) } returns schedule()
            every { dependencies.scheduleRepository.isBeforeBookingCloseAt(1L) } returns false

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    dependencies.memberService().createMemberBooking(10L, memberCommand())
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.BOOKING_CLOSED
        }

        test("회원 예매는 회차가 없을 때 안정적인 회차 없음 error를 반환한다") {
            val dependencies = CreationDependencies()
            every { dependencies.memberRepository.findById(10L) } returns member()
            every { dependencies.scheduleRepository.findPerformanceIdById(1L) } returns null

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    dependencies.memberService().createMemberBooking(10L, memberCommand())
                }

            exception.errorCode.code shouldBe "SCHEDULE_NOT_FOUND"
            exception.errorCode.message shouldBe "해당 회차를 찾을 수 없습니다."
        }

        test("회원 예매는 공연이 없을 때 안정적인 공연 없음 error를 반환한다") {
            val dependencies = CreationDependencies()
            every { dependencies.memberRepository.findById(10L) } returns member()
            every { dependencies.scheduleRepository.findPerformanceIdById(1L) } returns 20L
            every { dependencies.performanceRepository.lockById(20L) } returns null

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    dependencies.memberService().createMemberBooking(10L, memberCommand())
                }

            exception.errorCode.code shouldBe "PERFORMANCE_NOT_FOUND"
            exception.errorCode.message shouldBe "해당 공연 정보를 찾을 수 없습니다."
        }

        test("게스트 예매는 database 기준 마감 이후 생성할 수 없다") {
            val dependencies = CreationDependencies()
            every { dependencies.scheduleRepository.findPerformanceIdById(1L) } returns 20L
            every { dependencies.performanceRepository.lockById(20L) } returns
                dependencies.performance
            every { dependencies.scheduleRepository.lockById(1L) } returns schedule()
            every { dependencies.scheduleRepository.isBeforeBookingCloseAt(1L) } returns false
            every {
                dependencies.credentialAuthenticator.findUserId(
                    "booker",
                    "010-0000-0000",
                    "990101",
                    "1234",
                )
            } returns null

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    dependencies.guestService().createGuestBooking(guestCommand())
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.BOOKING_CLOSED
        }

        test("회원 예매는 lock한 회차의 Performance 관계가 바뀌었으면 생성하지 않는다") {
            val dependencies = CreationDependencies()
            val changedSchedule = schedule(performanceId = 21L)
            every { dependencies.memberRepository.findById(10L) } returns member()
            every { dependencies.scheduleRepository.findPerformanceIdById(1L) } returns 20L
            every { dependencies.performanceRepository.lockById(20L) } returns
                dependencies.performance
            every { dependencies.scheduleRepository.lockById(1L) } returns changedSchedule

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    dependencies.memberService().createMemberBooking(10L, memberCommand())
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
            verify(exactly = 0) { dependencies.scheduleRepository.isBeforeBookingCloseAt(any()) }
        }
    })

private class CreationDependencies {
    val scheduleRepository = scheduleRepositoryWithSavePassthrough()
    val bookingRepository = bookingRepositoryWithSavePassthrough()
    val userRepository = userRepositoryWithDeterministicSave()
    val performanceRepository = mockk<PerformanceRepository>(relaxed = true)
    val performance =
        frontofficePerformanceFixture(
            id = 20L,
            performanceTitle = "Performance Title",
            ticketPrice = 10_000,
            paymentAccount = PaymentAccount.of(BankName.BUSAN, "123-456", "holder"),
        )
    val memberRepository = mockk<MemberRepository>(relaxed = true)
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    val credentialAuthenticator = mockk<GuestBookingCredentialAuthenticator>(relaxed = true)
    val guestSessionStore = mockk<GuestSessionStore>(relaxed = true)

    fun guestService(): GuestBookingCommandService =
        GuestBookingCommandService(
            scheduleRepository,
            bookingRepository,
            userRepository,
            performanceRepository,
            eventPublisher,
            credentialAuthenticator,
            GuestBookingSessionManager(guestSessionStore),
            FIXED_CLOCK,
        )

    fun memberService(): MemberBookingCommandService =
        MemberBookingCommandService(
            scheduleRepository,
            bookingRepository,
            memberRepository,
            performanceRepository,
            eventPublisher,
            FIXED_CLOCK,
        )
}

private fun stubGuestCreation(
    dependencies: CreationDependencies,
    schedule: Schedule,
) {
    every { dependencies.scheduleRepository.findPerformanceIdById(1L) } returns 20L
    every { dependencies.performanceRepository.lockById(20L) } returns dependencies.performance
    every { dependencies.scheduleRepository.lockById(1L) } returns schedule
    every { dependencies.scheduleRepository.isBeforeBookingCloseAt(1L) } returns true
    every {
        dependencies.credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234")
    } returns null
    every { dependencies.credentialAuthenticator.encode("1234") } returns "encoded-password"
}

private fun scheduleRepositoryWithSavePassthrough(): ScheduleRepository =
    mockk(relaxed = true) {
        every { save(any()) } answers { firstArg() }
    }

private fun bookingRepositoryWithSavePassthrough(): BookingRepository =
    mockk(relaxed = true) {
        every { save(any()) } answers { firstArg() }
    }

private fun userRepositoryWithDeterministicSave(): UserRepository =
    mockk(relaxed = true) {
        every { save(any()) } returns Users.rehydrate(30L, Role.USER)
    }

private fun member(): Member =
    frontofficeMemberFixture(
        id = 10L,
        nickname = "nickname",
        email = "email@test.com",
        userId = 30L,
        socialId = 123L,
    )

private fun memberCommand(): MemberBookingCommand =
    MemberBookingCommand.of(
        1L,
        1,
        "booker",
        "010-0000-0000",
    )

private fun guestCommand(): GuestBookingCommand =
    GuestBookingCommand.of(
        1L,
        1,
        "booker",
        "010-0000-0000",
        "990101",
        "1234",
    )

private fun schedule(performanceId: Long = 20L): Schedule {
    val performanceDate = LocalDateTime.of(2026, 2, 1, 18, 0)
    return Schedule.rehydrate(
        1L,
        performanceDate,
        performanceDate.plusHours(2),
        10,
        0,
        ScheduleNumber.FIRST,
        performanceId,
    )
}

private val FIXED_CLOCK: Clock =
    Clock.fixed(
        Instant.parse("2026-01-01T00:00:00Z"),
        ZoneOffset.UTC,
    )
