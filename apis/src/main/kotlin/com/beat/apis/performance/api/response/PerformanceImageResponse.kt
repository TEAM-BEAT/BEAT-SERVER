package com.beat.apis.performance.api.response

import com.beat.global.support.jackson.CdnImageUrl

data class PerformanceImageResponse(
    val imageId: Long?, @field:CdnImageUrl val imageUrl: String?,
)
