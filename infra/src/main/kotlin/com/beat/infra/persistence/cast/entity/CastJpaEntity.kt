package com.beat.infra.persistence.cast.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "Cast")
@Table(name = "cast")
class CastJpaEntity private constructor(
    id: Long?,
    castName: String,
    castRole: String,
    castPhoto: String,
    performanceId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(nullable = false)
    var castName: String = castName
        protected set

    @Column(nullable = false)
    var castRole: String = castRole
        protected set

    @Column(nullable = false)
    var castPhoto: String = castPhoto
        protected set

    @Column(name = "performance_id", nullable = false)
    var performanceId: Long = performanceId
        protected set

    companion object {
        @JvmStatic
        fun rehydrate(
            id: Long?,
            castName: String,
            castRole: String,
            castPhoto: String,
            performanceId: Long,
        ): CastJpaEntity = CastJpaEntity(
            id = id,
            castName = castName,
            castRole = castRole,
            castPhoto = castPhoto,
            performanceId = performanceId,
        )
    }
}
