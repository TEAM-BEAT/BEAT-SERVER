package com.beat.domain.exception

enum class DomainErrorType {
    INVALID_INPUT,
    STATE_CONFLICT,
}

interface DomainErrorCode {
    val code: String
    val type: DomainErrorType
    val message: String
}

class DomainException(
    val errorCode: DomainErrorCode,
) : RuntimeException(errorCode.message)
