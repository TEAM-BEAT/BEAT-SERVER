package com.beat.domain.performance.domain

@JvmInline
value class PerformanceId private constructor(val value: Long) {
    companion object {
        @JvmStatic
        fun from(value: Long): PerformanceId = PerformanceId(value)

        @JvmStatic
        fun fromNullable(value: Long?): PerformanceId? = value?.let(::from)
    }
}
