package com.beat.application.frontoffice.home.booker.query

import com.beat.application.frontoffice.query.PresentationReadModel
import java.time.LocalDate
import java.time.LocalDateTime

@PresentationReadModel
fun interface HomeProjectionReader {
    fun read(genre: String?, now: LocalDateTime): HomeProjection
}

@PresentationReadModel
data class HomeProjection(
    val promotions: List<HomePromotionProjection>,
    val performances: List<HomePerformanceProjection>,
)

@PresentationReadModel
data class HomePromotionProjection(
    val promotionId: Long,
    val promotionPhoto: String,
    val performanceId: Long?,
    val redirectUrl: String,
    val isExternal: Boolean,
    val carouselNumber: String,
)

@PresentationReadModel
data class HomePerformanceProjection(
    val performanceId: Long,
    val performanceTitle: String,
    val ticketPrice: Int,
    val genre: String,
    val posterImage: String,
    val performanceVenue: String,
    val performanceDate: LocalDateTime?,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
)
