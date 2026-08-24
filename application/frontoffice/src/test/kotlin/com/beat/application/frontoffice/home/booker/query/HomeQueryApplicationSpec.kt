package com.beat.application.frontoffice.home.booker.query

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HomeQueryApplicationSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    test("장르와 clock 시각을 reader에 한 번 전달하고 promotion projection을 그대로 결과로 매핑한다") {
        val promotion = HomePromotionProjection(
            promotionId = 17L,
            promotionPhoto = "promotion.jpg",
            performanceId = null,
            redirectUrl = "https://example.com/promotion",
            isExternal = true,
            carouselNumber = "THREE",
        )
        val reader = RecordingHomeProjectionReader(
            HomeProjection(promotions = listOf(promotion), performances = emptyList()),
        )
        val service = HomeQueryService(reader, FIXED_CLOCK)

        val result = service.findHomePerformanceList("PLAY")

        reader.calls shouldBe listOf(ReaderCall("PLAY", NOW))
        result.promotionList shouldBe listOf(
            HomePromotionResult(
                promotionId = promotion.promotionId,
                promotionPhoto = promotion.promotionPhoto,
                performanceId = promotion.performanceId,
                redirectUrl = promotion.redirectUrl,
                isExternal = promotion.isExternal,
                carouselNumber = promotion.carouselNumber,
            ),
        )
    }

    test("공연 기간 형식과 dueDate 정책에 따라 비만료 공연, 미지정 회차, 만료 공연 순으로 정렬한다") {
        val reader = RecordingHomeProjectionReader(
            HomeProjection(
                promotions = emptyList(),
                performances = listOf(
                    performance(
                        id = 1L,
                        performanceDate = NOW.plusDays(3),
                        periodStart = LocalDate.of(2026, 1, 20),
                        periodEnd = LocalDate.of(2026, 1, 20),
                    ),
                    performance(
                        id = 2L,
                        performanceDate = NOW.plusDays(1),
                        periodStart = LocalDate.of(2026, 1, 21),
                        periodEnd = LocalDate.of(2026, 1, 23),
                    ),
                    performance(
                        id = 3L,
                        performanceDate = null,
                        periodStart = LocalDate.of(2026, 1, 24),
                        periodEnd = LocalDate.of(2026, 1, 24),
                    ),
                    performance(
                        id = 4L,
                        performanceDate = NOW.minusDays(1),
                        periodStart = LocalDate.of(2026, 1, 25),
                        periodEnd = LocalDate.of(2026, 1, 27),
                    ),
                    performance(
                        id = 5L,
                        performanceDate = NOW.minusDays(5),
                        periodStart = LocalDate.of(2026, 1, 28),
                        periodEnd = LocalDate.of(2026, 1, 28),
                    ),
                ),
            ),
        )
        val service = HomeQueryService(reader, FIXED_CLOCK)

        val result = service.findHomePerformanceList("PLAY")

        result.performanceList.map { it.performanceId } shouldBe listOf(2L, 1L, 3L, 4L, 5L)
        result.performanceList.map { it.dueDate } shouldBe listOf(1, 3, Int.MAX_VALUE, -1, -5)
        result.performanceList.map { it.performancePeriod } shouldBe listOf(
            "2026.01.21~2026.01.23",
            "2026.01.20",
            "2026.01.24",
            "2026.01.25~2026.01.27",
            "2026.01.28",
        )
    }

    test("장르가 null이고 projection이 비어 있으면 빈 결과를 반환하고 null을 reader에 전달한다") {
        val reader = RecordingHomeProjectionReader(HomeProjection(emptyList(), emptyList()))
        val service = HomeQueryService(reader, FIXED_CLOCK)

        val result = service.findHomePerformanceList(null)

        reader.calls shouldBe listOf(ReaderCall(null, NOW))
        result.promotionList shouldBe emptyList()
        result.performanceList shouldBe emptyList()
    }
})

private class RecordingHomeProjectionReader(
    private val projection: HomeProjection,
) : HomeProjectionReader {
    val calls = mutableListOf<ReaderCall>()

    override fun read(genre: String?, now: LocalDateTime): HomeProjection {
        calls += ReaderCall(genre, now)
        return projection
    }
}

private data class ReaderCall(
    val genre: String?,
    val now: LocalDateTime,
)

private fun performance(
    id: Long,
    performanceDate: LocalDateTime?,
    periodStart: LocalDate,
    periodEnd: LocalDate,
): HomePerformanceProjection = HomePerformanceProjection(
    performanceId = id,
    performanceTitle = "공연 $id",
    ticketPrice = 10_000 + id.toInt(),
    genre = "PLAY",
    posterImage = "poster-$id.jpg",
    performanceVenue = "공연장 $id",
    performanceDate = performanceDate,
    periodStartDate = periodStart,
    periodEndDate = periodEnd,
)

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2026-01-10T03:00:00Z"),
    ZoneId.of("Asia/Seoul"),
)

private val NOW: LocalDateTime = LocalDateTime.of(2026, 1, 10, 12, 0)
