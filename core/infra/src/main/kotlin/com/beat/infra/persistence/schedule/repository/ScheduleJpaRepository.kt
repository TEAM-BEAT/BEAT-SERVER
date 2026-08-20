package com.beat.infra.persistence.schedule.repository

import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ScheduleJpaRepository : JpaRepository<ScheduleJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    fun lockById(@Param("id") id: Long?): Optional<ScheduleJpaEntity>

    @Query("SELECT s.performanceId FROM Schedule s WHERE s.id = :id")
    fun findPerformanceIdById(@Param("id") id: Long): Long?

    @Query(
        value = """
            SELECT CURRENT_TIMESTAMP(6) < booking_close_at
            FROM schedule
            WHERE id = :id
            FOR UPDATE
        """,
        nativeQuery = true,
    )
    fun isBeforeBookingCloseAt(@Param("id") id: Long?): Long

    fun findAllByPerformanceId(performanceId: Long?): List<ScheduleJpaEntity>

    @Query("SELECT s.id FROM Schedule s WHERE s.performanceId = :performanceId")
    fun findIdsByPerformanceId(@Param("performanceId") performanceId: Long?): List<Long>

    fun countByPerformanceId(performanceId: Long?): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Schedule s WHERE s.performanceId = :performanceId")
    fun deleteByPerformanceId(@Param("performanceId") performanceId: Long?)
}
