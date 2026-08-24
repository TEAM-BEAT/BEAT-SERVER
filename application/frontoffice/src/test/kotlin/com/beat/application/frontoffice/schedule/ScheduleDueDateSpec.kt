package com.beat.application.frontoffice.schedule

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleDueDateSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    val today = LocalDate.of(2026, 4, 30)

    test("제공된 기준일로 과거, 당일, 미래 회차의 due date를 계산한다") {
        calculateDueDate(today, atNoon(today.minusDays(2))) shouldBe -2
        calculateDueDate(today, atNoon(today)) shouldBe 0
        calculateDueDate(today, atNoon(today.plusDays(3))) shouldBe 3
    }
})

private fun atNoon(date: LocalDate): LocalDateTime = LocalDateTime.of(date, LocalTime.NOON)
