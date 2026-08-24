package com.beat.domain.promotion

import com.beat.domain.exception.DomainException
import com.beat.domain.promotion.exception.PromotionErrorCode
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.service.PromotionCarouselDomainService
import com.beat.domain.promotion.service.PromotionEligibilityDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

class PromotionDomainServiceSpec : FunSpec({
    isolationMode = IsolationMode.SingleInstance

    val carouselService = PromotionCarouselDomainService()
    val eligibilityService = PromotionEligibilityDomainService()
    val today = LocalDate.of(2026, 7, 16)

    context("캐러셀 배치") {
        test("캐러셀 번호는 중복이나 null 없이 유효해야 한다") {
            carouselService.hasValidCarouselAssignments(listOf(CarouselNumber.ONE, CarouselNumber.THREE)) shouldBe true
            carouselService.hasValidCarouselAssignments(listOf(CarouselNumber.ONE, CarouselNumber.ONE)) shouldBe false
            carouselService.hasValidCarouselAssignments(listOf(CarouselNumber.ONE, null)) shouldBe false
        }

        test("기존 캐러셀 번호 순서대로 정렬하고 번호를 연속적으로 재배정한다") {
            val fifth = promotion(id = 1L, carouselNumber = CarouselNumber.FIVE)
            val second = promotion(id = 2L, carouselNumber = CarouselNumber.TWO)

            val arranged = carouselService.arrangeCarouselNumbers(listOf(fifth, second))

            arranged.map { it.id } shouldBe listOf(2L, 1L)
            arranged.map { it.carouselNumber } shouldBe listOf(CarouselNumber.ONE, CarouselNumber.TWO)
        }

        test("사용 가능한 캐러셀 슬롯보다 많은 프로모션은 배치할 수 없다") {
            val promotions = CarouselNumber.entries
                .mapIndexed { index, number -> promotion(id = index.toLong(), carouselNumber = number) }
                .toMutableList()
                .apply { add(promotion(id = 99L, carouselNumber = CarouselNumber.ONE)) }

            shouldThrow<DomainException> {
                carouselService.arrangeCarouselNumbers(promotions)
            }.errorCode shouldBe PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS
        }
    }

    context("프로모션 노출 자격") {
        test("모든 회차가 지난 내부 프로모션은 노출할 수 없다") {
            val internal = promotion(id = 1L, performanceId = 1L)
            val pastSchedules = listOf(
                today.minusDays(2).atStartOfDay(),
                today.minusDays(1).atStartOfDay(),
            )

            eligibilityService.isEligible(internal, pastSchedules, today) shouldBe false
        }

        test("오늘 또는 미래 회차가 있는 내부 프로모션은 노출할 수 있다") {
            val internal = promotion(id = 1L, performanceId = 1L)
            val schedules = listOf(
                today.minusDays(1).atStartOfDay(),
                today.atStartOfDay(),
            )

            eligibilityService.isEligible(internal, schedules, today) shouldBe true
        }

        test("공연이 없는 외부 프로모션과 회차가 없는 내부 프로모션은 기존 노출 자격을 유지한다") {
            val external = promotion(id = 1L)

            eligibilityService.isEligible(
                external,
                listOf(today.minusDays(1).atStartOfDay()),
                today,
            ) shouldBe true
            eligibilityService.isEligible(
                promotion(id = 1L, performanceId = 1L),
                emptyList<LocalDateTime>(),
                today,
            ) shouldBe true
        }
    }
})

private fun promotion(
    id: Long,
    performanceId: Long? = null,
    carouselNumber: CarouselNumber = CarouselNumber.ONE,
): Promotion = Promotion.rehydrate(
    id = id,
    promotionPhoto = "image",
    performanceId = performanceId,
    redirectUrl = "url",
    isExternal = performanceId == null,
    carouselNumber = carouselNumber,
)
