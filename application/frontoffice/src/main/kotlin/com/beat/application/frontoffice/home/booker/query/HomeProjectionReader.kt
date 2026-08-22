package com.beat.application.frontoffice.home.booker.query

import java.time.LocalDate
import java.time.LocalDateTime

fun interface HomeProjectionReader {
    fun read(genre: String?, now: LocalDateTime): HomeProjection
}

data class HomeProjection(
    val promotions: List<HomePromotionProjection>,
    val performances: List<HomePerformanceProjection>,
)

data class HomePromotionProjection(
    val promotionId: Long,
    val promotionPhoto: String,
    val performanceId: Long?,
    val redirectUrl: String,
    val isExternal: Boolean,
    val carouselNumber: String,
)

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
