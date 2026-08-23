package com.beat.apis.performance.api.request


data class StaffModifyRequest(
    val staffId: Long?,
    val staffName: String,
    val staffRole: String,
    val staffPhoto: String,
)
