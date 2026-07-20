package com.beat.admin.exception

class AdminApplicationException @JvmOverloads constructor(
    val errorCode: ApplicationErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.getMessage(), cause)
