package com.beat.contracts.storage

import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class ImagePresignedUpload private constructor(
    val uploadUrl: String,
    val imageKey: String,
) {
    companion object {
        @JvmStatic
        fun of(uploadUrl: String, imageKey: String): ImagePresignedUpload =
            ImagePresignedUpload(uploadUrl, imageKey)
    }
}
