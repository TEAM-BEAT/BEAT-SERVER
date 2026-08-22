package com.beat.apis.ticket

import com.beat.apis.support.BeatTestContainersConfig
import com.beat.application.frontoffice.ticket.maker.command.TicketBookingStatus
import com.beat.application.frontoffice.ticket.maker.command.TicketCommandService
import com.beat.application.frontoffice.ticket.maker.command.TicketStatusUpdate
import com.beat.application.frontoffice.ticket.maker.command.TicketUpdateCommand
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
@Import(BeatTestContainersConfig::class)
@Tags("integration", "correctness")
open class TicketBulkLockOrderingIntegrationTest : FunSpec() {

    @Autowired
    private lateinit var ticketCommandService: TicketCommandService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    private lateinit var makerMember: Member
    private lateinit var performance: Performance
    private lateinit var firstBooking: Booking
    private lateinit var secondBooking: Booking

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            val maker = userRepository.save(Users.create())
            val makerUserId = requireNotNull(maker.id)
            makerMember = memberRepository.save(
                Member.create(
                    nickname = "ticket-lock-maker-$makerUserId",
                    email = "ticket-lock-maker-$makerUserId@example.com",
                    userId = makerUserId,
                    socialIdentity = SocialIdentity.of(SocialType.KAKAO, 8_000_000_000L + makerUserId),
                ),
            )
            performance = performanceRepository.save(
                Performance.create(
                    performanceTitle = "Ticket lock ordering performance $makerUserId",
                    genre = Genre.BAND,
                    runningTime = RunningTime.of(120),
                    performanceDescription = "description",
                    performanceAttentionNote = "attention",
                    paymentAccount = null,
                    posterImage = "poster.jpg",
                    performanceTeamName = "team",
                    performanceVenue = "venue",
                    roadAddressName = "road",
                    placeDetailAddress = "detail",
                    latitude = "37.0",
                    longitude = "127.0",
                    performanceContact = "010-0000-0000",
                    performancePeriod = PerformancePeriod.of(
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(2),
                    ),
                    ticketPrice = TicketPrice.of(10_000),
                    totalScheduleCount = 2,
                    userId = makerUserId,
                ),
            )
            val performanceId = requireNotNull(performance.id)
            val scheduleStart = LocalDateTime.now().plusDays(1)
            val firstSchedule = scheduleRepository.save(
                Schedule.create(
                    performanceDate = scheduleStart,
                    bookingCloseAt = scheduleStart.plusHours(2),
                    totalTicketCount = 10,
                    scheduleNumber = ScheduleNumber.FIRST,
                    performanceId = performanceId,
                ),
            )
            val secondSchedule = scheduleRepository.save(
                Schedule.create(
                    performanceDate = scheduleStart.plusHours(4),
                    bookingCloseAt = scheduleStart.plusHours(6),
                    totalTicketCount = 10,
                    scheduleNumber = ScheduleNumber.SECOND,
                    performanceId = performanceId,
                ),
            )
            val createdAt = LocalDateTime.now()
            firstBooking = bookingRepository.save(
                Booking.create(
                    purchaseTicketCount = 1,
                    bookerName = "first-booker",
                    bookerPhoneNumber = "010-0000-0001",
                    birthDate = null,
                    password = null,
                    scheduleId = requireNotNull(firstSchedule.id),
                    userId = makerUserId,
                    createdAt = createdAt,
                    totalPaymentAmount = 0,
                ),
            )
            secondBooking = bookingRepository.save(
                Booking.create(
                    purchaseTicketCount = 1,
                    bookerName = "second-booker",
                    bookerPhoneNumber = "010-0000-0002",
                    birthDate = null,
                    password = null,
                    scheduleId = requireNotNull(secondSchedule.id),
                    userId = makerUserId,
                    createdAt = createdAt,
                    totalPaymentAmount = 0,
                ),
            )
        }

        test("bulk ticket updates with opposite booking order complete without deadlock") {
            val memberId = requireNotNull(makerMember.id)
            val performanceId = requireNotNull(performance.id)
            val ready = CountDownLatch(2)
            val start = CountDownLatch(1)
            executor = Executors.newFixedThreadPool(2)
            val firstRequest = updateTask(
                ready = ready,
                start = start,
                memberId = memberId,
                performanceId = performanceId,
                bookingIds = listOf(requireNotNull(firstBooking.id), requireNotNull(secondBooking.id)),
            )
            val secondRequest = updateTask(
                ready = ready,
                start = start,
                memberId = memberId,
                performanceId = performanceId,
                bookingIds = listOf(requireNotNull(secondBooking.id), requireNotNull(firstBooking.id)),
            )

            try {
                ready.await(5, TimeUnit.SECONDS) shouldBe true
                start.countDown()

                firstRequest.get(10, TimeUnit.SECONDS) shouldBe true
                secondRequest.get(10, TimeUnit.SECONDS) shouldBe true
            } finally {
                executor.shutdownNow()
            }

            checkNotNull(bookingRepository.findById(checkNotNull(firstBooking.id))).bookingStatus shouldBe
                BookingStatus.BOOKING_CONFIRMED
            checkNotNull(bookingRepository.findById(checkNotNull(secondBooking.id))).bookingStatus shouldBe
                BookingStatus.BOOKING_CONFIRMED
        }
    }

    private fun updateTask(
        ready: CountDownLatch,
        start: CountDownLatch,
        memberId: Long,
        performanceId: Long,
        bookingIds: List<Long>,
    ) = executor.submit<Boolean> {
        ready.countDown()
        check(start.await(5, TimeUnit.SECONDS)) { "Concurrent ticket update did not start" }
        ticketCommandService.updateTickets(
            memberId = memberId,
            command = TicketUpdateCommand(
                performanceId = performanceId,
                bookingList = bookingIds.map { bookingId ->
                    TicketStatusUpdate(
                        bookingId = bookingId,
                        bookingStatus = TicketBookingStatus.BOOKING_CONFIRMED,
                    )
                },
            ),
        )
        true
    }

    private lateinit var executor: java.util.concurrent.ExecutorService
}
