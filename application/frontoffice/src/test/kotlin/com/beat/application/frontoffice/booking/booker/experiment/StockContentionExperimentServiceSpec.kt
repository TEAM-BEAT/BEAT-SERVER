package com.beat.application.frontoffice.booking.booker.experiment

import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.sharedkernel.vo.BankName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus

class StockContentionExperimentServiceSpec : FunSpec() {
    init {
        test("공통 validation은 performance 행 잠금 없이 비잠금 조회를 사용한다") {
            val strategyRegistry = mockk<StockContentionStrategyRegistry>()
            val memberRepository = mockk<MemberRepository>()
            val performanceRepository = mockk<PerformanceRepository>()
            val bookingRepository = mockk<BookingRepository>()
            val scheduleStore = mockk<StockContentionScheduleStore>()
            val transactionManager = mockk<PlatformTransactionManager>(relaxed = true)
            val transactionStatus = mockk<TransactionStatus>(relaxed = true)
            val clock =
                Clock.fixed(
                    Instant.parse("2026-08-23T00:00:00Z"),
                    ZoneId.of("Asia/Seoul"),
                )
            val member =
                Member.create(
                    nickname = "experiment-member",
                    email = "experiment-member@example.com",
                    userId = 30L,
                    socialIdentity = SocialIdentity.of(SocialType.KAKAO, 30L),
                )
            val performance =
                com.beat.domain.performance.model.Performance.create(
                    performanceTitle = "experiment-performance",
                    genre = com.beat.domain.performance.model.Genre.BAND,
                    runningTime = com.beat.domain.performance.vo.RunningTime.of(120),
                    performanceDescription = "description",
                    performanceAttentionNote = "attention",
                    paymentAccount = PaymentAccount.of(BankName.BUSAN, "1234-5678", "실험자"),
                    posterImage = "poster.jpg",
                    performanceTeamName = "team",
                    performanceVenue = "venue",
                    roadAddressName = "road",
                    placeDetailAddress = "detail",
                    latitude = "37.0",
                    longitude = "127.0",
                    performanceContact = "010-0000-0000",
                    performancePeriod =
                        PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
                    ticketPrice = com.beat.domain.performance.vo.TicketPrice.of(100),
                    totalScheduleCount = 1,
                    userId = 30L,
                )
            val scheduleState =
                ScheduleStockState(
                    id = 10L,
                    performanceId = 20L,
                    performanceDate = LocalDateTime.of(2026, 9, 1, 19, 0),
                    bookingCloseAt = LocalDateTime.of(2026, 9, 1, 18, 0),
                    totalTicketCount = 10,
                    soldTicketCount = 0,
                    scheduleNumber = "FIRST",
                    bookingOpen = true,
                    version = null,
                )
            val savedBooking =
                Booking.rehydrate(
                    id = 99L,
                    purchaseTicketCount = 1,
                    bookerName = "홍길동",
                    bookerPhoneNumber = "010-1234-5678",
                    bookingStatus = com.beat.domain.booking.model.BookingStatus.CHECKING_PAYMENT,
                    createdAt = LocalDateTime.of(2026, 8, 23, 9, 0),
                    cancellationDate = null,
                    birthDate = null,
                    password = null,
                    refundAccount = null,
                    scheduleId = 10L,
                    userId = 30L,
                    totalPaymentAmount = 100,
                )
            val reservationStrategy = AcceptingReservationStrategy()

            every { transactionManager.getTransaction(any()) } returns transactionStatus
            every { strategyRegistry.get(StockContentionStrategy.PESSIMISTIC) } returns
                reservationStrategy
            every { memberRepository.findById(1L) } returns member
            every { scheduleStore.findBookingMetadataById(10L) } returns
                ScheduleBookingMetadata(performanceId = 20L, bookingOpen = true)
            every { performanceRepository.findById(20L) } returns performance
            every { scheduleStore.find(10L, true, false) } returns scheduleState
            every { scheduleStore.reserveWithPessimisticLock(10L, 1) } returns 1
            every { bookingRepository.save(any()) } returns savedBooking

            val service =
                StockContentionExperimentService(
                    strategyRegistry = strategyRegistry,
                    memberRepository = memberRepository,
                    performanceRepository = performanceRepository,
                    bookingRepository = bookingRepository,
                    scheduleStore = scheduleStore,
                    transactionManager = transactionManager,
                    clock = clock,
                    properties = StockContentionExperimentProperties(),
                )

            service.createMemberBooking(
                memberId = 1L,
                strategy = StockContentionStrategy.PESSIMISTIC,
                command =
                    StockContentionBookingCommand(
                        scheduleId = 10L,
                        purchaseTicketCount = 1,
                        bookerName = "홍길동",
                        bookerPhoneNumber = "010-1234-5678",
                    ),
            ) shouldBe
                StockContentionExperimentResponse(
                    outcome = StockContentionOutcome.ACCEPTED,
                    bookingId = 99L,
                    attemptCount = 1,
                )

            verify(exactly = 1) { performanceRepository.findById(20L) }
            verify(exactly = 0) { performanceRepository.lockById(20L) }
        }

        test("공통 metadata가 닫힌 schedule이면 전략 실행 전에 BOOKING_CLOSED를 반환한다") {
            val strategyRegistry = mockk<StockContentionStrategyRegistry>()
            val memberRepository = mockk<MemberRepository>()
            val performanceRepository = mockk<PerformanceRepository>()
            val bookingRepository = mockk<BookingRepository>()
            val scheduleStore = mockk<StockContentionScheduleStore>()
            val transactionManager = mockk<PlatformTransactionManager>(relaxed = true)
            val transactionStatus = mockk<TransactionStatus>(relaxed = true)
            val member = mockk<Member>(relaxed = true)
            val reservationStrategy = AcceptingReservationStrategy()
            val clock =
                Clock.fixed(
                    Instant.parse("2026-08-23T00:00:00Z"),
                    ZoneId.of("Asia/Seoul"),
                )

            every { transactionManager.getTransaction(any()) } returns transactionStatus
            every { strategyRegistry.get(StockContentionStrategy.PESSIMISTIC) } returns
                reservationStrategy
            every { memberRepository.findById(1L) } returns member
            every { scheduleStore.findBookingMetadataById(10L) } returns
                ScheduleBookingMetadata(performanceId = 20L, bookingOpen = false)

            val service =
                StockContentionExperimentService(
                    strategyRegistry = strategyRegistry,
                    memberRepository = memberRepository,
                    performanceRepository = performanceRepository,
                    bookingRepository = bookingRepository,
                    scheduleStore = scheduleStore,
                    transactionManager = transactionManager,
                    clock = clock,
                    properties = StockContentionExperimentProperties(),
                )

            val exception =
                shouldThrow<FrontofficeApplicationException> {
                    service.createMemberBooking(
                        memberId = 1L,
                        strategy = StockContentionStrategy.PESSIMISTIC,
                        command =
                            StockContentionBookingCommand(
                                scheduleId = 10L,
                                purchaseTicketCount = 1,
                                bookerName = "홍길동",
                                bookerPhoneNumber = "010-1234-5678",
                            ),
                    )
                }

            exception.errorCode shouldBe BookingApplicationErrorCode.BOOKING_CLOSED
            verify(exactly = 0) { performanceRepository.findById(any()) }
            verify(exactly = 0) { scheduleStore.find(any(), any(), any()) }
        }
    }
}

private class AcceptingReservationStrategy : StockContentionReservationStrategy {
    override val strategy: StockContentionStrategy = StockContentionStrategy.PESSIMISTIC

    override fun reserve(request: StockReservationRequest): StockReservationDecision =
        StockReservationDecision(StockContentionOutcome.ACCEPTED)
}
