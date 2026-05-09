package com.beat.global.support.exception

import com.beat.global.support.exception.base.BaseErrorCode

class BadRequestException(
    baseErrorCode: BaseErrorCode,
) : BeatException(baseErrorCode)
