package com.beat.global.support.response

interface SuccessCode {
    fun getStatus(): Int

    fun getMessage(): String
}
