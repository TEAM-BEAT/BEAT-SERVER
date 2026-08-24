package com.beat.infrastructure.persistence.performance.repository.query

import com.beat.infrastructure.persistence.exception.PersistenceMappingException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PerformancePeriodReadSupportTest : FunSpec({

    test("정규화된 period 컬럼이 없으면 엄격한 레거시 period로 대체한다") {
        val period = resolvePerformancePeriod(
            performanceId = 1L,
            startDate = null,
            endDate = null,
            legacyPeriod = "2026.07.16~2026.07.18",
        )

        period.startDate shouldBe LocalDate.of(2026, 7, 16)
        period.endDate shouldBe LocalDate.of(2026, 7, 18)
    }

    test("부분적으로만 채워진 period 컬럼은 persistence mapping failure로 변환된다") {
        shouldThrow<PersistenceMappingException> {
            resolvePerformancePeriod(
                performanceId = 1L,
                startDate = LocalDate.of(2026, 7, 16),
                endDate = null,
                legacyPeriod = "2026.07.16",
            )
        }
    }

    test("형식이 잘못된 레거시 period는 persistence mapping failure로 변환된다") {
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
