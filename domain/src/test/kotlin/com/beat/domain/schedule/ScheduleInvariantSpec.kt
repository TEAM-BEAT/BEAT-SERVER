package com.beat.domain.schedule

import com.beat.domain.exception.DomainException
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class ScheduleInvariantSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    context("티켓 재고") {
        test("예매와 해제는 원본을 바꾸지 않고 할당 수량을 반영한다") {
            val schedule = schedule(totalTicketCount = 10, allocatedTicketCount = 3)

            val reserved = schedule.reserveTickets(2)
            val released = reserved.releaseTickets(1)

            schedule.allocatedTicketCount shouldBe 3
            reserved.allocatedTicketCount shouldBe 5
            released.allocatedTicketCount shouldBe 4
            released.availableTicketCount shouldBe 6
        }

        test("예매 수량은 양수여야 한다") {
            shouldFailWith(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT) {
                schedule().reserveTickets(0)
            }
        }

        test("잔여 수량보다 많이 예매할 수 없다") {
            shouldFailWith(ScheduleErrorCode.INSUFFICIENT_TICKETS) {
                schedule(totalTicketCount = 5, allocatedTicketCount = 4).reserveTickets(2)
            }
        }

        test("해제 수량은 양수이며 할당 수량을 초과할 수 없다") {
            val schedule = schedule(allocatedTicketCount = 3)
            shouldFailWith(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT) { schedule.releaseTickets(0) }
            shouldFailWith(ScheduleErrorCode.EXCESS_TICKET_DELETE) { schedule.releaseTickets(4) }
        }

        test("잔여 수량은 전체 수량에서 할당 수량을 뺀 값이다") {
            schedule(totalTicketCount = 10, allocatedTicketCount = 3).availableTicketCount shouldBe 7
        }

        test("양수인 요청 수량이 잔여 수량 이내일 때만 구매할 수 있다") {
            val schedule = schedule(totalTicketCount = 10, allocatedTicketCount = 3)
            schedule.canPurchase(7) shouldBe true
            schedule.canPurchase(8) shouldBe false
            schedule.canPurchase(0) shouldBe false
            schedule.canPurchase(-1) shouldBe false
        }
    }

    context("티켓 수량 상태") {
        test("생성할 때 전체 수량은 음수일 수 없다") {
            shouldFailWith(ScheduleErrorCode.NEGATIVE_TICKET_COUNT) {
                Schedule.create(PERFORMANCE_AT, BOOKING_CLOSE_AT, -1, ScheduleNumber.FIRST, PERFORMANCE_ID)
            }
        }

        test("복원할 때 할당 수량은 음수이거나 전체 수량보다 클 수 없다") {
            shouldFailWith(ScheduleErrorCode.NEGATIVE_TICKET_COUNT) {
                schedule(allocatedTicketCount = -1)
            }
            shouldFailWith(ScheduleErrorCode.ALLOCATED_TICKETS_EXCEED_TOTAL) {
                schedule(totalTicketCount = 3, allocatedTicketCount = 4)
            }
        }

        test("수정한 전체 수량은 음수이거나 현재 할당 수량보다 작을 수 없다") {
            val schedule = schedule(allocatedTicketCount = 3)
            shouldFailWith(ScheduleErrorCode.ALLOCATED_TICKETS_EXCEED_TOTAL) {
                schedule.update(PERFORMANCE_AT, BOOKING_CLOSE_AT, 2, ScheduleNumber.SECOND)
            }
            shouldFailWith(ScheduleErrorCode.NEGATIVE_TICKET_COUNT) {
                schedule.update(PERFORMANCE_AT, BOOKING_CLOSE_AT, -1, ScheduleNumber.SECOND)
            }
        }
    }

    context("예매 종료 시각") {
        test("공연 연장에 맞춰 뒤로 변경할 수 있다") {
            val schedule = schedule(allocatedTicketCount = 3)
            val extendedCloseAt = schedule.bookingCloseAt.plusHours(1)

            schedule.updateBookingCloseAt(extendedCloseAt).bookingCloseAt shouldBe extendedCloseAt
        }

        test("공연 시작 이후로 변경할 수 없다") {
            val schedule = schedule(allocatedTicketCount = 3)
            shouldFailWith(ScheduleErrorCode.INVALID_BOOKING_WINDOW) {
                schedule.updateBookingCloseAt(schedule.performanceDate.minusNanos(1))
            }
        }
    }

    context("회차 lifecycle") {
        test("이미 지난 공연 시각으로 예정 회차를 만들 수 없다") {
            val now = LocalDateTime.of(2026, 1, 2, 12, 0)
            shouldFailWith(ScheduleErrorCode.PAST_SCHEDULE_NOT_ALLOWED) {
                Schedule.createUpcoming(
                    now.minusMinutes(1),
                    now.plusHours(1),
                    10,
                    ScheduleNumber.FIRST,
                    PERFORMANCE_ID,
                    now,
                )
            }
        }

        test("이미 종료된 회차를 다시 편성할 수 없다") {
            val performanceAt = LocalDateTime.of(2026, 1, 1, 12, 0)
            val schedule = Schedule.rehydrate(
                1L,
                performanceAt,
                performanceAt.plusHours(1),
                10,
                0,
                ScheduleNumber.FIRST,
                PERFORMANCE_ID,
            )
            shouldFailWith(ScheduleErrorCode.ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED) {
                schedule.reschedule(
                    performanceAt,
                    performanceAt.plusHours(1),
                    10,
                    ScheduleNumber.FIRST,
                    performanceAt.plusHours(2),
                )
            }
        }

        test("Performance 식별자로 소속을 판단한다") {
            val schedule = schedule()
            schedule.belongsTo(PERFORMANCE_ID) shouldBe true
            schedule.belongsTo(PERFORMANCE_ID + 1) shouldBe false
        }
    }
})

private fun schedule(
    totalTicketCount: Int = 10,
    allocatedTicketCount: Int = 0,
): Schedule = Schedule.rehydrate(
    1L,
    PERFORMANCE_AT,
    BOOKING_CLOSE_AT,
    totalTicketCount,
    allocatedTicketCount,
    ScheduleNumber.FIRST,
    PERFORMANCE_ID,
)

private inline fun shouldFailWith(expected: ScheduleErrorCode, action: () -> Unit) {
    shouldThrow<DomainException>(action).errorCode shouldBe expected
}

private const val PERFORMANCE_ID = 1L
private val PERFORMANCE_AT = LocalDateTime.of(2026, 5, 1, 12, 0)
private val BOOKING_CLOSE_AT = PERFORMANCE_AT.plusHours(1)
