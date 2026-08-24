package com.beat.infrastructure.persistence.cast.repository

import com.beat.infrastructure.persistence.cast.entity.CastJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface CastJpaRepository : JpaRepository<CastJpaEntity, Long> {
    fun findAllByPerformanceId(performanceId: Long): List<CastJpaEntity>

    @Query("SELECT c.id FROM Cast c WHERE c.performanceId = :performanceId")
    fun findIdsByPerformanceId(@Param("performanceId") performanceId: Long): List<Long>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Cast c WHERE c.performanceId = :performanceId")
    fun deleteByPerformanceId(@Param("performanceId") performanceId: Long)
}
