package com.beat.application.system.promotion.command

import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.promotion.service.PromotionCarouselDomainService
import com.beat.domain.promotion.service.PromotionEligibilityDomainService
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class PromotionMaintenanceApplicationSpec : FunSpec({
    test("Performance와 Schedule을 정렬해 잠근 뒤 만료 Promotion을 삭제하고 carousel을 재배치한다") {
        val expired = promotion(1L, 20L, CarouselNumber.FIVE)
        val active = promotion(2L, 10L, CarouselNumber.FOUR)
        val external = promotion(3L, null, CarouselNumber.TWO)
        val fixture = fixture(
            discovered = listOf(expired, active, external),
            authoritative = listOf(expired, active, external),
            schedules = listOf(
                schedule(102L, 10L, LocalDate.of(2026, 8, 24)),
                schedule(101L, 10L, LocalDate.of(2026, 8, 22)),
                schedule(201L, 20L, LocalDate.of(2026, 8, 22)),
            ),
        )

        fixture.service.checkAndDeleteInvalidPromotions()

        fixture.performances.lockedIds shouldContainExactly listOf(10L, 20L)
        fixture.schedules.lockedIds shouldContainExactly listOf(101L, 102L, 201L)
        fixture.promotions.deletedIds shouldContainExactly listOf(1L)
        fixture.promotions.saved.map { it.id } shouldContainExactly listOf(3L, 2L)
        fixture.promotions.saved.map { it.carouselNumber } shouldContainExactly
            listOf(CarouselNumber.ONE, CarouselNumber.TWO)
    }

    test("회차가 없거나 Performance가 이미 없는 기존 내부 Promotion은 현재 compatibility대로 유지한다") {
        val noSchedule = promotion(1L, 10L, CarouselNumber.ONE)
        val missingPerformance = promotion(2L, 20L, CarouselNumber.TWO)
        val fixture = fixture(
            discovered = listOf(noSchedule, missingPerformance),
            authoritative = listOf(noSchedule, missingPerformance),
            performanceIds = setOf(10L),
            schedules = emptyList(),
        )

        fixture.service.checkAndDeleteInvalidPromotions()

        fixture.promotions.deletedIds shouldBe emptyList()
        fixture.promotions.saved shouldBe emptyList()
    }

    test("discovery 이후 새 Performance reference가 나타나면 잠그지 않은 state로 삭제 판단하지 않는다") {
        val discovered = promotion(1L, 10L, CarouselNumber.ONE)
        val concurrentlyAdded = promotion(2L, 30L, CarouselNumber.TWO)
        val fixture = fixture(
            discovered = listOf(discovered),
            authoritative = listOf(discovered, concurrentlyAdded),
            performanceIds = setOf(10L, 30L),
            schedules = listOf(
                schedule(101L, 10L, LocalDate.of(2026, 8, 24)),
                schedule(301L, 30L, LocalDate.of(2026, 8, 1)),
            ),
        )

        fixture.service.checkAndDeleteInvalidPromotions()

        fixture.performances.lockedIds shouldContainExactly listOf(10L)
        fixture.promotions.deletedIds shouldBe emptyList()
    }
})

private data class PromotionMaintenanceFixture(
    val service: PromotionMaintenanceService,
    val promotions: RecordingSystemPromotionRepository,
    val performances: RecordingSystemPerformanceRepository,
    val schedules: RecordingSystemScheduleRepository,
)

private fun fixture(
    discovered: List<Promotion>,
    authoritative: List<Promotion>,
    performanceIds: Set<Long> = authoritative.mapNotNull(Promotion::performanceId).toSet(),
    schedules: List<Schedule>,
): PromotionMaintenanceFixture {
    val promotionRepository = RecordingSystemPromotionRepository(discovered, authoritative)
    val performanceRepository = RecordingSystemPerformanceRepository(performanceIds.associateWith(::performance))
    val scheduleRepository = RecordingSystemScheduleRepository(schedules)
    return PromotionMaintenanceFixture(
        service = PromotionMaintenanceService(
            promotionRepository,
            performanceRepository,
            scheduleRepository,
            PromotionCarouselDomainService(),
            PromotionEligibilityDomainService(),
            SYSTEM_CLOCK,
        ),
        promotions = promotionRepository,
        performances = performanceRepository,
        schedules = scheduleRepository,
    )
}

