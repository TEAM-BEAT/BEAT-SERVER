package com.beat.domain.performance.model

import com.beat.domain.exception.DomainException
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.sharedkernel.model.AggregateRoot
import com.beat.domain.user.model.Users
import java.time.LocalDateTime

class Performance private constructor(
    private val performanceId: Id?,
    val performanceTitle: String,
    val genre: Genre,
    private val runningTimeValue: RunningTime,
    val performanceDescription: String,
    val performanceAttentionNote: String,
    private val paymentAccountValue: PaymentAccount?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val roadAddressName: String,
    val placeDetailAddress: String,
    val latitude: String,
    val longitude: String,
    val performanceContact: String,
    private val performancePeriodValue: PerformancePeriod,
    private val ticketPriceValue: TicketPrice,
    val totalScheduleCount: Int,
    private val linkedUserId: Users.Id,
    private val casts: List<Cast>,
    private val staffs: List<Staff>,
    private val images: List<PerformanceImage>,
) : AggregateRoot {
    fun getId(): Long? = performanceId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Performance) return false
        return performanceId != null && performanceId == other.performanceId
    }

    override fun hashCode(): Int = performanceId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Performance(id=${getId()})"

    fun getUserId(): Long = linkedUserId.value

    fun getRunningTime(): Int = runningTimeValue.minutes

    fun getRunningTimeValue(): RunningTime = runningTimeValue

    fun getPaymentAccount(): PaymentAccount? = paymentAccountValue

    fun getBankName(): BankName? = paymentAccountValue?.bankName

    fun getAccountNumber(): String? = paymentAccountValue?.accountNumber

    fun getAccountHolder(): String? = paymentAccountValue?.accountHolder

    fun getPerformancePeriodValue(): PerformancePeriod = performancePeriodValue

    fun getTicketPrice(): Int = ticketPriceValue.amount

    fun getTicketPriceValue(): TicketPrice = ticketPriceValue

    fun getCasts(): List<Cast> = casts.toList()

    fun getStaffs(): List<Staff> = staffs.toList()

    fun getImages(): List<PerformanceImage> = images.toList()

    fun replaceContent(
        casts: List<Cast>,
        staffs: List<Staff>,
        images: List<PerformanceImage>,
    ): Performance = copy(casts = casts, staffs = staffs, images = images)

    fun update(
        performanceTitle: String,
        genre: Genre,
        runningTime: RunningTime,
        performanceDescription: String,
        performanceAttentionNote: String,
        paymentAccount: PaymentAccount?,
        posterImage: String,
        performanceTeamName: String,
        performanceVenue: String,
        roadAddressName: String,
        placeDetailAddress: String,
        latitude: String,
        longitude: String,
        performanceContact: String,
        performancePeriod: PerformancePeriod,
        totalScheduleCount: Int,
    ): Performance {
        validateTotalScheduleCount(totalScheduleCount)
        return Performance(
            performanceId = performanceId,
            performanceTitle = performanceTitle,
            genre = genre,
            runningTimeValue = runningTime,
            performanceDescription = performanceDescription,
            performanceAttentionNote = performanceAttentionNote,
            paymentAccountValue = paymentAccount,
            posterImage = posterImage,
            performanceTeamName = performanceTeamName,
            performanceVenue = performanceVenue,
            roadAddressName = roadAddressName,
            placeDetailAddress = placeDetailAddress,
            latitude = latitude,
            longitude = longitude,
            performanceContact = performanceContact,
            performancePeriodValue = performancePeriod,
            ticketPriceValue = ticketPriceValue,
            totalScheduleCount = totalScheduleCount,
            linkedUserId = linkedUserId,
            casts = casts,
            staffs = staffs,
            images = images,
        )
    }

    @JvmOverloads
    fun updateTicketPrice(newTicketPrice: Int, hasActiveBooking: Boolean = false): Performance =
        updateTicketPrice(TicketPrice.of(newTicketPrice), hasActiveBooking)

    @JvmOverloads
    fun updateTicketPrice(newTicketPrice: TicketPrice, hasActiveBooking: Boolean = false): Performance {
        if (hasActiveBooking && ticketPriceValue != newTicketPrice) {
            throw DomainException(PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED)
        }
        return copy(ticketPrice = newTicketPrice)
    }

    fun ensureDeletable(hasActiveBooking: Boolean) {
        if (hasActiveBooking) {
            throw DomainException(PerformanceErrorCode.DELETE_NOT_ALLOWED)
        }
    }

    fun calculateEndAt(start: LocalDateTime): LocalDateTime = runningTimeValue.endsAt(start)

    fun isOwnedBy(userId: Long): Boolean = linkedUserId.value == userId

    private fun copy(
        ticketPrice: TicketPrice = ticketPriceValue,
        casts: List<Cast> = this.casts,
        staffs: List<Staff> = this.staffs,
        images: List<PerformanceImage> = this.images,
    ): Performance = Performance(
        performanceId = performanceId,
        performanceTitle = performanceTitle,
        genre = genre,
        runningTimeValue = runningTimeValue,
        performanceDescription = performanceDescription,
        performanceAttentionNote = performanceAttentionNote,
        paymentAccountValue = paymentAccountValue,
        posterImage = posterImage,
        performanceTeamName = performanceTeamName,
        performanceVenue = performanceVenue,
        roadAddressName = roadAddressName,
        placeDetailAddress = placeDetailAddress,
        latitude = latitude,
        longitude = longitude,
        performanceContact = performanceContact,
        performancePeriodValue = performancePeriodValue,
        ticketPriceValue = ticketPrice,
        totalScheduleCount = totalScheduleCount,
        linkedUserId = linkedUserId,
        casts = casts.toList(),
        staffs = staffs.toList(),
        images = images.toList(),
    )

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
        @JvmOverloads
        fun create(
            performanceTitle: String,
            genre: Genre,
            runningTime: RunningTime,
            performanceDescription: String,
            performanceAttentionNote: String,
            paymentAccount: PaymentAccount?,
            posterImage: String,
            performanceTeamName: String,
            performanceVenue: String,
            roadAddressName: String,
            placeDetailAddress: String,
            latitude: String,
            longitude: String,
            performanceContact: String,
            performancePeriod: PerformancePeriod,
            ticketPrice: TicketPrice,
            totalScheduleCount: Int,
            userId: Long,
            casts: List<Cast> = emptyList(),
            staffs: List<Staff> = emptyList(),
            images: List<PerformanceImage> = emptyList(),
        ): Performance {
            validateTotalScheduleCount(totalScheduleCount)
            return Performance(
                null, performanceTitle, genre, runningTime, performanceDescription, performanceAttentionNote,
                paymentAccount, posterImage, performanceTeamName, performanceVenue, roadAddressName,
                placeDetailAddress, latitude, longitude, performanceContact, performancePeriod, ticketPrice,
                totalScheduleCount, linkedUserId = Users.Id.from(userId),
                casts = casts.toList(), staffs = staffs.toList(), images = images.toList(),
            )
        }

        @JvmStatic
        @JvmOverloads
        fun rehydrate(
            id: Long?,
            performanceTitle: String,
            genre: Genre,
            runningTime: RunningTime,
            performanceDescription: String,
            performanceAttentionNote: String,
            paymentAccount: PaymentAccount?,
            posterImage: String,
            performanceTeamName: String,
            performanceVenue: String,
            roadAddressName: String,
            placeDetailAddress: String,
            latitude: String,
            longitude: String,
            performanceContact: String,
            performancePeriod: PerformancePeriod,
            ticketPrice: TicketPrice,
            totalScheduleCount: Int,
            userId: Long,
            casts: List<Cast> = emptyList(),
            staffs: List<Staff> = emptyList(),
            images: List<PerformanceImage> = emptyList(),
        ): Performance = Performance(
            Id.fromNullable(id), performanceTitle, genre, runningTime, performanceDescription, performanceAttentionNote,
            paymentAccount, posterImage, performanceTeamName, performanceVenue, roadAddressName, placeDetailAddress,
            latitude, longitude, performanceContact, performancePeriod, ticketPrice, totalScheduleCount,
            linkedUserId = Users.Id.from(userId),
            casts = casts.toList(), staffs = staffs.toList(), images = images.toList(),
        )

        private fun validateTotalScheduleCount(totalScheduleCount: Int) {
            if (totalScheduleCount < 0) {
                throw DomainException(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT)
            }
        }
    }
}
