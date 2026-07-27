package com.beat.contracts.storage

import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class CarouselPresignedUpload private constructor(
    val uploadUrl: String,
    val imageKey: String,
) {
    companion object {
        @JvmStatic
        fun of(uploadUrl: String, imageKey: String): CarouselPresignedUpload =
            CarouselPresignedUpload(uploadUrl, imageKey)
    }
}
