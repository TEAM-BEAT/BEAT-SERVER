package com.beat.apps.api.ticket

import com.beat.apps.api.fixture.performanceFixture
import com.beat.apps.api.fixture.scheduleFixture
import com.beat.apps.api.fixture.usersFixture
import com.beat.apps.api.support.BeatTestContainersConfig
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
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
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

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("예매 순서가 반대인 bulk ticket 갱신은 deadlock 없이 완료된다") {
            val fixture = createFixture()
            val memberId = requireNotNull(fixture.makerMember.id)
            val performanceId = requireNotNull(fixture.performance.id)
            val firstBookingId = requireNotNull(fixture.firstBooking.id)
            val secondBookingId = requireNotNull(fixture.secondBooking.id)
            val ready = CountDownLatch(2)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val firstRequest = updateTask(
                    executor = executor,
                    ready = ready,
                    start = start,
                    memberId = memberId,
                    performanceId = performanceId,
                    bookingIds = listOf(firstBookingId, secondBookingId),
                )
                val secondRequest = updateTask(
                    executor = executor,
                    ready = ready,
                    start = start,
                    memberId = memberId,
                    performanceId = performanceId,
                    bookingIds = listOf(secondBookingId, firstBookingId),
                )
                ready.await(5, TimeUnit.SECONDS) shouldBe true
                start.countDown()

                firstRequest.get(10, TimeUnit.SECONDS) shouldBe true
                secondRequest.get(10, TimeUnit.SECONDS) shouldBe true
            } finally {
                executor.shutdownNow()
            }

            checkNotNull(bookingRepository.findById(firstBookingId)).bookingStatus shouldBe
                BookingStatus.BOOKING_CONFIRMED
            checkNotNull(bookingRepository.findById(secondBookingId)).bookingStatus shouldBe
                BookingStatus.BOOKING_CONFIRMED
        }
    }

    private fun createFixture(): Fixture {
        val maker = userRepository.save(usersFixture())
        val makerUserId = requireNotNull(maker.id)
        val makerMember = memberRepository.save(
            Member.create(
                nickname = "ticket-lock-maker-$makerUserId",
                email = "ticket-lock-maker-$makerUserId@example.com",
                userId = makerUserId,
                socialIdentity = SocialIdentity.of(SocialType.KAKAO, 8_000_000_000L + makerUserId),
            ),
        )
        val performance = performanceRepository.save(
            performanceFixture(
                performanceTitle = "Ticket lock ordering performance $makerUserId",
                genre = Genre.BAND,
                paymentAccount = null,
                posterImage = "poster.jpg",
                performancePeriod = PerformancePeriod.of(
                    NOW.toLocalDate(),
                    NOW.toLocalDate().plusDays(1),
                ),
                ticketPrice = 10_000,
                totalScheduleCount = 2,
                userId = makerUserId,
            ),
        )
        val performanceId = requireNotNull(performance.id)
        val scheduleStart = NOW.plusDays(1)
        val firstSchedule = scheduleRepository.save(
            scheduleFixture(
                performanceDate = scheduleStart,
                bookingCloseAt = scheduleStart.plusHours(2),
                totalTicketCount = 10,
                scheduleNumber = ScheduleNumber.FIRST,
                performanceId = performanceId,
            ),
        )
        val secondSchedule = scheduleRepository.save(
            scheduleFixture(
                performanceDate = scheduleStart.plusHours(4),
                bookingCloseAt = scheduleStart.plusHours(6),
                totalTicketCount = 10,
                scheduleNumber = ScheduleNumber.SECOND,
                performanceId = performanceId,
            ),
        )
        val createdAt = NOW
        val firstBooking = bookingRepository.save(
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
        val secondBooking = bookingRepository.save(
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
        return Fixture(makerMember, performance, firstBooking, secondBooking)
    }

    private fun updateTask(
        executor: ExecutorService,
        ready: CountDownLatch,
        start: CountDownLatch,
        memberId: Long,
        performanceId: Long,
        bookingIds: List<Long>,
    ): Future<Boolean> = executor.submit<Boolean> {
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

    private data class Fixture(
        val makerMember: Member,
        val performance: Performance,
        val firstBooking: Booking,
        val secondBooking: Booking,
    )
}

private val NOW: LocalDateTime = LocalDateTime.now()
