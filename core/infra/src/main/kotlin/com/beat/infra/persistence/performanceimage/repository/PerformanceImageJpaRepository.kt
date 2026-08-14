package com.beat.infra.persistence.performanceimage.repository

import com.beat.infra.persistence.performanceimage.entity.PerformanceImageJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PerformanceImageJpaRepository : JpaRepository<PerformanceImageJpaEntity, Long> {
    fun findAllByPerformanceId(performanceId: Long?): List<PerformanceImageJpaEntity>

    @Query("SELECT p.id FROM PerformanceImage p WHERE p.performanceId = :performanceId")
    fun findIdsByPerformanceId(@Param("performanceId") performanceId: Long?): List<Long>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PerformanceImage p WHERE p.performanceId = :performanceId")
    fun deleteByPerformanceId(@Param("performanceId") performanceId: Long?)
}
