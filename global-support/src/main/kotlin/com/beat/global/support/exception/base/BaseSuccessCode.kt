package com.beat.global.support.exception.base

interface BaseSuccessCode {
    fun getStatus(): Int

    fun getMessage(): String
}
