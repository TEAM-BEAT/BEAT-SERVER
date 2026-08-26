package com.beat.domain.performance.model

import com.beat.domain.exception.DomainException
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.sharedkernel.model.AggregateRoot
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.user.model.Users
import java.time.LocalDateTime

class Performance
private constructor(
    private val performanceId: Id?,
    val performanceTitle: String,
    val genre: Genre,
    val runningTimeValue: RunningTime,
    val performanceDescription: String,
    val performanceAttentionNote: String,
    val paymentAccount: PaymentAccount?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val roadAddressName: String,
    val placeDetailAddress: String,
    val latitude: String,
    val longitude: String,
    val performanceContact: String,
    val performancePeriodValue: PerformancePeriod,
    val ticketPriceValue: TicketPrice,
    val totalScheduleCount: Int,
    private val linkedUserId: Users.Id,
    private val castValues: List<Cast>,
    private val staffValues: List<Staff>,
    private val imageValues: List<PerformanceImage>,
) : AggregateRoot {
    val id: Long?
        get() = performanceId?.value

    val userId: Long
        get() = linkedUserId.value

    val runningTime: Int
        get() = runningTimeValue.minutes

    val bankName: BankName?
        get() = paymentAccount?.bankName

    val accountNumber: String?
        get() = paymentAccount?.accountNumber

    val accountHolder: String?
        get() = paymentAccount?.accountHolder

    val ticketPrice: Int
        get() = ticketPriceValue.amount

    val casts: List<Cast>
        get() = castValues.toList()

    val staffs: List<Staff>
        get() = staffValues.toList()

    val images: List<PerformanceImage>
        get() = imageValues.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Performance) return false
        return performanceId != null && performanceId == other.performanceId
    }

    override fun hashCode(): Int = performanceId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Performance(id=$id)"

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
        ticketPrice: TicketPrice = ticketPriceValue,
        hasActiveBooking: Boolean = false,
    ): Performance {
        validateTotalScheduleCount(totalScheduleCount)
        validateTicketPriceUpdate(ticketPrice, hasActiveBooking)
        validatePaymentAccount(ticketPrice, paymentAccount)
        return Performance(
            performanceId = performanceId,
            performanceTitle = performanceTitle,
            genre = genre,
            runningTimeValue = runningTime,
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
            performancePeriodValue = performancePeriod,
            ticketPriceValue = ticketPrice,
            totalScheduleCount = totalScheduleCount,
            linkedUserId = linkedUserId,
            castValues = castValues,
            staffValues = staffValues,
            imageValues = imageValues,
        )
    }

    fun updateTicketPrice(newTicketPrice: Int, hasActiveBooking: Boolean = false): Performance =
        updateTicketPrice(TicketPrice.of(newTicketPrice), hasActiveBooking)

    fun updateTicketPrice(
        newTicketPrice: TicketPrice,
        hasActiveBooking: Boolean = false,
    ): Performance {
        validateTicketPriceUpdate(newTicketPrice, hasActiveBooking)
        validatePaymentAccount(newTicketPrice, paymentAccount)
        return copy(ticketPrice = newTicketPrice)
    }

    fun ensureDeletable(hasActiveBooking: Boolean) {
        if (hasActiveBooking) {
            throw DomainException(PerformanceErrorCode.DELETE_NOT_ALLOWED)
        }
    }

    private fun validateTicketPriceUpdate(
        newTicketPrice: TicketPrice,
        hasActiveBooking: Boolean,
    ) {
        if (hasActiveBooking && ticketPriceValue != newTicketPrice) {
            throw DomainException(PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED)
        }
    }

    fun calculateEndAt(start: LocalDateTime): LocalDateTime = runningTimeValue.endsAt(start)

    fun isOwnedBy(userId: Long): Boolean = linkedUserId.value == userId

    private fun copy(
        ticketPrice: TicketPrice = ticketPriceValue,
        casts: List<Cast> = this.castValues,
        staffs: List<Staff> = this.staffValues,
        images: List<PerformanceImage> = this.imageValues,
    ): Performance =
        Performance(
            performanceId = performanceId,
            performanceTitle = performanceTitle,
            genre = genre,
            runningTimeValue = runningTimeValue,
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
            ticketPriceValue = ticketPrice,
            totalScheduleCount = totalScheduleCount,
            linkedUserId = linkedUserId,
            castValues = casts.toList(),
            staffValues = staffs.toList(),
            imageValues = images.toList(),
        )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            fun from(value: Long): Id = Id(value)

            fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
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
            validatePaymentAccount(ticketPrice, paymentAccount)
            return Performance(
                null,
                performanceTitle,
                genre,
                runningTime,
                performanceDescription,
                performanceAttentionNote,
                paymentAccount,
                posterImage,
                performanceTeamName,
                performanceVenue,
                roadAddressName,
                placeDetailAddress,
                latitude,
                longitude,
                performanceContact,
                performancePeriod,
                ticketPrice,
                totalScheduleCount,
                linkedUserId = Users.Id.from(userId),
                castValues = casts.toList(),
                staffValues = staffs.toList(),
                imageValues = images.toList(),
            )
        }

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
        ): Performance =
            Performance(
                Id.fromNullable(id),
                performanceTitle,
                genre,
                runningTime,
                performanceDescription,
                performanceAttentionNote,
                paymentAccount,
                posterImage,
                performanceTeamName,
                performanceVenue,
                roadAddressName,
                placeDetailAddress,
                latitude,
                longitude,
                performanceContact,
                performancePeriod,
                ticketPrice,
                totalScheduleCount,
                linkedUserId = Users.Id.from(userId),
                castValues = casts.toList(),
                staffValues = staffs.toList(),
                imageValues = images.toList(),
            )

        private fun validateTotalScheduleCount(totalScheduleCount: Int) {
            if (totalScheduleCount < 0) {
                throw DomainException(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT)
            }
        }

        private fun validatePaymentAccount(
            ticketPrice: TicketPrice,
            paymentAccount: PaymentAccount?,
        ) {
            if (ticketPrice.amount == 0 && paymentAccount != null) {
                throw DomainException(
                    PerformanceErrorCode.FREE_PERFORMANCE_PAYMENT_ACCOUNT_NOT_ALLOWED
                )
            }
            if (ticketPrice.amount > 0 && paymentAccount == null) {
                throw DomainException(
                    PerformanceErrorCode.PAID_PERFORMANCE_PAYMENT_ACCOUNT_REQUIRED
                )
            }
        }

    }
}
