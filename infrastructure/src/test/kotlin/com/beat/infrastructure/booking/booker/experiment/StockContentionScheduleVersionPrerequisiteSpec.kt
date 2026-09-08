package com.beat.infrastructure.booking.booker.experiment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.jdbc.core.JdbcTemplate

class StockContentionScheduleVersionPrerequisiteSpec : FunSpec() {
    init {
        test("호환되는 schedule.version이 있으면 prerequisite 검증을 통과한다") {
            val jdbcTemplate = mockk<JdbcTemplate>()
            every { jdbcTemplate.queryForObject(any<String>(), Int::class.java) } returns 1

            StockContentionScheduleVersionPrerequisite(jdbcTemplate)

            verify(exactly = 1) {
                jdbcTemplate.queryForObject(any<String>(), Int::class.java)
            }
        }

        test("schedule.version migration이 없으면 beatDev prerequisite 오류로 fail fast 한다") {
            val jdbcTemplate = mockk<JdbcTemplate>()
            every { jdbcTemplate.queryForObject(any<String>(), Int::class.java) } returns 0

            val exception =
                shouldThrow<IllegalStateException> {
                    StockContentionScheduleVersionPrerequisite(jdbcTemplate)
                }

            exception.message shouldContain "beatDev migration"
            exception.message shouldContain "schedule.version"
        }
    }
}
