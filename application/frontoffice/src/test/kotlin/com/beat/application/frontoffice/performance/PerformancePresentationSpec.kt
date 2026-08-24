package com.beat.application.frontoffice.performance

import com.beat.domain.performance.vo.PerformancePeriod
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PerformancePresentationSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    val today = LocalDate.of(2026, 4, 30)

    context("가장 가까운 회차까지의 일수") {
        test("과거가 아닌 회차 중 가장 가까운 회차를 우선한다") {
            nearestDueDate(
                today,
                listOf(atNoon(today.plusDays(7)), atNoon(today.minusDays(1)), atNoon(today.plusDays(2))),
            ) shouldBe 2
        }

        test("미래 회차가 없으면 가장 이른 과거 회차를 사용한다") {
            nearestDueDate(
                today,
                listOf(atNoon(today.minusDays(1)), atNoon(today.minusDays(4))),
            ) shouldBe -4
        }

        test("회차가 없으면 정렬용 sentinel을 사용한다") {
            nearestDueDate(today, emptyList()) shouldBe Int.MAX_VALUE
        }
    }

    test("공연 기간의 단일 날짜와 범위 표시 형식을 유지한다") {
        formatPerformancePeriod(
            PerformancePeriod.of(LocalDate.of(2026, 7, 16), LocalDate.of(2026, 7, 16)),
        ) shouldBe "2026.07.16"
        formatPerformancePeriod(
            PerformancePeriod.of(LocalDate.of(2026, 7, 16), LocalDate.of(2026, 7, 18)),
        ) shouldBe "2026.07.16~2026.07.18"
    }
})

private fun atNoon(date: LocalDate): LocalDateTime = LocalDateTime.of(date, LocalTime.NOON)
