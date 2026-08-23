package com.beat.infra.persistence.ticket.repository.query

import com.beat.application.frontoffice.ticket.maker.query.MakerTicketBookingStatus
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketReader
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import com.beat.infra.config.JpaConfig
import com.beat.infra.support.MySqlTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(
    properties = [
        "spring.config.import=classpath:application-persistence.yml",
        "DB_HIKARI_MAX_POOL_SIZE=10",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [JpaConfig::class, MySqlTestContainerConfig::class])
@ActiveProfiles("test")
@Tags("integration")
class MakerTicketQueriesIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var makerTicketReader: MakerTicketReader

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("maker ticket reader는 MySQL에서 상태를 정렬하고 은행 표시명을 매핑한다") {
            val userId = requireNotNull(userRepository.save(Users.create()).id)
            val performanceId = requireNotNull(
                performanceRepository.save(
                    Performance.create(
                        performanceTitle = "JDSL ticket ordering performance",
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
                            LocalDate.of(2026, 8, 25),
                            LocalDate.of(2026, 8, 25),
                        ),
                        ticketPrice = TicketPrice.of(10_000),
                        totalScheduleCount = 1,
                        userId = userId,
                    ),
                ).id,
            )
            val scheduleId = requireNotNull(
                scheduleRepository.save(
                    Schedule.create(
                        performanceDate = LocalDateTime.of(2026, 8, 25, 19, 0),
                        bookingCloseAt = LocalDateTime.of(2026, 8, 25, 20, 0),
                        totalTicketCount = 10,
                        scheduleNumber = ScheduleNumber.FIRST,
                        performanceId = performanceId,
                    ),
                ).id,
            )
            val refundCreatedAt = LocalDateTime.of(2026, 8, 21, 10, 0)
            val checkingOlderCreatedAt = LocalDateTime.of(2026, 8, 21, 11, 0)
            val checkingNewerCreatedAt = LocalDateTime.of(2026, 8, 21, 12, 0)

            bookingRepository.save(
                Booking.rehydrate(
                    id = null,
                    purchaseTicketCount = 1,
                    bookerName = "refund-booker",
                    bookerPhoneNumber = "010-0000-0001",
                    bookingStatus = BookingStatus.REFUND_REQUESTED,
                    createdAt = refundCreatedAt,
                    cancellationDate = null,
                    birthDate = null,
                    password = null,
                    refundAccount = RefundAccount.of(BankName.KAKAOBANK, "123-456", "refund-holder"),
                    scheduleId = scheduleId,
                    userId = userId,
                    totalPaymentAmount = 10_000,
                ),
            )
            bookingRepository.save(
                Booking.rehydrate(
                    id = null,
                    purchaseTicketCount = 1,
                    bookerName = "checking-older-booker",
                    bookerPhoneNumber = "010-0000-0002",
                    bookingStatus = BookingStatus.CHECKING_PAYMENT,
                    createdAt = checkingOlderCreatedAt,
                    cancellationDate = null,
                    birthDate = null,
                    password = null,
                    refundAccount = null,
                    scheduleId = scheduleId,
                    userId = userId,
                    totalPaymentAmount = 10_000,
                ),
            )
            bookingRepository.save(
                Booking.rehydrate(
                    id = null,
                    purchaseTicketCount = 1,
                    bookerName = "checking-newer-booker",
                    bookerPhoneNumber = "010-0000-0003",
                    bookingStatus = BookingStatus.CHECKING_PAYMENT,
                    createdAt = checkingNewerCreatedAt,
                    cancellationDate = null,
                    birthDate = null,
                    password = null,
                    refundAccount = null,
                    scheduleId = scheduleId,
                    userId = userId,
                    totalPaymentAmount = 10_000,
                ),
            )

            val result = makerTicketReader.findTickets(performanceId, emptyList(), emptyList())

            result shouldHaveSize 3
            result.map { it.bookingStatus } shouldBe listOf(
                MakerTicketBookingStatus.REFUND_REQUESTED,
                MakerTicketBookingStatus.CHECKING_PAYMENT,
                MakerTicketBookingStatus.CHECKING_PAYMENT,
            )
            result.map { it.createdAt } shouldBe listOf(
                refundCreatedAt,
                checkingNewerCreatedAt,
                checkingOlderCreatedAt,
            )
            result.first().bankName shouldBe BankName.KAKAOBANK.displayName
            result.first().bankName shouldNotBe BankName.KAKAOBANK.name
        }

        test("maker ticket reader는 MySQL에서 cross join 동적 query를 생성하고 실행한다") {
            makerTicketReader.findTickets(999_999L, emptyList(), emptyList()).shouldBeEmpty()
        }
    }
}
