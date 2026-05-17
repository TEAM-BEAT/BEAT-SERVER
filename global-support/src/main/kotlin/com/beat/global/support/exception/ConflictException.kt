package com.beat.global.support.exception

import com.beat.global.support.exception.base.BaseErrorCode

class ConflictException(
    baseErrorCode: BaseErrorCode,
) : BeatException(baseErrorCode)
