package com.beat.domain.performance.domain

import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class PerformanceImage private constructor(
    private val imageId: Id?,
    val performanceImageUrl: String,
    private val linkedPerformanceId: PerformanceId,
) {
    fun getId(): Long? = imageId?.value

    fun getPerformanceId(): Long = linkedPerformanceId.value

    fun update(performanceImageUrl: String): PerformanceImage = copy(
        performanceImageUrl = performanceImageUrl
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
        fun create(performanceImageUrl: String, performanceId: Long): PerformanceImage = PerformanceImage(
            imageId = null,
            performanceImageUrl = performanceImageUrl,
            linkedPerformanceId = PerformanceId.from(performanceId)
        )

        @JvmStatic
        fun rehydrate(id: Long?, performanceImageUrl: String, performanceId: Long): PerformanceImage = PerformanceImage(
            imageId = Id.fromNullable(id),
            performanceImageUrl = performanceImageUrl,
            linkedPerformanceId = PerformanceId.from(performanceId)
        )
    }
}
