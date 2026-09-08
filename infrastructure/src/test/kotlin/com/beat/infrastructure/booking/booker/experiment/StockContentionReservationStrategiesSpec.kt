package com.beat.infrastructure.booking.booker.experiment

import com.beat.application.frontoffice.booking.booker.experiment.OptimisticReservationConflict
import com.beat.application.frontoffice.booking.booker.experiment.ScheduleStockState
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionOutcome
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionScheduleStore
import com.beat.application.frontoffice.booking.booker.experiment.StockReservationRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class StockContentionReservationStrategiesSpec : FunSpec() {
    private val scheduleStore = mockk<StockContentionScheduleStore>()
    private val request = StockReservationRequest(10L, 20L, 1)
    private val openSchedule =
        ScheduleStockState(
            id = 10L,
            performanceId = 20L,
            performanceDate = LocalDateTime.of(2030, 1, 1, 19, 0),
            bookingCloseAt = LocalDateTime.of(2030, 1, 1, 18, 0),
            totalTicketCount = 100,
            soldTicketCount = 99,
            scheduleNumber = "FIRST",
            bookingOpen = true,
            version = 4L,
        )

    init {
        beforeTest { clearMocks(scheduleStore) }

        test("pessimistic 전략은 행 잠금 뒤 조건부 재고를 한 번만 차감한다") {
            every { scheduleStore.find(10L, true, false) } returns openSchedule
            every { scheduleStore.reserveWithPessimisticLock(10L, 1) } returns 1

            PessimisticStockContentionReservationStrategy(scheduleStore)
                .reserve(request)
                .outcome shouldBe StockContentionOutcome.ACCEPTED

            verify(exactly = 1) { scheduleStore.find(10L, true, false) }
            verify(exactly = 1) { scheduleStore.reserveWithPessimisticLock(10L, 1) }
        }

        test("atomic 전략은 0행 갱신을 후속 조회 없이 SOLD_OUT으로 분류한다") {
            every { scheduleStore.reserveWithAtomicUpdate(10L, 1) } returns 0

            AtomicStockContentionReservationStrategy(scheduleStore)
                .reserve(request)
                .outcome shouldBe StockContentionOutcome.SOLD_OUT

            verify(exactly = 0) { scheduleStore.find(any(), any(), any()) }
        }

        test("optimistic 전략은 CAS 충돌 뒤 최신 행에도 재고가 있으면 충돌 예외를 발생시킨다") {
            every { scheduleStore.find(10L, false, true) } returnsMany
                listOf(openSchedule, openSchedule.copy(version = 5L))
            every { scheduleStore.reserveWithOptimisticCas(10L, 1, 4L) } returns 0

            shouldThrow<OptimisticReservationConflict> {
                OptimisticStockContentionReservationStrategy(scheduleStore).reserve(request)
            }
        }

        test("redis 전략은 lock 안에서 일반 재고 증가를 사용하고 atomic 갱신을 호출하지 않는다") {
            val reservationLock = mockk<StockContentionRedisReservationLock>()
            every { scheduleStore.find(10L, false, false) } returns openSchedule
            every { scheduleStore.reserveWithRedisLock(10L, 1) } returns 1
            every { reservationLock.withLock(10L, any<() -> StockContentionOutcome>()) } answers
                {
                    secondArg<() -> StockContentionOutcome>().invoke()
                }

            val strategy = RedisStockContentionReservationStrategy(scheduleStore, reservationLock)
            strategy.executeWithReservationLock(10L) { strategy.reserve(request).outcome }

            verify(exactly = 1) { scheduleStore.reserveWithRedisLock(10L, 1) }
            verify(exactly = 0) { scheduleStore.reserveWithAtomicUpdate(10L, 1) }
        }
    }
}
