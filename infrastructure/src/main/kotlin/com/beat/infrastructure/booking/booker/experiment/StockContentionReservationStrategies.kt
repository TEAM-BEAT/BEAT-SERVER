package com.beat.infrastructure.booking.booker.experiment

import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.experiment.OptimisticReservationConflict
import com.beat.application.frontoffice.booking.booker.experiment.ScheduleStockState
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentEnabled
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionLockTimeout
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionLockUnavailable
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionOutcome
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionReservationStrategy
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionScheduleStore
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionStrategy
import com.beat.application.frontoffice.booking.booker.experiment.StockReservationDecision
import com.beat.application.frontoffice.booking.booker.experiment.StockReservationRequest
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import java.time.Duration
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

private fun missingOrWrongSchedule(
    state: ScheduleStockState?,
    request: StockReservationRequest,
): ScheduleStockState {
    if (state == null || state.performanceId != request.performanceId) {
        throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
    }
    return state
}

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class PessimisticStockContentionReservationStrategy(
    private val scheduleStore: StockContentionScheduleStore
) : StockContentionReservationStrategy {
    override val strategy: StockContentionStrategy = StockContentionStrategy.PESSIMISTIC

    override fun reserve(request: StockReservationRequest): StockReservationDecision {
        val state =
            missingOrWrongSchedule(scheduleStore.find(request.scheduleId, true, false), request)
        if (!state.canReserve(request.purchaseTicketCount)) {
            return StockReservationDecision(StockContentionOutcome.SOLD_OUT)
        }
        return if (
            scheduleStore.reserveWithPessimisticLock(
                request.scheduleId,
                request.purchaseTicketCount,
            ) == 1
        ) {
            StockReservationDecision(StockContentionOutcome.ACCEPTED)
        } else {
            StockReservationDecision(StockContentionOutcome.SOLD_OUT)
        }
    }
}

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class OptimisticStockContentionReservationStrategy(
    private val scheduleStore: StockContentionScheduleStore
) : StockContentionReservationStrategy {
    override val strategy: StockContentionStrategy = StockContentionStrategy.OPTIMISTIC

    override fun reserve(request: StockReservationRequest): StockReservationDecision {
        val state =
            missingOrWrongSchedule(scheduleStore.find(request.scheduleId, false, true), request)
        if (!state.canReserve(request.purchaseTicketCount)) {
            return StockReservationDecision(StockContentionOutcome.SOLD_OUT)
        }
        val version =
            checkNotNull(state.version) { "The dev schedule.version migration is required" }
        if (
            scheduleStore.reserveWithOptimisticCas(
                request.scheduleId,
                request.purchaseTicketCount,
                version,
            ) == 1
        ) {
            return StockReservationDecision(StockContentionOutcome.ACCEPTED)
        }

        // A normal read is intentional here. FOR UPDATE would turn the optimistic retry into a
        // pessimistic wait and would violate the strategy's lock-set contract.
        val current =
            missingOrWrongSchedule(scheduleStore.find(request.scheduleId, false, true), request)
        if (!current.canReserve(request.purchaseTicketCount)) {
            return StockReservationDecision(StockContentionOutcome.SOLD_OUT)
        }
        throw OptimisticReservationConflict()
    }
}

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class AtomicStockContentionReservationStrategy(
    private val scheduleStore: StockContentionScheduleStore
) : StockContentionReservationStrategy {
    override val strategy: StockContentionStrategy = StockContentionStrategy.ATOMIC

    override fun reserve(request: StockReservationRequest): StockReservationDecision {
        return if (
            scheduleStore.reserveWithAtomicUpdate(
                request.scheduleId,
                request.purchaseTicketCount,
            ) == 1
        ) {
            StockReservationDecision(StockContentionOutcome.ACCEPTED)
        } else {
            // Common metadata validation already classified missing, mismatched, and closed
            // schedules. A failed conditional update is therefore sold out without a snapshot
            // follow-up read in the same REPEATABLE READ transaction.
            StockReservationDecision(StockContentionOutcome.SOLD_OUT)
        }
    }
}

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class RedisStockContentionReservationStrategy(
    private val scheduleStore: StockContentionScheduleStore,
    private val reservationLock: StockContentionRedisReservationLock,
) : StockContentionReservationStrategy {
    override val strategy: StockContentionStrategy = StockContentionStrategy.REDIS

    override fun reserve(request: StockReservationRequest): StockReservationDecision {
        val state =
            missingOrWrongSchedule(scheduleStore.find(request.scheduleId, false, false), request)
        if (!state.canReserve(request.purchaseTicketCount)) {
            return StockReservationDecision(StockContentionOutcome.SOLD_OUT)
        }
        if (
            scheduleStore.reserveWithRedisLock(request.scheduleId, request.purchaseTicketCount) == 1
        ) {
            return StockReservationDecision(StockContentionOutcome.ACCEPTED)
        }
        val current =
            missingOrWrongSchedule(scheduleStore.find(request.scheduleId, false, false), request)
        return if (current.canReserve(request.purchaseTicketCount)) {
            StockReservationDecision(StockContentionOutcome.CONFLICT_EXHAUSTED)
        } else {
            StockReservationDecision(StockContentionOutcome.SOLD_OUT)
        }
    }

    override fun <T> executeWithReservationLock(scheduleId: Long, operation: () -> T): T =
        reservationLock.withLock(scheduleId, operation)
}

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class StockContentionRedisReservationLock(
    private val redisTemplate: StringRedisTemplate,
    @param:Value("\${booking.experiment.redis.lease-seconds:60}") private val leaseSeconds: Long,
    @param:Value("\${booking.experiment.redis.acquire-timeout-seconds:30}")
    private val acquireTimeoutSeconds: Long,
) {
    fun <T> withLock(scheduleId: Long, operation: () -> T): T {
        val key = "$KEY_PREFIX$scheduleId"
        val token = UUID.randomUUID().toString()
        acquire(key, token)
        return try {
            operation()
        } finally {
            release(key, token)
        }
    }

    private fun acquire(key: String, token: String) {
        val deadline = System.nanoTime() + Duration.ofSeconds(acquireTimeoutSeconds).toNanos()
        try {
            while (System.nanoTime() < deadline) {
                if (
                    redisTemplate
                        .opsForValue()
                        .setIfAbsent(key, token, Duration.ofSeconds(leaseSeconds)) == true
                ) {
                    return
                }
                Thread.sleep(10)
            }
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw StockContentionLockTimeout()
        } catch (failure: RuntimeException) {
            throw StockContentionLockUnavailable(failure)
        }
        throw StockContentionLockTimeout()
    }

    private fun release(key: String, token: String) {
        repeat(RELEASE_MAX_ATTEMPTS) { attempt ->
            try {
                val deleted = redisTemplate.execute(RELEASE_SCRIPT, listOf(key), token) ?: 0L
                if (deleted == 0L) {
                    return
                }
                return
            } catch (failure: RuntimeException) {
                if (attempt == RELEASE_MAX_ATTEMPTS - 1) {
                    log.warn(
                        "Stock contention Redis lock release failed; lease expiry will recover key",
                        failure,
                    )
                    return
                }
                try {
                    Thread.sleep(RELEASE_RETRY_DELAY_MILLIS)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    log.warn(
                        "Stock contention Redis lock release interrupted; lease expiry will recover key",
                        interrupted,
                    )
                    return
                }
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(StockContentionRedisReservationLock::class.java)
        const val KEY_PREFIX = "beat:stock-contention:schedule:"
        const val RELEASE_MAX_ATTEMPTS = 3
        const val RELEASE_RETRY_DELAY_MILLIS = 10L
        val RELEASE_SCRIPT =
            DefaultRedisScript<Long>(
                """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """
                    .trimIndent(),
                Long::class.javaObjectType,
            )
    }
}
