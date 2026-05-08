package com.beat.global.support.exception.base

interface BaseErrorCode {
    fun getStatus(): Int

    fun getMessage(): String
}
