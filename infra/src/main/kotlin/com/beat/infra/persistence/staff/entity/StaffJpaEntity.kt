package com.beat.infra.persistence.staff.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "Staff")
@Table(name = "staff")
class StaffJpaEntity private constructor(
    id: Long?,
    staffName: String,
    staffRole: String,
    staffPhoto: String,
    performanceId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(nullable = false)
    var staffName: String = staffName
        protected set

    @Column(nullable = false)
    var staffRole: String = staffRole
        protected set

    @Column(nullable = false)
    var staffPhoto: String = staffPhoto
        protected set

    @Column(name = "performance_id", nullable = false)
    var performanceId: Long = performanceId
        protected set

    companion object {
        @JvmStatic
        fun rehydrate(
            id: Long?,
            staffName: String,
            staffRole: String,
            staffPhoto: String,
            performanceId: Long,
        ): StaffJpaEntity = StaffJpaEntity(
            id = id,
            staffName = staffName,
            staffRole = staffRole,
            staffPhoto = staffPhoto,
            performanceId = performanceId,
        )
    }
}
