package com.beat.global.support.exception

import com.beat.global.support.exception.base.BaseErrorCode

class ForbiddenException(
    baseErrorCode: BaseErrorCode,
) : BeatException(baseErrorCode)
