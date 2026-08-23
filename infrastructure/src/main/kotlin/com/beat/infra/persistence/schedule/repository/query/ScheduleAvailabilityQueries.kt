package com.beat.infra.persistence.schedule.repository.query

import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReader
import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReadModel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
internal class ScheduleAvailabilityQueries(
    private val jdbcTemplate: JdbcTemplate,
) : PerformanceScheduleAvailabilityReader {

    override fun findAllByPerformanceId(
        performanceId: Long,
    ): List<PerformanceScheduleAvailabilityReadModel> =
        jdbcTemplate.query(QUERY, { resultSet, _ ->
            PerformanceScheduleAvailabilityReadModel(
                scheduleId = resultSet.getLong("schedule_id"),
                performanceDate = resultSet.getTimestamp("performance_date").toLocalDateTime(),
                scheduleNumber = resultSet.getString("schedule_number"),
                availableTicketCount = resultSet.getInt("available_ticket_count"),
                isBooking = resultSet.getBoolean("is_booking"),
                evaluatedAt = resultSet.getTimestamp("evaluated_at").toLocalDateTime(),
            )
        }, performanceId)

    private companion object {
        const val QUERY = """
            WITH query_clock AS (
                SELECT CURRENT_TIMESTAMP(6) AS evaluated_at
            )
            SELECT
                schedule.id AS schedule_id,
                schedule.performance_date,
                schedule.schedule_number,
                schedule.total_ticket_count - schedule.sold_ticket_count AS available_ticket_count,
                (
                    query_clock.evaluated_at < schedule.booking_close_at
                    AND schedule.sold_ticket_count < schedule.total_ticket_count
                ) AS is_booking,
                query_clock.evaluated_at
            FROM schedule
            CROSS JOIN query_clock
            WHERE schedule.performance_id = ?
            ORDER BY schedule.performance_date, schedule.id
        """
    }
}
