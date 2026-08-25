package com.beat.apps.api.performance

import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommandService
import com.beat.application.frontoffice.booking.booker.result.GuestBookingCreationOutcome
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.performance.maker.command.PerformanceBankName
import com.beat.application.frontoffice.performance.maker.command.PerformanceGenre
import com.beat.application.frontoffice.performance.maker.command.PerformanceImageStorage
import com.beat.application.frontoffice.performance.maker.command.PerformanceModifyCommand
import com.beat.application.frontoffice.performance.maker.command.PerformanceModifyCommandService
import com.beat.application.frontoffice.performance.maker.command.PerformancePresignedUrls
import com.beat.application.frontoffice.performance.maker.command.ScheduleModifyCommand
import com.beat.apps.api.fixture.performanceFixture
import com.beat.apps.api.fixture.scheduleFixture
import com.beat.apps.api.fixture.usersFixture
import com.beat.apps.api.support.BeatTestContainersConfig
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.user.repository.UserRepository
import io.kotest.core.NamedTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
@ActiveProfiles("test")
@Import(BeatTestContainersConfig::class, PerformanceBookingPriceConcurrencySpec.TestConfig::class)
@Tags("correctness")
open class PerformanceBookingPriceConcurrencySpec : FunSpec() {

    private val NOW: LocalDateTime = LocalDateTime.now()

    @Autowired private lateinit var guestBookingCommandService: GuestBookingCommandService

    @Autowired private lateinit var performanceModifyCommandService: PerformanceModifyCommandService

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var performanceRepository: PerformanceRepository

    @Autowired private lateinit var scheduleRepository: ScheduleRepository

    @Autowired private lateinit var bookingRepository: BookingRepository

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        context("가격 변경 transaction이 Performance lock을 먼저 획득하면").config(tags = setOf(CORRECTNESS)) {
            test("예매는 변경된 authoritative 가격을 snapshot 한다") {
                val fixture = createFixture()
                val modifierHasLock = CountDownLatch(1)
                val bookingStarted = CountDownLatch(1)
                val executor = Executors.newFixedThreadPool(2)

                try {
                    val modifyFuture = executor.submit {
                        TransactionTemplate(transactionManager).execute {
                            checkNotNull(performanceRepository.lockById(fixture.performanceId))
                            modifierHasLock.countDown()
                            check(bookingStarted.await(5, TimeUnit.SECONDS))
                            performanceModifyCommandService.modifyPerformance(
                                fixture.memberId,
                                modifyCommand(fixture, NEW_TICKET_PRICE),
                            )
                        }
                    }
                    check(modifierHasLock.await(5, TimeUnit.SECONDS))
                    val bookingFuture =
                        executor.submit<GuestBookingCreationOutcome> {
                            bookingStarted.countDown()
                            guestBookingCommandService.createGuestBooking(
                                guestBookingCommand(fixture)
                            )
                        }

                    modifyFuture.get(10, TimeUnit.SECONDS)
                    val bookingResult = bookingFuture.get(10, TimeUnit.SECONDS)

                    bookingResult.booking.totalPaymentAmount shouldBe NEW_TICKET_PRICE
                    assertPersistedSnapshot(fixture, NEW_TICKET_PRICE)
                } finally {
                    executor.shutdownNow()
                }
            }
        }

