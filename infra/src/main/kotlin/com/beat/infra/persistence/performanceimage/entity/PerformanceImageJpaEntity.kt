package com.beat.infra.persistence.performanceimage.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "PerformanceImage")
@Table(name = "performance_image")
class PerformanceImageJpaEntity private constructor(
    id: Long?,
    performanceImageUrl: String,
    performanceId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(name = "performance_image_url", nullable = false)
    var performanceImageUrl: String = performanceImageUrl
        protected set

    @Column(name = "performance_id", nullable = false)
    var performanceId: Long = performanceId
        protected set

    companion object {
        @JvmStatic
        fun rehydrate(
            id: Long?,
            performanceImageUrl: String,
            performanceId: Long,
        ): PerformanceImageJpaEntity = PerformanceImageJpaEntity(
            id = id,
            performanceImageUrl = performanceImageUrl,
            performanceId = performanceId,
        )
    }
}
