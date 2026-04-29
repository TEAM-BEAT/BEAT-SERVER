package com.beat.infra.persistence.performance.entity

import com.beat.domain.BaseTimeEntity
import com.beat.domain.performance.domain.BankName
import com.beat.domain.performance.domain.Genre
import jakarta.persistence.*

@Entity(name = "Performance")
@Table(name = "performance")
class PerformanceJpaEntity private constructor(
    id: Long?,
    performanceTitle: String,
    genre: Genre,
    runningTime: Int,
    performanceDescription: String,
    performanceAttentionNote: String,
    bankName: BankName?,
    accountNumber: String?,
    accountHolder: String?,
    posterImage: String,
    performanceTeamName: String,
    performanceVenue: String,
    roadAddressName: String,
    placeDetailAddress: String,
    latitude: String,
    longitude: String,
    performanceContact: String,
    performancePeriod: String,
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var bankName: BankName? = bankName
        protected set

    @Column(nullable = true)
    var accountNumber: String? = accountNumber
        protected set

    @Column(nullable = true)
    var accountHolder: String? = accountHolder
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

    @Column(nullable = false)
    var performancePeriod: String = performancePeriod
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
        @JvmStatic
        fun rehydrate(
            id: Long?,
            performanceTitle: String,
            genre: Genre,
            runningTime: Int,
            performanceDescription: String,
            performanceAttentionNote: String,
            bankName: BankName?,
            accountNumber: String?,
            accountHolder: String?,
            posterImage: String,
            performanceTeamName: String,
            performanceVenue: String,
            roadAddressName: String,
            placeDetailAddress: String,
            latitude: String,
            longitude: String,
            performanceContact: String,
            performancePeriod: String,
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
            bankName = bankName,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
            posterImage = posterImage,
            performanceTeamName = performanceTeamName,
            performanceVenue = performanceVenue,
            roadAddressName = roadAddressName,
            placeDetailAddress = placeDetailAddress,
            latitude = latitude,
            longitude = longitude,
            performanceContact = performanceContact,
            performancePeriod = performancePeriod,
            ticketPrice = ticketPrice,
            totalScheduleCount = totalScheduleCount,
            userId = userId,
        )
    }
}
