package com.beat.application.frontoffice.performance.maker.command

import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class ImagePresignedUpload private constructor(
    val uploadUrl: String,
    val imageKey: String,
) {
    companion object {
        fun of(uploadUrl: String, imageKey: String): ImagePresignedUpload =
            ImagePresignedUpload(uploadUrl, imageKey)
    }
}
