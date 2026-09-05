package com.beat.infrastructure.booking.booker.experiment

import com.beat.application.frontoffice.booking.booker.experiment.ScheduleBookingMetadata
import com.beat.application.frontoffice.booking.booker.experiment.ScheduleStockState
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentEnabled
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionScheduleStore
import java.sql.ResultSet
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class JdbcStockContentionScheduleStore(private val jdbcTemplate: JdbcTemplate) :
    StockContentionScheduleStore {
    /**
     * Resolves the schedule link and booking window without acquiring a row lock. Inventory columns
     * are intentionally not read here; Atomic can keep its conditional UPDATE as the first stock
     * operation after this common metadata validation.
     */
    override fun findBookingMetadataById(scheduleId: Long): ScheduleBookingMetadata? =
        jdbcTemplate
            .query(
                """
                SELECT performance_id,
                       CASE
                           WHEN CURRENT_TIMESTAMP(6) < booking_close_at THEN 1
                           ELSE 0
                       END AS booking_open
                FROM schedule
                WHERE id = ?
                """
                    .trimIndent(),
                { resultSet: ResultSet, _: Int ->
                    ScheduleBookingMetadata(
                        performanceId = resultSet.getLong("performance_id"),
                        bookingOpen = resultSet.getInt("booking_open") == 1,
                    )
                },
                scheduleId,
            )
            .firstOrNull()

    override fun find(
        scheduleId: Long,
        forUpdate: Boolean,
        withVersion: Boolean,
    ): ScheduleStockState? {
        val versionColumn = if (withVersion) ", version" else ""
        val lockClause = if (forUpdate) " FOR UPDATE" else ""
        val sql =
            """
            SELECT id,
                   performance_id,
                   performance_date,
                   booking_close_at,
                   total_ticket_count,
                   sold_ticket_count,
                   schedule_number,
                   CASE WHEN CURRENT_TIMESTAMP(6) < booking_close_at THEN 1 ELSE 0 END AS booking_open
                   $versionColumn
            FROM schedule
            WHERE id = ?$lockClause
            """
                .trimIndent()
        return jdbcTemplate.query(sql, rowMapper(withVersion), scheduleId).firstOrNull()
    }

    override fun reserveWithPessimisticLock(scheduleId: Long, ticketCount: Int): Int =
        jdbcTemplate.update(
            """
            UPDATE schedule
            SET sold_ticket_count = sold_ticket_count + ?
            WHERE id = ?
              AND CURRENT_TIMESTAMP(6) < booking_close_at
              AND sold_ticket_count + ? <= total_ticket_count
            """
                .trimIndent(),
            ticketCount,
            scheduleId,
            ticketCount,
        )

    override fun reserveWithOptimisticCas(
        scheduleId: Long,
        ticketCount: Int,
        version: Long,
    ): Int =
        jdbcTemplate.update(
            """
            UPDATE schedule
            SET sold_ticket_count = sold_ticket_count + ?,
                version = version + 1
            WHERE id = ?
              AND version = ?
              AND CURRENT_TIMESTAMP(6) < booking_close_at
              AND sold_ticket_count + ? <= total_ticket_count
            """
                .trimIndent(),
            ticketCount,
            scheduleId,
            version,
            ticketCount,
        )

    override fun reserveWithAtomicUpdate(scheduleId: Long, ticketCount: Int): Int =
        jdbcTemplate.update(
            """
            UPDATE schedule
            SET sold_ticket_count = sold_ticket_count + ?
            WHERE id = ?
              AND CURRENT_TIMESTAMP(6) < booking_close_at
              AND sold_ticket_count + ? <= total_ticket_count
            """
                .trimIndent(),
            ticketCount,
            scheduleId,
            ticketCount,
        )

    /**
     * Increments stock while the caller owns the Redis schedule lock. The read/availability check
     * is performed immediately before this update under that owner-token lock. The conditional
     * capacity and deadline predicates remain a database safety net, but this is intentionally a
     * separate ordinary increment operation from the Atomic strategy's reservation method.
     */
    override fun reserveWithRedisLock(scheduleId: Long, ticketCount: Int): Int =
        jdbcTemplate.update(
            """
            UPDATE schedule
            SET sold_ticket_count = sold_ticket_count + ?
            WHERE id = ?
              AND CURRENT_TIMESTAMP(6) < booking_close_at
              AND sold_ticket_count + ? <= total_ticket_count
            """
                .trimIndent(),
            ticketCount,
            scheduleId,
            ticketCount,
        )

    private fun rowMapper(withVersion: Boolean): RowMapper<ScheduleStockState> =
        RowMapper { resultSet: ResultSet, _: Int ->
            ScheduleStockState(
                id = resultSet.getLong("id"),
                performanceId = resultSet.getLong("performance_id"),
                performanceDate =
                    resultSet.getObject("performance_date", java.time.LocalDateTime::class.java),
                bookingCloseAt =
                    resultSet.getObject("booking_close_at", java.time.LocalDateTime::class.java),
                totalTicketCount = resultSet.getInt("total_ticket_count"),
                soldTicketCount = resultSet.getInt("sold_ticket_count"),
                scheduleNumber = resultSet.getString("schedule_number"),
                bookingOpen = resultSet.getInt("booking_open") == 1,
                version =
                    if (withVersion) {
                        resultSet.getObject("version", Long::class.javaObjectType)
                    } else {
                        null
                    },
            )
        }
}