        context("예매 transaction이 Performance lock을 먼저 획득하면").config(tags = setOf(CORRECTNESS)) {
            test("기존 가격을 snapshot 하고 뒤이은 가격 변경은 거부한다") {
                val fixture = createFixture()
                val bookingHasLock = CountDownLatch(1)
                val modifierStarted = CountDownLatch(1)
                val executor = Executors.newFixedThreadPool(2)

                try {
                    val bookingFuture =
                        executor.submit<GuestBookingCreationOutcome?> {
                            TransactionTemplate(transactionManager).execute {
                                checkNotNull(performanceRepository.lockById(fixture.performanceId))
                                bookingHasLock.countDown()
                                check(modifierStarted.await(5, TimeUnit.SECONDS))
                                guestBookingCommandService.createGuestBooking(
                                    guestBookingCommand(fixture)
                                )
                            }
                        }
                    check(bookingHasLock.await(5, TimeUnit.SECONDS))
                    val modifyFuture =
                        executor.submit<Throwable?> {
                            modifierStarted.countDown()
                            try {
                                performanceModifyCommandService.modifyPerformance(
                                    fixture.memberId,
                                    modifyCommand(fixture, NEW_TICKET_PRICE),
                                )
                                null
                            } catch (failure: Throwable) {
                                failure
                            }
                        }

                    val bookingResult = bookingFuture.get(10, TimeUnit.SECONDS)
                    val modifyFailure = modifyFuture.get(10, TimeUnit.SECONDS)

                    bookingResult!!.booking.totalPaymentAmount shouldBe OLD_TICKET_PRICE
                    modifyFailure.shouldBeInstanceOf<FrontofficeApplicationException>()
                    modifyFailure.errorCode.code shouldBe
                        PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED.code
                    modifyFailure.errorCode.type shouldBe
                        FrontofficeApplicationErrorType.INVALID_INPUT
                    assertPersistedSnapshot(fixture, OLD_TICKET_PRICE)
                } finally {
                    executor.shutdownNow()
                }
            }
        }
    }

    private fun createFixture(): Fixture {
        val maker = userRepository.save(usersFixture())
        val makerUserId = requireNotNull(maker.id)
        val member =
            memberRepository.save(
                Member.create(
                    nickname = "price-lock-maker-$makerUserId",
                    email = "price-lock-maker-$makerUserId@example.com",
                    userId = makerUserId,
                    socialIdentity =
                        SocialIdentity.of(SocialType.KAKAO, 7_000_000_000L + makerUserId),
                )
            )
        val performance =
            performanceRepository.save(
                performanceFixture(
                    performanceTitle = "Price lock performance $makerUserId",
                    performanceDescription = "description",
                    performanceAttentionNote = "attention",
                    paymentAccount = PaymentAccount.of(BankName.BUSAN, "123-456", "maker"),
                    posterImage = POSTER_IMAGE,
                    performanceTeamName = "team",
                    performanceVenue = "venue",
                    roadAddressName = "road",
                    placeDetailAddress = "detail",
                    latitude = "37.0",
                    longitude = "127.0",
                    performanceContact = "010-0000-0000",
                    performancePeriod = PerformancePeriod.of(NOW.toLocalDate(), NOW.toLocalDate()),
                    ticketPrice = OLD_TICKET_PRICE,
                    totalScheduleCount = 1,
                    userId = makerUserId,
                )
            )
        val performanceId = requireNotNull(performance.id)
        val performanceDate = NOW.plusDays(1)
        val schedule =
            scheduleRepository.save(
                scheduleFixture(
                    performanceDate = performanceDate,
                    bookingCloseAt = performanceDate.plusHours(2),
                    totalTicketCount = 10,
                    scheduleNumber = ScheduleNumber.FIRST,
                    performanceId = performanceId,
                )
            )
        return Fixture(
            memberId = requireNotNull(member.id),
            performanceId = performanceId,
            scheduleId = requireNotNull(schedule.id),
            performanceDate = performanceDate,
            makerUserId = makerUserId,
        )
    }

    private fun modifyCommand(fixture: Fixture, ticketPrice: Int): PerformanceModifyCommand =
        PerformanceModifyCommand.of(
            performanceId = fixture.performanceId,
            performanceTitle = "Price lock performance ${fixture.makerUserId}",
            genre = PerformanceGenre.BAND,
            runningTime = 120,
            performanceDescription = "description",
            performanceAttentionNote = "attention",
            bankName = PerformanceBankName.BUSAN,
            accountNumber = "123-456",
            accountHolder = "maker",
            posterImage = POSTER_IMAGE,
            performanceTeamName = "team",
            performanceVenue = "venue",
            roadAddressName = "road",
            placeDetailAddress = "detail",
            latitude = "37.0",
            longitude = "127.0",
            performanceContact = "010-0000-0000",
            ticketPrice = ticketPrice,
            schedules =
                listOf(ScheduleModifyCommand.of(fixture.scheduleId, fixture.performanceDate, 10)),
            casts = emptyList(),
            staffs = emptyList(),
            images = emptyList(),
        )

    private fun guestBookingCommand(fixture: Fixture): GuestBookingCommand =
        GuestBookingCommand.of(
            fixture.scheduleId,
            1,
            "가격검증예매자",
            "010-9999-${(fixture.makerUserId % 10_000).toString().padStart(4, '0')}",
            "900101",
            "1234",
        )

    private fun assertPersistedSnapshot(fixture: Fixture, expectedPrice: Int) {
        checkNotNull(performanceRepository.findById(fixture.performanceId)).ticketPrice shouldBe
            expectedPrice
        checkNotNull(scheduleRepository.findById(fixture.scheduleId)).allocatedTicketCount shouldBe
            1
        val bookings = bookingRepository.findAll().filter { it.scheduleId == fixture.scheduleId }
        bookings.size shouldBe 1
        bookings.single().totalPaymentAmount shouldBe expectedPrice
    }

    private data class Fixture(
        val memberId: Long,
        val performanceId: Long,
        val scheduleId: Long,
        val performanceDate: LocalDateTime,
        val makerUserId: Long,
    )

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        @Primary
        fun performanceImageStorage(): PerformanceImageStorage =
            object : PerformanceImageStorage {
                override fun issueAllPresignedUrls(
                    posterImage: String,
                    castImages: List<String>,
                    staffImages: List<String>,
                    performanceImages: List<String>,
                ): PerformancePresignedUrls = PerformancePresignedUrls(emptyMap())

                override fun exists(imageKey: String): Boolean = true
            }
    }
}

private val CORRECTNESS = NamedTag("correctness")

private const val OLD_TICKET_PRICE = 10_000
private const val NEW_TICKET_PRICE = 20_000
private const val POSTER_IMAGE = "dev/poster/price-lock.jpg"
