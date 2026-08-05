package com.beat.infra.persistence.performance.repository.query

import com.beat.infra.persistence.exception.PersistenceMappingException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PerformancePeriodReadSupportTest {

    @Test
    fun `falls back to the strict legacy period when normalized columns are absent`() {
        val period = resolvePerformancePeriod(
            performanceId = 1L,
            startDate = null,
            endDate = null,
            legacyPeriod = "2026.07.16~2026.07.18",
        )

        assertEquals(LocalDate.of(2026, 7, 16), period.startDate)
        assertEquals(LocalDate.of(2026, 7, 18), period.endDate)
    }

    @Test
    fun `translates partially populated period columns to persistence mapping failure`() {
        assertThrows(PersistenceMappingException::class.java) {
            resolvePerformancePeriod(
                performanceId = 1L,
                startDate = LocalDate.of(2026, 7, 16),
                endDate = null,
                legacyPeriod = "2026.07.16",
            )
        }
    }

    @Test
    fun `translates malformed legacy period to persistence mapping failure`() {
        assertThrows(PersistenceMappingException::class.java) {
            resolvePerformancePeriod(
                performanceId = 1L,
                startDate = null,
                endDate = null,
                legacyPeriod = "not-a-period",
            )
        }
    }
}
