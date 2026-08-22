package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.exception.DomainException
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.TicketPrice
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
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class BookingCreationFlowSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    test("게스트 예매는 Performance와 Schedule을 authoritative lock 순서로 확인하고 snapshot을 저장한다") {
        val dependencies = CreationDependencies()
        val schedule = schedule()
        stubPerformance(dependencies.performance)
        stubGuestCreation(dependencies, schedule)

        val result = dependencies.guestService().createGuestBooking(guestCommand())

        val order = Mockito.inOrder(dependencies.scheduleRepository, dependencies.performanceRepository)
        order.verify(dependencies.scheduleRepository).findPerformanceIdById(1L)
        order.verify(dependencies.performanceRepository).lockById(20L)
        order.verify(dependencies.scheduleRepository).lockById(1L)
        val savedBooking = savedArgument<Booking>(dependencies.bookingRepository, "save")
        savedBooking.bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
        savedBooking.password shouldBe "encoded-password"
        result.totalPaymentAmount shouldBe 10_000
    }

    test("회원 예매는 회원의 authoritative user identity와 Performance 가격 snapshot을 저장한다") {
        val dependencies = CreationDependencies()
        val schedule = schedule()
        stubPerformance(dependencies.performance)
        Mockito.`when`(dependencies.memberRepository.findById(10L)).thenReturn(member())
        Mockito.`when`(dependencies.scheduleRepository.findPerformanceIdById(1L)).thenReturn(20L)
        Mockito.`when`(dependencies.performanceRepository.lockById(20L)).thenReturn(dependencies.performance)
        Mockito.`when`(dependencies.scheduleRepository.lockById(1L)).thenReturn(schedule)
        Mockito.`when`(dependencies.scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(true)
        val result = dependencies.memberService().createMemberBooking(10L, memberCommand())

        val order = Mockito.inOrder(dependencies.scheduleRepository, dependencies.performanceRepository)
        order.verify(dependencies.scheduleRepository).findPerformanceIdById(1L)
        order.verify(dependencies.performanceRepository).lockById(20L)
        order.verify(dependencies.scheduleRepository).lockById(1L)
        val savedBooking = savedArgument<Booking>(dependencies.bookingRepository, "save")
        savedBooking.bookingStatus shouldBe BookingStatus.CHECKING_PAYMENT
        savedBooking.userId shouldBe 30L
        result.userId shouldBe 30L
        result.totalPaymentAmount shouldBe 10_000
    }

    test("회원 예매는 database 기준 마감 이후 생성할 수 없다") {
        val dependencies = CreationDependencies()
        Mockito.`when`(dependencies.memberRepository.findById(10L)).thenReturn(member())
        Mockito.`when`(dependencies.scheduleRepository.findPerformanceIdById(1L)).thenReturn(20L)
        Mockito.`when`(dependencies.performanceRepository.lockById(20L)).thenReturn(dependencies.performance)
        Mockito.`when`(dependencies.scheduleRepository.lockById(1L)).thenReturn(schedule())
        Mockito.`when`(dependencies.scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(false)

        val exception = shouldThrow<FrontofficeApplicationException> {
            dependencies.memberService().createMemberBooking(10L, memberCommand())
        }

        exception.errorCode shouldBe BookingApplicationErrorCode.BOOKING_CLOSED
    }

    test("회원 예매는 회차가 없을 때 안정적인 회차 없음 error를 반환한다") {
        val dependencies = CreationDependencies()
        Mockito.`when`(dependencies.memberRepository.findById(10L)).thenReturn(member())
        Mockito.`when`(dependencies.scheduleRepository.findPerformanceIdById(1L)).thenReturn(null)

        val exception = shouldThrow<FrontofficeApplicationException> {
            dependencies.memberService().createMemberBooking(10L, memberCommand())
        }

        exception.errorCode.code shouldBe "SCHEDULE_NOT_FOUND"
        exception.errorCode.message shouldBe "해당 회차를 찾을 수 없습니다."
    }

    test("회원 예매는 공연이 없을 때 안정적인 공연 없음 error를 반환한다") {
        val dependencies = CreationDependencies()
        Mockito.`when`(dependencies.memberRepository.findById(10L)).thenReturn(member())
        Mockito.`when`(dependencies.scheduleRepository.findPerformanceIdById(1L)).thenReturn(20L)
        Mockito.`when`(dependencies.performanceRepository.lockById(20L)).thenReturn(null)

        val exception = shouldThrow<FrontofficeApplicationException> {
            dependencies.memberService().createMemberBooking(10L, memberCommand())
        }

        exception.errorCode.code shouldBe "PERFORMANCE_NOT_FOUND"
        exception.errorCode.message shouldBe "해당 공연 정보를 찾을 수 없습니다."
    }

    test("게스트 예매는 database 기준 마감 이후 생성할 수 없다") {
        val dependencies = CreationDependencies()
        Mockito.`when`(dependencies.scheduleRepository.findPerformanceIdById(1L)).thenReturn(20L)
        Mockito.`when`(dependencies.performanceRepository.lockById(20L)).thenReturn(dependencies.performance)
        Mockito.`when`(dependencies.scheduleRepository.lockById(1L)).thenReturn(schedule())
        Mockito.`when`(dependencies.scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(false)
        Mockito.`when`(dependencies.credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234"))
            .thenReturn(null)

        val exception = shouldThrow<FrontofficeApplicationException> {
            dependencies.guestService().createGuestBooking(guestCommand())
        }

        exception.errorCode shouldBe BookingApplicationErrorCode.BOOKING_CLOSED
    }

    test("회원 예매는 lock한 회차의 Performance 관계가 바뀌었으면 생성하지 않는다") {
        val dependencies = CreationDependencies()
        val changedSchedule = schedule(performanceId = 21L)
        Mockito.`when`(dependencies.memberRepository.findById(10L)).thenReturn(member())
        Mockito.`when`(dependencies.scheduleRepository.findPerformanceIdById(1L)).thenReturn(20L)
        Mockito.`when`(dependencies.performanceRepository.lockById(20L)).thenReturn(dependencies.performance)
        Mockito.`when`(dependencies.scheduleRepository.lockById(1L)).thenReturn(changedSchedule)

        val exception = shouldThrow<FrontofficeApplicationException> {
            dependencies.memberService().createMemberBooking(10L, memberCommand())
        }

        exception.errorCode shouldBe BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
        verifyNoInvocation(dependencies.scheduleRepository, "isBeforeBookingCloseAt")
    }
})

private class CreationDependencies {
    val scheduleRepository = mockWithSavePassthrough(ScheduleRepository::class.java)
    val bookingRepository = mockWithSavePassthrough(BookingRepository::class.java)
    val userRepository = mockWithDeterministicUserSave()
    val performanceRepository = Mockito.mock(PerformanceRepository::class.java)
    val performance = Mockito.mock(Performance::class.java)
    val memberRepository = Mockito.mock(MemberRepository::class.java)
    val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    val credentialAuthenticator = Mockito.mock(GuestBookingCredentialAuthenticator::class.java)

    fun guestService(): GuestBookingCommandService = GuestBookingCommandService(
        scheduleRepository,
        bookingRepository,
        userRepository,
        performanceRepository,
        eventPublisher,
        credentialAuthenticator,
        FIXED_CLOCK,
    )

    fun memberService(): MemberBookingCommandService = MemberBookingCommandService(
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
    Mockito.`when`(dependencies.scheduleRepository.findPerformanceIdById(1L)).thenReturn(20L)
    Mockito.`when`(dependencies.performanceRepository.lockById(20L)).thenReturn(dependencies.performance)
    Mockito.`when`(dependencies.scheduleRepository.lockById(1L)).thenReturn(schedule)
    Mockito.`when`(dependencies.scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(true)
    Mockito.`when`(dependencies.credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234"))
        .thenReturn(null)
    Mockito.`when`(dependencies.credentialAuthenticator.encode("1234")).thenReturn("encoded-password")
}

private fun <T> mockWithSavePassthrough(type: Class<T>): T = Mockito.mock(
    type,
    Answer<Any?> { invocation ->
        if (invocation.method.name == "save") {
            invocation.arguments[0]
        } else {
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    },
)

private fun mockWithDeterministicUserSave(): UserRepository = Mockito.mock(
    UserRepository::class.java,
    Answer<Any?> { invocation ->
        if (invocation.method.name == "save") {
            Users.rehydrate(30L, Role.USER)
        } else {
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    },
)

private fun verifyNoInvocation(mock: Any, methodName: String) {
    Mockito.mockingDetails(mock).invocations.any { it.method.name == methodName } shouldBe false
}

private inline fun <reified T> savedArgument(mock: Any, methodName: String): T =
    Mockito.mockingDetails(mock).invocations
        .single { it.method.name == methodName }
        .arguments[0] as T

private fun stubPerformance(performance: Performance) {
    Mockito.`when`(performance.ticketPriceValue).thenReturn(TicketPrice.of(10_000))
    Mockito.`when`(performance.performanceTitle).thenReturn("Performance Title")
    Mockito.`when`(performance.paymentAccount)
        .thenReturn(PaymentAccount.of(BankName.BUSAN, "123-456", "holder"))
}

private fun member(): Member = Member.rehydrate(
    10L,
    "nickname",
    "email@test.com",
    null,
    30L,
    SocialIdentity.of(SocialType.KAKAO, 123L),
)

private fun memberCommand(): MemberBookingCommand = MemberBookingCommand.of(
    1L,
    1,
    "booker",
    "010-0000-0000",
)

private fun guestCommand(): GuestBookingCommand = GuestBookingCommand.of(
    1L,
    1,
    "booker",
    "010-0000-0000",
    "990101",
    "1234",
)

private fun schedule(
    performanceId: Long = 20L,
): Schedule {
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

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2026-01-01T00:00:00Z"),
    ZoneOffset.UTC,
)
