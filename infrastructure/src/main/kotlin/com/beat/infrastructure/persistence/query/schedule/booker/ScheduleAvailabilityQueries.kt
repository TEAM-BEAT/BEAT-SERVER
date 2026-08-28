package com.beat.infrastructure.persistence.query.schedule.booker

import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReadModel
import com.beat.application.frontoffice.performance.booker.query.PerformanceScheduleAvailabilityReader
import com.beat.infrastructure.jooq.generated.Schedule
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
internal class ScheduleAvailabilityQueries(private val dsl: DSLContext) :
    PerformanceScheduleAvailabilityReader {

    override fun findAllByPerformanceId(
        performanceId: Long
    ): List<PerformanceScheduleAvailabilityReadModel> {
        val scheduleIdField = Schedule.ID.`as`("schedule_id")
        val evaluatedAtField =
            DSL.field("CURRENT_TIMESTAMP(6)", LocalDateTime::class.java).`as`("evaluated_at")
        val availableTicketCountField =
            Schedule.TOTAL_TICKET_COUNT.minus(Schedule.SOLD_TICKET_COUNT)
                .`as`("available_ticket_count")
        val isBookingField =
            DSL.field(
                    "CURRENT_TIMESTAMP(6) < {0} AND {1} < {2}",
                    Boolean::class.java,
                    Schedule.BOOKING_CLOSE_AT,
                    Schedule.SOLD_TICKET_COUNT,
                    Schedule.TOTAL_TICKET_COUNT,
                )
                .`as`("is_booking")

        return dsl.select(
                scheduleIdField,
                Schedule.PERFORMANCE_DATE,
                Schedule.SCHEDULE_NUMBER,
                availableTicketCountField,
                isBookingField,
                evaluatedAtField,
            )
            .from(Schedule.TABLE)
            .where(Schedule.PERFORMANCE_ID.eq(performanceId))
            .orderBy(Schedule.PERFORMANCE_DATE.asc(), Schedule.ID.asc())
            .fetch { record ->
                PerformanceScheduleAvailabilityReadModel(
                    scheduleId = record[scheduleIdField]!!,
                    performanceDate = record[Schedule.PERFORMANCE_DATE]!!,
                    scheduleNumber = record[Schedule.SCHEDULE_NUMBER]!!,
                    availableTicketCount = record[availableTicketCountField]!!,
                    isBooking = record[isBookingField]!!,
                    evaluatedAt = record[evaluatedAtField]!!,
                )
            }
    }
}
