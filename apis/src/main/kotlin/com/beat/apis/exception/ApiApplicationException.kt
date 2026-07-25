package com.beat.apis.exception

class ApiApplicationException(
    val errorCode: ApplicationErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.getMessage(), cause)
