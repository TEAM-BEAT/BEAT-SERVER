package com.beat.application.admin.exception

import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType
import com.beat.domain.exception.DomainException

internal fun <T> translateDomainFailure(block: () -> T): T =
    try {
        block()
    } catch (exception: DomainException) {
        throw AdminApplicationException(exception.errorCode.toAdminErrorCode(), exception)
    }

private fun DomainErrorCode.toAdminErrorCode(): AdminApplicationErrorCode {
    val type =
        when (this.type) {
            DomainErrorType.INVALID_INPUT -> AdminApplicationErrorType.INVALID_INPUT
            DomainErrorType.STATE_CONFLICT -> AdminApplicationErrorType.STATE_CONFLICT
        }
    return InternalAdminApplicationErrorCode(code, type, message)
}

internal data class InternalAdminApplicationErrorCode(
    override val code: String,
    override val type: AdminApplicationErrorType,
    override val message: String,
) : AdminApplicationErrorCode
