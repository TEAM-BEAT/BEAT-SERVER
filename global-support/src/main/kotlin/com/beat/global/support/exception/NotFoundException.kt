package com.beat.global.support.exception

import com.beat.global.support.exception.base.BaseErrorCode

class NotFoundException(
    baseErrorCode: BaseErrorCode,
) : BeatException(baseErrorCode)
