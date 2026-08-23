package com.beat.domain.schedule

import com.beat.domain.exception.DomainException
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.service.ScheduleSequenceDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalTime

class ScheduleSequenceDomainServiceSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance
    val service = ScheduleSequenceDomainService()

    test("시간순으로 회차 번호를 부여하고 입력 목록은 변경하지 않는다") {
        val later = sequenceSchedule(2L, TODAY.plusDays(1))
        val earlier = sequenceSchedule(1L, TODAY)
        val input = listOf(later, earlier)

        val assigned = service.assignScheduleNumbers(input)

        assigned.map { it.id } shouldBe listOf(1L, 2L)
        assigned.map { it.scheduleNumber } shouldBe listOf(ScheduleNumber.FIRST, ScheduleNumber.SECOND)
        input shouldBe listOf(later, earlier)
    }

    test("지원하는 회차 번호보다 많은 회차는 허용하지 않는다") {
        val schedules = (0..ScheduleNumber.entries.size).map { index ->
            sequenceSchedule(index.toLong(), TODAY.plusDays(index.toLong()))
        }

        shouldThrow<DomainException> { service.assignScheduleNumbers(schedules) }.errorCode shouldBe
            ScheduleErrorCode.TOO_MANY_SCHEDULES
        service.validateScheduleCount(ScheduleNumber.entries.size.toLong())
    }

    test("서로 다른 Performance의 회차를 함께 정렬할 수 없다") {
        val schedules = listOf(
            sequenceSchedule(1L, TODAY, performanceId = 10L),
            sequenceSchedule(2L, TODAY.plusDays(1), performanceId = 20L),
        )

        shouldThrow<DomainException> { service.assignScheduleNumbers(schedules) }.errorCode shouldBe
            ScheduleErrorCode.MIXED_PERFORMANCE_SCHEDULES
    }
})

private fun sequenceSchedule(
    id: Long,
    performanceDate: LocalDate,
    performanceId: Long = 1L,
): Schedule {
    val performanceAt = performanceDate.atTime(LocalTime.NOON)
    return Schedule.rehydrate(
        id,
        performanceAt,
        performanceAt.plusHours(1),
        10,
        0,
        ScheduleNumber.FIRST,
        performanceId,
    )
}

private val TODAY = LocalDate.of(2026, 4, 30)
