package com.beat.domain.performance.model

class PerformanceImage
private constructor(
    private val imageId: Id?,
    val performanceImageUrl: String,
) {
    val id: Long?
        get() = imageId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerformanceImage) return false
        return imageId != null && imageId == other.imageId
    }

    override fun hashCode(): Int = imageId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "PerformanceImage(id=$id)"

    fun update(performanceImageUrl: String): PerformanceImage =
        PerformanceImage(
            imageId = imageId,
            performanceImageUrl = performanceImageUrl,
        )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            fun fromNullable(value: Long?): Id? = value?.let(::Id)
        }
    }

    companion object {
        fun create(performanceImageUrl: String): PerformanceImage =
            PerformanceImage(
                imageId = null,
                performanceImageUrl = performanceImageUrl,
            )

        fun rehydrate(id: Long?, performanceImageUrl: String): PerformanceImage =
            PerformanceImage(
                imageId = Id.fromNullable(id),
                performanceImageUrl = performanceImageUrl,
            )
    }
}
