package com.beat.apps.api.performance.api.request


data class CastModifyRequest(
    val castId: Long?,
    val castName: String,
    val castRole: String,
    val castPhoto: String,
)
