package com.beat.apis.schedule

import com.beat.apis.support.BeatTestContainersConfig
import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReadModel
import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReader
import com.beat.domain.schedule.repository.ScheduleRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

@SpringBootTest
@ActiveProfiles("test")
@Import(BeatTestContainersConfig::class)
@Tags("integration", "correctness")
open class ScheduleBookingAvailabilityIntegrationTest : FunSpec() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var scheduleAvailabilityReader: PerformanceScheduleAvailabilityReader

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        afterTest {
            jdbcTemplate.update("DELETE FROM schedule WHERE performance_id = ?", PERFORMANCE_ID)
        }

        test("availability 조회는 모든 회차에 하나의 데이터베이스 시계를 사용한다") {
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

            schedules.size shouldBe 3
            schedulesByNumber.getValue("FIRST").isBooking shouldBe true
            schedulesByNumber.getValue("SECOND").isBooking shouldBe false
            schedulesByNumber.getValue("THIRD").isBooking shouldBe false
            schedules.map { it.evaluatedAt }.distinct().count().toLong() shouldBe 1L

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
            extendedSchedules.getValue("SECOND").isBooking shouldBe true
        }

        test("REPEATABLE READ에서 lock 대기 후 예매 마감 시각을 다시 확인한다") {
            jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String::class.java) shouldBe
                "REPEATABLE-READ"
            val databaseNow = jdbcTemplate.queryForObject(
                "SELECT CURRENT_TIMESTAMP(6)",
                LocalDateTime::class.java,
            )!!
            val scheduleId = insertSchedule(databaseNow, databaseNow.plusSeconds(1), 10, 0, "FIRST")
            jdbcTemplate.queryForObject(
                """
                SELECT CURRENT_TIMESTAMP(6) < booking_close_at
                FROM schedule
                WHERE id = ?
                """.trimIndent(),
                Long::class.java,
                scheduleId,
            ) shouldBe 1L
            val lockAcquired = CountDownLatch(1)
            val waitingRequestStarted = CountDownLatch(1)
            val releaseLock = CountDownLatch(1)
            val executor: ExecutorService = Executors.newFixedThreadPool(2)

            try {
                val lockHolder: Future<*> = executor.submit(
                    Runnable {
                        TransactionTemplate(transactionManager).executeWithoutResult {
                            checkNotNull(scheduleRepository.lockById(scheduleId))
                            lockAcquired.countDown()
                            check(releaseLock.await(5, TimeUnit.SECONDS))
                        }
                    },
                )
                lockAcquired.await(5, TimeUnit.SECONDS) shouldBe true

                val waitingRequest: Future<Boolean> = executor.submit(
                    Callable {
                        waitingRequestStarted.countDown()
                        TransactionTemplate(transactionManager).execute { _ ->
                            checkNotNull(scheduleRepository.lockById(scheduleId))
                            scheduleRepository.isBeforeBookingCloseAt(scheduleId)
                        }
                    },
                )

                waitingRequestStarted.await(5, TimeUnit.SECONDS) shouldBe true
                awaitBookingCloseAt(scheduleId)
                releaseLock.countDown()
                lockHolder.get(5, TimeUnit.SECONDS)
                waitingRequest.get(5, TimeUnit.SECONDS) shouldBe false
            } finally {
                releaseLock.countDown()
                executor.shutdownNow()
            }
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

    private fun awaitBookingCloseAt(scheduleId: Long) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            val isClosed = jdbcTemplate.queryForObject(
                "SELECT CURRENT_TIMESTAMP(6) >= booking_close_at FROM schedule WHERE id = ?",
                Long::class.java,
                scheduleId,
            ) == 1L
            if (isClosed) {
                return
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10))
        }
        error("booking_close_at did not elapse within 5 seconds")
    }

    private companion object {
        const val PERFORMANCE_ID = 9_999_991L
    }
}
