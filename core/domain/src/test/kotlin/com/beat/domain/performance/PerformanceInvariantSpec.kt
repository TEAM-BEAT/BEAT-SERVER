package com.beat.domain.performance

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.performance.model.Cast
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.model.PerformanceImage
import com.beat.domain.performance.model.Staff
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.sharedkernel.model.AggregateRoot
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PerformanceInvariantSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    context("공연의 회차 수") {
        test("생성할 때 음수일 수 없다") {
            shouldFailWith(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT) {
                performance(totalScheduleCount = -1)
            }
        }

        test("수정할 때 음수일 수 없다") {
            shouldFailWith(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT) {
                update(performance(), totalScheduleCount = -1)
            }
        }
    }

    context("active Booking이 있는 공연") {
        test("다른 가격으로 변경할 수 없다") {
            shouldFailWith(PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED) {
                performance().updateTicketPrice(12_000, hasActiveBooking = true)
            }
        }

        test("삭제할 수 없다") {
            shouldFailWith(PerformanceErrorCode.DELETE_NOT_ALLOWED) {
                performance().ensureDeletable(hasActiveBooking = true)
            }
        }
    }

    test("Cast, Staff, PerformanceImage는 독립 Aggregate가 아니며 Performance 식별자를 중복 소유하지 않는다") {
        listOf(Cast::class.java, Staff::class.java, PerformanceImage::class.java).forEach { childType ->
            AggregateRoot::class.java.isAssignableFrom(childType) shouldBe false
            childType.declaredFields.any { it.name == "performanceId" } shouldBe false
        }
    }
})

private fun performance(totalScheduleCount: Int = 1): Performance = Performance.create(
    performanceTitle = "title",
    genre = Genre.BAND,
    runningTime = RunningTime.of(60),
    performanceDescription = "description",
    performanceAttentionNote = "attention",
    paymentAccount = null,
    posterImage = "poster",
    performanceTeamName = "team",
    performanceVenue = "venue",
    roadAddressName = "road",
    placeDetailAddress = "detail",
    latitude = "37.1",
    longitude = "127.1",
    performanceContact = "010-1234-5678",
    performancePeriod = PERFORMANCE_PERIOD,
    ticketPrice = TicketPrice.of(10_000),
    totalScheduleCount = totalScheduleCount,
    userId = 1L,
)

private fun update(performance: Performance, totalScheduleCount: Int): Performance = performance.update(
    performanceTitle = "title",
    genre = Genre.BAND,
    runningTime = RunningTime.of(60),
    performanceDescription = "description",
    performanceAttentionNote = "attention",
    paymentAccount = null,
    posterImage = "poster",
    performanceTeamName = "team",
    performanceVenue = "venue",
    roadAddressName = "road",
    placeDetailAddress = "detail",
    latitude = "37.1",
    longitude = "127.1",
    performanceContact = "010-1234-5678",
    performancePeriod = PERFORMANCE_PERIOD,
    totalScheduleCount = totalScheduleCount,
)

private inline fun shouldFailWith(expected: PerformanceErrorCode, action: () -> Unit) {
    shouldThrow<DomainException>(action).errorCode shouldBe expected
}

private val PERFORMANCE_PERIOD = PerformancePeriod.of(
    LocalDate.of(2026, 1, 1),
    LocalDate.of(2026, 1, 1),
)
