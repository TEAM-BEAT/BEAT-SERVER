package com.beat.application.admin.exception

import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType
import com.beat.domain.exception.DomainException
import com.beat.domain.promotion.exception.PromotionErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DomainFailureTranslatorSpec : FunSpec({
    test("domain failure를 원인과 domain code를 보존한 채 변환한다") {
        val domainFailure = DomainException(PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS)

        val applicationFailure = shouldThrow<AdminApplicationException> {
            translateDomainFailure<Unit> { throw domainFailure }
        }

        applicationFailure.cause shouldBe domainFailure
        applicationFailure.errorCode.code shouldBe PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS.code
        applicationFailure.errorCode.type shouldBe AdminApplicationErrorType.INVALID_INPUT
        applicationFailure.errorCode.message shouldBe PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS.message
    }

    test("domain message는 유지한 채 domain 에러 타입을 매핑한다") {
        assertTranslation(
            TestDomainErrorCode("INVALID", DomainErrorType.INVALID_INPUT, "invalid"),
            AdminApplicationErrorType.INVALID_INPUT,
            "invalid",
        )
        assertTranslation(
            TestDomainErrorCode("CONFLICT", DomainErrorType.STATE_CONFLICT, "conflict"),
            AdminApplicationErrorType.STATE_CONFLICT,
            "conflict",
        )
    }
})

private fun assertTranslation(
    errorCode: DomainErrorCode,
    expectedType: AdminApplicationErrorType,
    expectedMessage: String,
) {
    val applicationFailure = shouldThrow<AdminApplicationException> {
        translateDomainFailure<Unit> { throw DomainException(errorCode) }
    }

    applicationFailure.errorCode.code shouldBe errorCode.code
    applicationFailure.errorCode.type shouldBe expectedType
    applicationFailure.errorCode.message shouldBe expectedMessage
}

private data class TestDomainErrorCode(
    override val code: String,
    override val type: DomainErrorType,
    override val message: String,
) : DomainErrorCode
