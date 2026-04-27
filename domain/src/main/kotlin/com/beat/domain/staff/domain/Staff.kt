package com.beat.domain.staff.domain

import com.beat.domain.performance.domain.PerformanceId
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class Staff private constructor(
    private val staffId: Id?,
    val staffName: String,
    val staffRole: String,
    val staffPhoto: String,
    private val linkedPerformanceId: PerformanceId,
) {
    fun getId(): Long? = staffId?.value

    fun getPerformanceId(): Long = linkedPerformanceId.value

    fun update(staffName: String, staffRole: String, staffPhoto: String): Staff = copy(
        staffName = staffName,
        staffRole = staffRole,
        staffPhoto = staffPhoto
    )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            @JvmStatic
            fun from(value: Long): Id = Id(value)

            @JvmStatic
            fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
        @JvmStatic
        fun create(staffName: String, staffRole: String, staffPhoto: String, performanceId: Long): Staff = Staff(
            staffId = null,
            staffName = staffName,
            staffRole = staffRole,
            staffPhoto = staffPhoto,
            linkedPerformanceId = PerformanceId.from(performanceId)
        )

        @JvmStatic
        fun rehydrate(id: Long?, staffName: String, staffRole: String, staffPhoto: String, performanceId: Long): Staff = Staff(
            staffId = Id.fromNullable(id),
            staffName = staffName,
            staffRole = staffRole,
            staffPhoto = staffPhoto,
            linkedPerformanceId = PerformanceId.from(performanceId)
        )
    }
}
