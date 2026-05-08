package com.beat.apis.user.application.exception

import com.beat.global.support.exception.base.BaseErrorCode

enum class UserApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    USER_NOT_FOUND(404, "유저가 없습니다")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