private class RecordingSystemPromotionRepository(
    private val discovered: List<Promotion>,
    private val authoritative: List<Promotion>,
) : PromotionRepository {
    val deletedIds = mutableListOf<Long>()
    val saved = mutableListOf<Promotion>()

    override fun findAll(): List<Promotion> = discovered
    override fun lockAll(): List<Promotion> = authoritative
    override fun deleteByPromotionIds(promotionIds: List<Long>) {
        deletedIds += promotionIds
    }
    override fun saveAll(promotions: List<Promotion>): List<Promotion> = promotions.also(saved::addAll)
    override fun findById(promotionId: Long): Promotion? = null
    override fun save(promotion: Promotion): Promotion = promotion
    override fun deleteByPerformanceId(performanceId: Long) = Unit
    override fun findByCarouselNumber(carouselNumber: CarouselNumber): Promotion? = null
}

private class RecordingSystemPerformanceRepository(
    private val performances: Map<Long, Performance>,
) : PerformanceRepository {
    val lockedIds = mutableListOf<Long>()

    override fun lockById(id: Long): Performance? {
        lockedIds += id
        return performances[id]
    }
    override fun findById(id: Long): Performance? = performances[id]
    override fun save(performance: Performance): Performance = performance
    override fun deleteById(id: Long) = Unit
}

private class RecordingSystemScheduleRepository(
    schedules: List<Schedule>,
) : ScheduleRepository {
    private val schedules = schedules.associateBy { checkNotNull(it.id) }
    val lockedIds = mutableListOf<Long>()

    override fun findIdsByPerformanceId(performanceId: Long): List<Long> = schedules.values
        .filter { it.performanceId == performanceId }
        .map { checkNotNull(it.id) }
        .reversed()
    override fun lockById(id: Long): Schedule? {
        lockedIds += id
        return schedules[id]
    }
    override fun findById(id: Long): Schedule? = schedules[id]
    override fun findPerformanceIdById(id: Long): Long? = schedules[id]?.performanceId
    override fun isBeforeBookingCloseAt(id: Long): Boolean = false
    override fun findAllByPerformanceId(performanceId: Long): List<Schedule> = schedules.values
        .filter { it.performanceId == performanceId }
    override fun findAllById(ids: Collection<Long>): List<Schedule> = ids.mapNotNull(schedules::get)
    override fun countByPerformanceId(performanceId: Long): Int = findAllByPerformanceId(performanceId).size
    override fun save(schedule: Schedule): Schedule = schedule
    override fun saveAll(schedules: List<Schedule>): List<Schedule> = schedules
    override fun delete(schedule: Schedule) = Unit
    override fun deleteByPerformanceId(performanceId: Long) = Unit
}

private fun promotion(id: Long, performanceId: Long?, carouselNumber: CarouselNumber): Promotion =
    Promotion.rehydrate(id, "image-$id", performanceId, "url-$id", performanceId == null, carouselNumber)

private fun schedule(id: Long, performanceId: Long, date: LocalDate): Schedule = Schedule.rehydrate(
    id = id,
    performanceDate = date.atTime(20, 0),
    bookingCloseAt = date.atTime(20, 0),
    totalTicketCount = 10,
    allocatedTicketCount = 0,
    scheduleNumber = ScheduleNumber.FIRST,
    performanceId = performanceId,
)

private fun performance(id: Long): Performance = Performance.rehydrate(
    id,
    "title",
    Genre.PLAY,
    RunningTime.of(120),
    "description",
    "attention",
    null,
    "poster",
    "team",
    "venue",
    "road",
    "detail",
    "37.0",
    "127.0",
    "contact",
    PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
    TicketPrice.of(10_000),
    1,
    1L,
)

private val SYSTEM_CLOCK: Clock = Clock.fixed(
    Instant.parse("2026-08-23T00:00:00Z"),
    ZoneOffset.UTC,
)
