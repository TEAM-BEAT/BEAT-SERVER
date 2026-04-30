package com.beat.apis.performance.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class StaffApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    STAFF_NOT_FOUND(404, "스태프가 존재하지 않습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
