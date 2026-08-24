package com.beat.infrastructure.persistence.performance.entity

import com.beat.infrastructure.persistence.common.BaseTimeEntity
import com.beat.domain.performance.model.Genre
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "Performance")
@Table(name = "performance")
internal class PerformanceJpaEntity private constructor(
    id: Long?,
    performanceTitle: String,
    genre: Genre,
    runningTime: Int,
    performanceDescription: String,
    performanceAttentionNote: String,
    paymentAccount: PaymentAccountJpaValue?,
    posterImage: String,
    performanceTeamName: String,
    performanceVenue: String,
    roadAddressName: String,
    placeDetailAddress: String,
    latitude: String,
    longitude: String,
    performanceContact: String,
    performancePeriodValue: PerformancePeriodJpaValue?,
    legacyPerformancePeriod: String,
    ticketPrice: Int,
    totalScheduleCount: Int,
    userId: Long,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(nullable = false)
    var performanceTitle: String = performanceTitle
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var genre: Genre = genre
        protected set

    @Column(nullable = false)
    var runningTime: Int = runningTime
        protected set

    @Column(nullable = false, length = 1500)
    var performanceDescription: String = performanceDescription
        protected set

    @Column(nullable = false, length = 1500)
    var performanceAttentionNote: String = performanceAttentionNote
        protected set

    @Embedded
    var paymentAccount: PaymentAccountJpaValue? = paymentAccount
        protected set

    @Column(nullable = false)
    var posterImage: String = posterImage
        protected set

    @Column(nullable = false)
    var performanceTeamName: String = performanceTeamName
        protected set

    @Column(nullable = false, columnDefinition = "text")
    var performanceVenue: String = performanceVenue
        protected set

    @Column(nullable = false)
    var roadAddressName: String = roadAddressName
        protected set

    @Column(nullable = false)
    var placeDetailAddress: String = placeDetailAddress
        protected set

    @Column(nullable = false)
    var latitude: String = latitude
        protected set

    @Column(nullable = false)
    var longitude: String = longitude
        protected set

    @Column(nullable = false)
    var performanceContact: String = performanceContact
        protected set

    @Embedded
    var performancePeriodValue: PerformancePeriodJpaValue? = performancePeriodValue
        protected set

    @Column(name = "performance_period", nullable = false)
    var legacyPerformancePeriod: String = legacyPerformancePeriod
        protected set

    @Column(nullable = false)
    var ticketPrice: Int = ticketPrice
        protected set

    @Column(nullable = false)
    var totalScheduleCount: Int = totalScheduleCount
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    companion object {
        fun rehydrate(
            id: Long?,
            performanceTitle: String,
            genre: Genre,
            runningTime: Int,
            performanceDescription: String,
            performanceAttentionNote: String,
            paymentAccount: PaymentAccountJpaValue?,
            posterImage: String,
            performanceTeamName: String,
            performanceVenue: String,
            roadAddressName: String,
            placeDetailAddress: String,
            latitude: String,
            longitude: String,
            performanceContact: String,
            performancePeriodValue: PerformancePeriodJpaValue?,
            legacyPerformancePeriod: String,
            ticketPrice: Int,
            totalScheduleCount: Int,
            userId: Long,
        ): PerformanceJpaEntity = PerformanceJpaEntity(
            id = id,
            performanceTitle = performanceTitle,
            genre = genre,
            runningTime = runningTime,
            performanceDescription = performanceDescription,
            performanceAttentionNote = performanceAttentionNote,
            paymentAccount = paymentAccount,
            posterImage = posterImage,
            performanceTeamName = performanceTeamName,
            performanceVenue = performanceVenue,
            roadAddressName = roadAddressName,
            placeDetailAddress = placeDetailAddress,
            latitude = latitude,
            longitude = longitude,
            performanceContact = performanceContact,
            performancePeriodValue = performancePeriodValue,
            legacyPerformancePeriod = legacyPerformancePeriod,
            ticketPrice = ticketPrice,
            totalScheduleCount = totalScheduleCount,
            userId = userId,
        )
    }
}
