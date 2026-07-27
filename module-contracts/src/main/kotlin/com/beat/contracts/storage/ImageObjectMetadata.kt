package com.beat.contracts.storage

import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class ImageObjectMetadata private constructor(
    val contentType: String?,
    val contentLength: Long,
) {
    companion object {
        @JvmStatic
        fun of(contentType: String?, contentLength: Long): ImageObjectMetadata =
            ImageObjectMetadata(contentType, contentLength)
    }
}
