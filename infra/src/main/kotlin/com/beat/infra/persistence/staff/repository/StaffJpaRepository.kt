package com.beat.infra.persistence.staff.repository

import com.beat.infra.persistence.staff.entity.StaffJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StaffJpaRepository : JpaRepository<StaffJpaEntity, Long> {
    fun findAllByPerformanceId(performanceId: Long?): List<StaffJpaEntity>

    @Query("SELECT s.id FROM Staff s WHERE s.performanceId = :performanceId")
    fun findIdsByPerformanceId(@Param("performanceId") performanceId: Long?): List<Long>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Staff s WHERE s.performanceId = :performanceId")
    fun deleteByPerformanceId(@Param("performanceId") performanceId: Long?)
}
