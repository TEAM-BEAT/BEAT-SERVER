package com.beat.infrastructure.persistence.home.query

import com.beat.application.frontoffice.home.booker.query.HomeProjectionReader
import com.beat.domain.performance.model.Genre
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.infrastructure.config.JpaConfig
import com.beat.infrastructure.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infrastructure.persistence.performance.entity.PerformancePeriodJpaValue
import com.beat.infrastructure.persistence.performance.repository.PerformanceJpaRepository
import com.beat.infrastructure.persistence.promotion.entity.PromotionJpaEntity
import com.beat.infrastructure.persistence.promotion.repository.PromotionJpaRepository
import com.beat.infrastructure.persistence.schedule.entity.ScheduleJpaEntity
import com.beat.infrastructure.persistence.schedule.repository.ScheduleJpaRepository
import com.beat.infrastructure.support.MySqlTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
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
class HomeProjectionQueriesIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var reader: HomeProjectionReader

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var performanceRepository: PerformanceJpaRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleJpaRepository

    @Autowired
    private lateinit var promotionRepository: PromotionJpaRepository

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("genre로 필터링하고 authoritative home 공연 날짜를 선택한다") {
            val futurePerformance = performanceRepository.save(
                performance(
                    title = "Future performance",
                    genre = Genre.BAND,
                    periodStart = LocalDate.of(2026, 8, 1),
                    periodEnd = LocalDate.of(2026, 8, 31),
                ),
            )
            val pastOnlyPerformance = performance(
                title = "Past only performance",
                genre = Genre.BAND,
                periodStart = LocalDate.of(2026, 7, 1),
                periodEnd = LocalDate.of(2026, 7, 31),
            )
            val noSchedulePerformance = performance(
                title = "No schedule performance",
                genre = Genre.BAND,
                periodStart = LocalDate.of(2026, 9, 1),
                periodEnd = LocalDate.of(2026, 9, 30),
            )
            val differentGenrePerformance = performance(
                title = "Different genre performance",
                genre = Genre.PLAY,
                periodStart = LocalDate.of(2026, 10, 1),
                periodEnd = LocalDate.of(2026, 10, 31),
            )
            performanceRepository.saveAll(
                listOf(pastOnlyPerformance, noSchedulePerformance, differentGenrePerformance),
            )
            performanceRepository.flush()

            val now = LocalDateTime.of(2026, 8, 20, 12, 0)
            scheduleRepository.saveAll(
                listOf(
                    schedule(futurePerformance.id!!, now.plusDays(5), ScheduleNumber.FIRST),
                    schedule(futurePerformance.id!!, now.plusDays(1), ScheduleNumber.SECOND),
                    schedule(pastOnlyPerformance.id!!, now.minusDays(2), ScheduleNumber.FIRST),
                    schedule(pastOnlyPerformance.id!!, now.minusDays(5), ScheduleNumber.SECOND),
                ),
            )
            entityManager.flush()
            entityManager.clear()

            val performances = reader.read("BAND", now).performances.associateBy { it.performanceId }

            performances.keys shouldBe setOf(
                futurePerformance.id!!,
                pastOnlyPerformance.id!!,
                noSchedulePerformance.id!!,
            )

            val future = performances.getValue(futurePerformance.id!!)
            future.performanceTitle shouldBe "Future performance"
            future.ticketPrice shouldBe 15_000
            future.genre shouldBe "BAND"
            future.posterImage shouldBe "poster-Future performance.png"
            future.performanceVenue shouldBe "venue-Future performance"
            future.performanceDate shouldBe now.plusDays(1)
            future.periodStartDate shouldBe LocalDate.of(2026, 8, 1)
            future.periodEndDate shouldBe LocalDate.of(2026, 8, 31)

            val pastOnly = performances.getValue(pastOnlyPerformance.id!!)
            pastOnly.performanceDate shouldBe now.minusDays(5)

            val noSchedule = performances.getValue(noSchedulePerformance.id!!)
            noSchedule.performanceDate shouldBe null
            noSchedule.periodStartDate shouldBe LocalDate.of(2026, 9, 1)
            noSchedule.periodEndDate shouldBe LocalDate.of(2026, 9, 30)
        }

        test("nullable 필드를 잃지 않고 carousel 순서로 promotion을 projection한다") {
            val firstPerformance = performance(
                title = "Carousel performance",
                genre = Genre.BAND,
                periodStart = LocalDate.of(2026, 8, 1),
                periodEnd = LocalDate.of(2026, 8, 31),
            )
            performanceRepository.saveAndFlush(firstPerformance)

            promotionRepository.saveAll(
                listOf(
                    PromotionJpaEntity.rehydrate(
                        id = null,
                        promotionPhoto = "three.png",
                        performanceId = null,
                        redirectUrl = "",
                        isExternal = true,
                        carouselNumber = CarouselNumber.THREE,
                    ),
                    PromotionJpaEntity.rehydrate(
                        id = null,
                        promotionPhoto = "one.png",
                        performanceId = firstPerformance.id!!,
                        redirectUrl = "https://beat.example/one",
                        isExternal = false,
                        carouselNumber = CarouselNumber.ONE,
                    ),
                    PromotionJpaEntity.rehydrate(
                        id = null,
                        promotionPhoto = "two.png",
                        performanceId = null,
                        redirectUrl = "https://beat.example/two",
                        isExternal = false,
                        carouselNumber = CarouselNumber.TWO,
                    ),
                ),
            )
            entityManager.flush()
            entityManager.clear()

            val promotions = reader.read(null, NOW).promotions

            promotions.map { it.carouselNumber } shouldBe listOf("ONE", "TWO", "THREE")
            promotions.map { it.promotionPhoto } shouldBe listOf("one.png", "two.png", "three.png")
            promotions.map { it.performanceId } shouldBe listOf(firstPerformance.id, null, null)
            promotions.map { it.redirectUrl } shouldBe listOf(
                "https://beat.example/one",
                "https://beat.example/two",
                "",
            )
            promotions.map { it.isExternal } shouldBe listOf(false, false, true)
        }
    }

    private fun performance(
        title: String,
        genre: Genre,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): PerformanceJpaEntity = PerformanceJpaEntity.rehydrate(
        id = null,
        performanceTitle = title,
        genre = genre,
        runningTime = 90,
        performanceDescription = "description-$title",
        performanceAttentionNote = "attention-$title",
        paymentAccount = null,
        posterImage = "poster-$title.png",
        performanceTeamName = "team-$title",
        performanceVenue = "venue-$title",
        roadAddressName = "road",
        placeDetailAddress = "detail",
        latitude = "37.0",
        longitude = "127.0",
        performanceContact = "010-0000-0000",
        performancePeriodValue = PerformancePeriodJpaValue(periodStart, periodEnd),
        legacyPerformancePeriod = "$periodStart~$periodEnd",
        ticketPrice = 15_000,
        totalScheduleCount = 2,
        userId = 1L,
    )

    private fun schedule(
        performanceId: Long,
        performanceDate: LocalDateTime,
        scheduleNumber: ScheduleNumber,
    ): ScheduleJpaEntity = ScheduleJpaEntity.rehydrate(
        id = null,
        performanceDate = performanceDate,
        bookingCloseAt = performanceDate.minusHours(1),
        totalTicketCount = 100,
        soldTicketCount = 0,
        scheduleNumber = scheduleNumber,
        performanceId = performanceId,
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 20, 12, 0)
    }
}
