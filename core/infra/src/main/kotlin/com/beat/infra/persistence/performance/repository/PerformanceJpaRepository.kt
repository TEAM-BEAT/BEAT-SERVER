package com.beat.infra.persistence.performance.repository

import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface PerformanceJpaRepository : JpaRepository<PerformanceJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Performance p WHERE p.id = :id")
    fun lockById(@Param("id") id: Long): PerformanceJpaEntity?
}
