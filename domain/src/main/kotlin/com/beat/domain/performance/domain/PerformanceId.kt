package com.beat.domain.performance.domain

@JvmInline
value class PerformanceId private constructor(val value: Long) {
    companion object {
        @JvmStatic
        fun of(value: Long?): PerformanceId? = value?.let(::PerformanceId)
    }
}
