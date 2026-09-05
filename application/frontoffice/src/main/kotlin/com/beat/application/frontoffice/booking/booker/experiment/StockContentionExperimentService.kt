package com.beat.application.frontoffice.booking.booker.experiment

import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.locks.LockSupport
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class StockContentionExperimentProperties {
    @Value("\${booking.experiment.optimistic-max-attempts:50}") var optimisticMaxAttempts: Int = 50

    @Value("\${booking.experiment.optimistic-backoff-millis:1}")
    var optimisticBackoffMillis: Long = 1L

    @Value("\${booking.experiment.transaction-timeout-seconds:30}")
    var transactionTimeoutSeconds: Int = 30
}

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class StockContentionStrategyRegistry(strategies: List<StockContentionReservationStrategy>) {
    private val strategiesByName = strategies.associateBy { it.strategy }

    init {
        check(strategiesByName.keys == StockContentionStrategy.entries.toSet()) {
            "Stock contention experiment must register every strategy"
        }
    }

    fun get(strategy: StockContentionStrategy): StockContentionReservationStrategy =
        checkNotNull(strategiesByName[strategy]) {
            "No stock contention strategy registered: $strategy"
        }
}

@Service
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class StockContentionExperimentService(
    private val strategyRegistry: StockContentionStrategyRegistry,
    private val memberRepository: MemberRepository,
    private val performanceRepository: PerformanceRepository,
    private val bookingRepository: BookingRepository,
    private val scheduleStore: StockContentionScheduleStore,
    transactionManager: PlatformTransactionManager,
    private val clock: Clock,
    private val properties: StockContentionExperimentProperties,
) {
    private val optimisticAttemptLimit =
        properties.optimisticMaxAttempts.coerceIn(1, MAX_OPTIMISTIC_ATTEMPTS)
    private val optimisticBackoffNanos =
        properties.optimisticBackoffMillis.coerceIn(0L, MAX_OPTIMISTIC_BACKOFF_MILLIS) *
            NANOS_PER_MILLISECOND
    private val transactionTemplate =
        TransactionTemplate(transactionManager).apply {
            timeout =
                properties.transactionTimeoutSeconds.coerceIn(1, MAX_TRANSACTION_TIMEOUT_SECONDS)
        }

    fun createMemberBooking(
        memberId: Long,
        strategy: StockContentionStrategy,
        command: StockContentionBookingCommand,
    ): StockContentionExperimentResponse {
        validateBookerContact(command.bookerName, command.bookerPhoneNumber)
        validatePurchaseTicketCount(command.purchaseTicketCount)

        val selectedStrategy = strategyRegistry.get(strategy)
        return try {
            selectedStrategy.executeWithReservationLock(command.scheduleId) {
                if (strategy == StockContentionStrategy.OPTIMISTIC) {
                    createWithOptimisticRetry(memberId, command, selectedStrategy)
                } else {
                    executeAttempt(memberId, command, selectedStrategy, 1)
                }
            }
        } catch (_: StockContentionLockTimeout) {
            StockContentionExperimentResponse(
                outcome = StockContentionOutcome.LOCK_TIMEOUT,
                bookingId = null,
                attemptCount = 0,
            )
        } catch (_: StockContentionLockUnavailable) {
            StockContentionExperimentResponse(
                outcome = StockContentionOutcome.LOCK_TIMEOUT,
                bookingId = null,
                attemptCount = 0,
            )
        }
    }

    private fun createWithOptimisticRetry(
        memberId: Long,
        command: StockContentionBookingCommand,
        strategy: StockContentionReservationStrategy,
    ): StockContentionExperimentResponse {
        for (attempt in 1..optimisticAttemptLimit) {
            try {
                return executeAttempt(memberId, command, strategy, attempt)
            } catch (_: OptimisticReservationConflict) {
                if (attempt == optimisticAttemptLimit) {
                    return StockContentionExperimentResponse(
                        outcome = StockContentionOutcome.CONFLICT_EXHAUSTED,
                        bookingId = null,
                        attemptCount = attempt,
                    )
                }
                LockSupport.parkNanos(optimisticBackoffNanos)
                if (Thread.currentThread().isInterrupted) {
                    return StockContentionExperimentResponse(
                        outcome = StockContentionOutcome.CONFLICT_EXHAUSTED,
                        bookingId = null,
                        attemptCount = attempt,
                    )
                }
            }
        }
        error("Optimistic retry loop did not execute")
    }

    private fun executeAttempt(
        memberId: Long,
        command: StockContentionBookingCommand,
        strategy: StockContentionReservationStrategy,
        attempt: Int,
    ): StockContentionExperimentResponse =
        checkNotNull(
            transactionTemplate.execute {
                val member =
                    memberRepository.findById(memberId)
                        ?: throw FrontofficeApplicationException(
                            BookingApplicationErrorCode.MEMBER_NOT_FOUND
                        )
                val scheduleMetadata =
                    scheduleStore.findBookingMetadataById(command.scheduleId)
                        ?: throw FrontofficeApplicationException(
                            BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
                        )
                if (!scheduleMetadata.bookingOpen) {
                    throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.BOOKING_CLOSED
                    )
                }
                val performanceId = scheduleMetadata.performanceId
                val performance =
                    // Common validation must not serialize requests on the performance row. The
                    // selected schedule strategy is the only contention mechanism in this
                    // experiment.
                    performanceRepository.findById(performanceId)
                        ?: throw FrontofficeApplicationException(
                            BookingApplicationErrorCode.PERFORMANCE_NOT_FOUND
                        )
                val reservation =
                    strategy.reserve(
                        StockReservationRequest(
                            scheduleId = command.scheduleId,
                            performanceId = performanceId,
                            purchaseTicketCount = command.purchaseTicketCount,
                        )
                    )
                if (reservation.outcome != StockContentionOutcome.ACCEPTED) {
                    return@execute StockContentionExperimentResponse(
                        outcome = reservation.outcome,
                        bookingId = null,
                        attemptCount = attempt,
                    )
                }

                val totalPaymentAmount =
                    calculatePaymentAmount(performance.ticketPrice, command.purchaseTicketCount)
                val booking =
                    Booking.create(
                        purchaseTicketCount = command.purchaseTicketCount,
                        bookerName = command.bookerName,
                        bookerPhoneNumber = command.bookerPhoneNumber,
                        birthDate = null,
                        password = null,
                        scheduleId = command.scheduleId,
                        userId = member.userId,
                        createdAt = LocalDateTime.now(clock),
                        totalPaymentAmount = totalPaymentAmount,
                    )
                val savedBooking = bookingRepository.save(booking)
                StockContentionExperimentResponse(
                    outcome = StockContentionOutcome.ACCEPTED,
                    bookingId = requireNotNull(savedBooking.id),
                    attemptCount = attempt,
                )
            }
        )

    private fun validateBookerContact(name: String, phoneNumber: String) {
        if (!NAME_PATTERN.matches(name) || !PHONE_PATTERN.matches(phoneNumber)) {
            throw FrontofficeApplicationException(
                BookingApplicationErrorCode.INVALID_REQUEST_FORMAT
            )
        }
    }

    private fun validatePurchaseTicketCount(ticketCount: Int) {
        if (ticketCount !in MIN_PURCHASE_TICKET_COUNT..MAX_PURCHASE_TICKET_COUNT) {
            throw FrontofficeApplicationException(
                BookingApplicationErrorCode.INVALID_REQUEST_FORMAT
            )
        }
    }

    private fun calculatePaymentAmount(ticketPrice: Int, quantity: Int): Int {
        val amount = ticketPrice.toLong() * quantity
        if (amount > Int.MAX_VALUE) {
            throw FrontofficeApplicationException(
                BookingApplicationErrorCode.TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE
            )
        }
        return amount.toInt()
    }

    private companion object {
        val NAME_PATTERN = Regex("^[a-zA-Z가-힣]+$")
        val PHONE_PATTERN = Regex("^\\d{3}-\\d{4}-\\d{4}$")
        const val MIN_PURCHASE_TICKET_COUNT = 1
        const val MAX_PURCHASE_TICKET_COUNT = 10
        const val MAX_OPTIMISTIC_ATTEMPTS = 50
        const val MAX_OPTIMISTIC_BACKOFF_MILLIS = 1L
        const val MAX_TRANSACTION_TIMEOUT_SECONDS = 30
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
