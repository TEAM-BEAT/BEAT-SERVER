package com.beat.global.support.exception

import com.beat.global.support.exception.base.BaseErrorCode

class UnauthorizedException(
    baseErrorCode: BaseErrorCode,
) : BeatException(baseErrorCode)
