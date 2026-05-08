package com.beat.global.support.exception

import com.beat.global.support.exception.base.BaseErrorCode

open class BeatException(
    val baseErrorCode: BaseErrorCode,
) : RuntimeException(baseErrorCode.getMessage())
