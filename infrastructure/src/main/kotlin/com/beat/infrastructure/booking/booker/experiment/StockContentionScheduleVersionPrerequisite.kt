package com.beat.infrastructure.booking.booker.experiment

import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentEnabled
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@Profile("dev & !prod")
@StockContentionExperimentEnabled
class StockContentionScheduleVersionPrerequisite(private val jdbcTemplate: JdbcTemplate) {
    init {
        check(hasCompatibleVersionColumn()) { MIGRATION_REQUIRED_MESSAGE }
    }

    private fun hasCompatibleVersionColumn(): Boolean =
        jdbcTemplate.queryForObject(COMPATIBILITY_QUERY, Int::class.java) == 1

    private companion object {
        const val COMPATIBILITY_QUERY =
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'schedule'
              AND column_name = 'version'
              AND data_type = 'bigint'
              AND is_nullable = 'NO'
              AND (column_default = '0' OR column_default = 0)
            """

        const val MIGRATION_REQUIRED_MESSAGE =
            "Stock contention experiment requires the beatDev migration: " +
                "schedule.version must be BIGINT NOT NULL DEFAULT 0. " +
                "Run scripts/apply-local-dev-booking-migration.sh before enabling " +
                "BOOKING_EXPERIMENT_ENABLED."
    }
}
