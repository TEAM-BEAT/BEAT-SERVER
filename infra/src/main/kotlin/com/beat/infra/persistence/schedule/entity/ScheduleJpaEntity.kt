package com.beat.infra.persistence.schedule.entity

import com.beat.domain.schedule.domain.ScheduleNumber
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity(name = "Schedule")
@Table(name = "schedule")
class ScheduleJpaEntity private constructor(
    id: Long?,
    performanceDate: LocalDateTime,
    totalTicketCount: Int,
    soldTicketCount: Int,
    isBooking: Boolean,
    scheduleNumber: ScheduleNumber,
    performanceId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(nullable = false)
    var performanceDate: LocalDateTime = performanceDate
        protected set

    @Column(nullable = false)
    var totalTicketCount: Int = totalTicketCount
        protected set

    @Column(nullable = false)
    var soldTicketCount: Int = soldTicketCount
        protected set

    @Column(nullable = false)
    var isBooking: Boolean = isBooking
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var scheduleNumber: ScheduleNumber = scheduleNumber
        protected set

    @Column(name = "performance_id", nullable = false)
    var performanceId: Long = performanceId
        protected set

    companion object {
        @JvmStatic
        fun rehydrate(
            id: Long?,
            performanceDate: LocalDateTime,
            totalTicketCount: Int,
            soldTicketCount: Int,
            isBooking: Boolean,
            scheduleNumber: ScheduleNumber,
            performanceId: Long,
        ): ScheduleJpaEntity = ScheduleJpaEntity(
            id = id,
            performanceDate = performanceDate,
            totalTicketCount = totalTicketCount,
            soldTicketCount = soldTicketCount,
            isBooking = isBooking,
            scheduleNumber = scheduleNumber,
            performanceId = performanceId,
        )
    }
}
