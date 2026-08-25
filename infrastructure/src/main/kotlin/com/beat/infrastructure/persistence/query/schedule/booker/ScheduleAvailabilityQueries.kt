package com.beat.infrastructure.persistence.query.schedule.booker

import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReader
import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReadModel
import com.beat.infrastructure.jooq.generated.Schedule
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class ScheduleAvailabilityQueries(
    private val dsl: DSLContext,
) : PerformanceScheduleAvailabilityReader {

    override fun findAllByPerformanceId(
        performanceId: Long,
    ): List<PerformanceScheduleAvailabilityReadModel> {
        val evaluatedAtField = DSL.field("CURRENT_TIMESTAMP(6)", LocalDateTime::class.java).`as`("evaluated_at")
        val availableTicketCountField = Schedule.TOTAL_TICKET_COUNT.minus(Schedule.SOLD_TICKET_COUNT).`as`("available_ticket_count")
        val isBookingField = DSL.field(
            "CURRENT_TIMESTAMP(6) < {0} AND {1} < {2}",
            Boolean::class.java,
            Schedule.BOOKING_CLOSE_AT,
            Schedule.SOLD_TICKET_COUNT,
            Schedule.TOTAL_TICKET_COUNT,
        ).`as`("is_booking")

        return dsl.select(
            Schedule.ID.`as`("schedule_id"),
            Schedule.PERFORMANCE_DATE,
            Schedule.SCHEDULE_NUMBER,
            availableTicketCountField,
            isBookingField,
            evaluatedAtField,
        ).from(Schedule.TABLE)
            .where(Schedule.PERFORMANCE_ID.eq(performanceId))
            .orderBy(Schedule.PERFORMANCE_DATE.asc(), Schedule.ID.asc())
            .fetch { record ->
                PerformanceScheduleAvailabilityReadModel(
                    scheduleId = record.get("schedule_id", Long::class.java)!!,
                    performanceDate = record.get(Schedule.PERFORMANCE_DATE)!!,
                    scheduleNumber = record.get(Schedule.SCHEDULE_NUMBER)!!,
                    availableTicketCount = record.get("available_ticket_count", Int::class.java)!!,
                    isBooking = record.get("is_booking", Boolean::class.java)!!,
                    evaluatedAt = record.get("evaluated_at", LocalDateTime::class.java)!!,
                )
            }
    }
}
