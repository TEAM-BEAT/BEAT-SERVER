package com.beat.domain.promotion.service

import com.beat.domain.exception.DomainException
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.exception.PromotionErrorCode

class PromotionCarouselDomainService {
    fun hasValidCarouselAssignments(carouselNumbers: List<CarouselNumber?>): Boolean =
        carouselNumbers.size <= CarouselNumber.entries.size &&
            carouselNumbers.all { it != null } &&
            carouselNumbers.distinct().size == carouselNumbers.size

    fun arrangeCarouselNumbers(promotions: List<Promotion>): List<Promotion> {
        if (promotions.size > CarouselNumber.entries.size) {
            throw DomainException(PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS)
        }

        return promotions
            .sortedBy { it.carouselNumber.number }
            .mapIndexed { index, promotion ->
                promotion.updateCarouselNumber(CarouselNumber.entries[index])
            }
    }
}
