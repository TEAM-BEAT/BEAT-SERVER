package com.beat.domain.performance.domain

import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.global.support.exception.BadRequestException
import com.beat.domain.user.domain.Users
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class Performance private constructor(
    private val performanceId: Id?,
    val performanceTitle: String,
    val genre: Genre,
    val runningTime: Int,
    val performanceDescription: String,
    val performanceAttentionNote: String,
    val bankName: BankName?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val roadAddressName: String,
    val placeDetailAddress: String,
    val latitude: String,
    val longitude: String,
    val performanceContact: String,
    val performancePeriod: String,
    val ticketPrice: Int,
    val totalScheduleCount: Int,
    private val linkedUserId: Users.Id,
) {
    fun getId(): Long? = performanceId?.value

    fun getUserId(): Long = linkedUserId.value

    fun update(
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
        totalScheduleCount: Int,
    ): Performance {
        validateRunningTime(runningTime)
        validateTotalScheduleCount(totalScheduleCount)

        return copy(
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
            totalScheduleCount = totalScheduleCount,
        )
    }

    fun updateTicketPrice(newTicketPrice: Int): Performance {
        validateTicketPrice(newTicketPrice)
        return copy(ticketPrice = newTicketPrice)
    }

    fun updatePerformancePeriod(performanceDates: List<LocalDateTime>): Performance {
        require(performanceDates.isNotEmpty()) { "performanceDates must not be empty" }
        val startDate = performanceDates.min()
        val endDate = performanceDates.max()
        return copy(performancePeriod = formatPerformancePeriod(startDate, endDate))
    }

    fun isOwnedBy(userId: Long): Boolean = linkedUserId.value == userId

    private fun formatPerformancePeriod(startDate: LocalDateTime, endDate: LocalDateTime): String {
        val start = startDate.toLocalDate().toString().replace("-", ".")
        val end = endDate.toLocalDate().toString().replace("-", ".")
        return if (startDate.toLocalDate() == endDate.toLocalDate()) start else "$start~$end"
    }

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            @JvmStatic
            fun from(value: Long): Id = Id(value)

            @JvmStatic
            fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
        @JvmStatic
        fun create(
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
        ): Performance {
            validateRunningTime(runningTime)
            validateTicketPrice(ticketPrice)
            validateTotalScheduleCount(totalScheduleCount)

            return Performance(
                performanceId = null,
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
                linkedUserId = Users.Id.from(userId),
            )
        }

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
        ): Performance = Performance(
            performanceId = Id.fromNullable(id),
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
            linkedUserId = Users.Id.from(userId),
        )

        private fun validateTicketPrice(ticketPrice: Int) {
            if (ticketPrice < 0) {
                throw BadRequestException(PerformanceErrorCode.NEGATIVE_TICKET_PRICE)
            }
        }

        private fun validateRunningTime(runningTime: Int) {
            if (runningTime <= 0) {
                throw BadRequestException(PerformanceErrorCode.INVALID_DATA_FORMAT)
            }
        }

        private fun validateTotalScheduleCount(totalScheduleCount: Int) {
            if (totalScheduleCount < 0) {
                throw BadRequestException(PerformanceErrorCode.INVALID_DATA_FORMAT)
            }
        }
    }
}
