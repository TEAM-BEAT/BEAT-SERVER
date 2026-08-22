package com.beat.infra.persistence.performance.repository.query

import com.beat.infra.persistence.exception.PersistenceMappingException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PerformancePeriodReadSupportTest : FunSpec({

    test("falls back to the strict legacy period when normalized columns are absent") {
        val period = resolvePerformancePeriod(
            performanceId = 1L,
            startDate = null,
            endDate = null,
            legacyPeriod = "2026.07.16~2026.07.18",
        )

        period.startDate shouldBe LocalDate.of(2026, 7, 16)
        period.endDate shouldBe LocalDate.of(2026, 7, 18)
    }

    test("translates partially populated period columns to persistence mapping failure") {
        shouldThrow<PersistenceMappingException> {
            resolvePerformancePeriod(
                performanceId = 1L,
                startDate = LocalDate.of(2026, 7, 16),
                endDate = null,
                legacyPeriod = "2026.07.16",
            )
        }
    }

    test("translates malformed legacy period to persistence mapping failure") {
        shouldThrow<PersistenceMappingException> {
            resolvePerformancePeriod(
                performanceId = 1L,
                startDate = null,
                endDate = null,
                legacyPeriod = "not-a-period",
            )
        }
    }
})
