package com.beat.apis.schedule

import com.beat.apis.support.AbstractIntegrationTest
import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReadModel
import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReader
import com.beat.domain.schedule.repository.ScheduleRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class ScheduleBookingAvailabilityIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var scheduleAvailabilityReader: PerformanceScheduleAvailabilityReader

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @AfterEach
    fun cleanUp() {
        jdbcTemplate.update("DELETE FROM schedule WHERE performance_id = ?", PERFORMANCE_ID)
    }

    @Test
    fun availabilityQueryUsesOneDatabaseClockForEverySchedule() {
        val databaseNow = jdbcTemplate.queryForObject(
            "SELECT CURRENT_TIMESTAMP(6)",
            LocalDateTime::class.java,
        )!!
        insertSchedule(databaseNow.plusDays(1), databaseNow.plusDays(1).plusHours(2), 10, 0, "FIRST")
        insertSchedule(databaseNow.minusHours(2), databaseNow.minusSeconds(1), 10, 0, "SECOND")
        insertSchedule(databaseNow.plusDays(1), databaseNow.plusDays(1).plusHours(2), 10, 10, "THIRD")

        val schedules: List<PerformanceScheduleAvailabilityReadModel> =
            scheduleAvailabilityReader.findAllByPerformanceId(PERFORMANCE_ID)
        val schedulesByNumber = schedules.associateBy { it.scheduleNumber }

        assertEquals(3, schedules.size)
        assertTrue(schedulesByNumber.getValue("FIRST").isBooking)
        assertFalse(schedulesByNumber.getValue("SECOND").isBooking)
        assertFalse(schedulesByNumber.getValue("THIRD").isBooking)
        assertEquals(1L, schedules.map { it.evaluatedAt }.distinct().count().toLong())

        jdbcTemplate.update(
            """
            UPDATE schedule
            SET booking_close_at = DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 HOUR)
            WHERE performance_id = ? AND schedule_number = 'SECOND'
            """.trimIndent(),
            PERFORMANCE_ID,
        )

        val extendedSchedules: Map<String, PerformanceScheduleAvailabilityReadModel> =
            scheduleAvailabilityReader
                .findAllByPerformanceId(PERFORMANCE_ID)
                .associateBy { it.scheduleNumber }
        assertTrue(extendedSchedules.getValue("SECOND").isBooking)
    }

    @Test
    fun closeTimeIsRecheckedAfterLockWaitUnderRepeatableRead() {
        assertEquals(
            "REPEATABLE-READ",
            jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String::class.java),
        )
        val databaseNow = jdbcTemplate.queryForObject(
            "SELECT CURRENT_TIMESTAMP(6)",
            LocalDateTime::class.java,
        )!!
        val scheduleId = insertSchedule(databaseNow, databaseNow.plusSeconds(2), 10, 0, "FIRST")
        assertEquals(
            1L,
            jdbcTemplate.queryForObject(
                """
                SELECT CURRENT_TIMESTAMP(6) < booking_close_at
                FROM schedule
                WHERE id = ?
                """.trimIndent(),
                Long::class.java,
                scheduleId,
            ),
        )
        val lockAcquired = CountDownLatch(1)
        val executor: ExecutorService = Executors.newFixedThreadPool(2)

        try {
            val lockHolder: Future<*> = executor.submit(
                Runnable {
                    TransactionTemplate(transactionManager).executeWithoutResult {
                        scheduleRepository.lockById(scheduleId).orElseThrow()
                        lockAcquired.countDown()
                        holdLockPastCloseTime()
                    }
                },
            )
            assertTrue(lockAcquired.await(5, TimeUnit.SECONDS))

            val waitingRequest: Future<Boolean> = executor.submit(
                Callable {
                    TransactionTemplate(transactionManager).execute { _ ->
                        scheduleRepository.lockById(scheduleId).orElseThrow()
                        scheduleRepository.isBeforeBookingCloseAt(scheduleId)
                    }
                },
            )

            lockHolder.get(5, TimeUnit.SECONDS)
            assertFalse(waitingRequest.get(5, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }

    private fun insertSchedule(
        performanceDate: LocalDateTime,
        bookingCloseAt: LocalDateTime,
        totalTicketCount: Int,
        soldTicketCount: Int,
        scheduleNumber: String,
    ): Long {
        jdbcTemplate.update(
            """
            INSERT INTO schedule (
                performance_date,
                booking_close_at,
                total_ticket_count,
                sold_ticket_count,
                schedule_number,
                performance_id
            ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            performanceDate,
            bookingCloseAt,
            totalTicketCount,
            soldTicketCount,
            scheduleNumber,
            PERFORMANCE_ID,
        )
        return jdbcTemplate.queryForObject(
            "SELECT id FROM schedule WHERE performance_id = ? AND schedule_number = ?",
            Long::class.java,
            PERFORMANCE_ID,
            scheduleNumber,
        )!!
    }

    private fun holdLockPastCloseTime() {
        try {
            Thread.sleep(3_000)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException(exception)
        }
    }

    private companion object {
        const val PERFORMANCE_ID = 9_999_991L
    }
}
