package com.beat.domain.cast.domain

import com.beat.domain.performance.domain.Performance

@ConsistentCopyVisibility
data class Cast private constructor(
    private val castId: Id?,
    val castName: String,
    val castRole: String,
    val castPhoto: String,
    private val linkedPerformanceId: Performance.Id,
) {
    fun getId(): Long? = castId?.value

    fun getPerformanceId(): Long = linkedPerformanceId.value

    fun update(castName: String, castRole: String, castPhoto: String): Cast = copy(
        castName = castName,
        castRole = castRole,
        castPhoto = castPhoto
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
        fun create(castName: String, castRole: String, castPhoto: String, performanceId: Long): Cast = Cast(
            castId = null,
            castName = castName,
            castRole = castRole,
            castPhoto = castPhoto,
            linkedPerformanceId = Performance.Id.from(performanceId)
        )

        @JvmStatic
        fun rehydrate(id: Long?, castName: String, castRole: String, castPhoto: String, performanceId: Long): Cast =
            Cast(
                castId = Id.fromNullable(id),
                castName = castName,
                castRole = castRole,
                castPhoto = castPhoto,
                linkedPerformanceId = Performance.Id.from(performanceId)
            )
    }
}
