package com.beat.infra.persistence.performance.repository.query

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.infra.persistence.exception.PersistenceMappingException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

internal fun resolvePerformancePeriod(
    performanceId: Long,
    startDate: LocalDate?,
    endDate: LocalDate?,
    legacyPeriod: String?,
): PerformancePeriod {
    try {
        if (startDate != null && endDate != null) {
            return PerformancePeriod.of(startDate, endDate)
        }
        check(startDate == null && endDate == null) {
            "Performance period columns must be both null or both non-null"
        }
        return parseLegacyPerformancePeriod(legacyPeriod)
    } catch (exception: DomainException) {
        throw PersistenceMappingException.invalidStoredState("Performance", performanceId, exception)
    } catch (exception: IllegalStateException) {
        throw PersistenceMappingException.invalidStoredState("Performance", performanceId, exception)
    }
}

private fun parseLegacyPerformancePeriod(legacyPeriod: String?): PerformancePeriod {
    check(!legacyPeriod.isNullOrBlank()) { INVALID_LEGACY_PERIOD_MESSAGE }

    val dates = legacyPeriod.split("~", limit = 3)
    check(dates.size <= 2) { INVALID_LEGACY_PERIOD_MESSAGE }

    try {
        val startDate = LocalDate.parse(dates.first(), LEGACY_DATE_FORMATTER)
        val endDate = if (dates.size == 1) {
            startDate
        } else {
            LocalDate.parse(dates.last(), LEGACY_DATE_FORMATTER)
        }
        return PerformancePeriod.of(startDate, endDate)
    } catch (exception: DateTimeParseException) {
        throw IllegalStateException(INVALID_LEGACY_PERIOD_MESSAGE, exception)
    }
}

private const val INVALID_LEGACY_PERIOD_MESSAGE = "Invalid legacy performance period"

private val LEGACY_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu.MM.dd").withResolverStyle(ResolverStyle.STRICT)
