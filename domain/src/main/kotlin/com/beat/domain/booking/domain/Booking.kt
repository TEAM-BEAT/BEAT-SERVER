package com.beat.domain.booking.domain

import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.performance.domain.BankName
import com.beat.domain.schedule.domain.Schedule
import com.beat.domain.user.domain.Users
import com.beat.global.support.exception.BadRequestException
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class Booking private constructor(
    private val bookingId: Id?,
    private val purchaseTicketCount: Int,
    private val bookerName: String,
    private val bookerPhoneNumber: String,
    private val bookingStatus: BookingStatus,
    private val createdAt: LocalDateTime,
    private val cancellationDate: LocalDateTime?,
    private val birthDate: String?,
    private val password: String?,
    private val bankName: BankName?,
    private val accountNumber: String?,
    private val accountHolder: String?,
    private val linkedScheduleId: Schedule.Id,
    private val linkedUserId: Users.Id,
) {
    fun getId(): Long? = bookingId?.value

    fun getPurchaseTicketCount(): Int = purchaseTicketCount

    fun getBookerName(): String = bookerName

    fun getBookerPhoneNumber(): String = bookerPhoneNumber

    fun getBookingStatus(): BookingStatus = bookingStatus

    fun getCreatedAt(): LocalDateTime = createdAt

    fun getCancellationDate(): LocalDateTime? = cancellationDate

    fun getBirthDate(): String? = birthDate

    fun getPassword(): String? = password

    fun getBankName(): BankName? = bankName

    fun getAccountNumber(): String? = accountNumber

    fun getAccountHolder(): String? = accountHolder

    fun getScheduleId(): Long = linkedScheduleId.value

    fun getUserId(): Long = linkedUserId.value

    fun updateBookingStatus(bookingStatus: BookingStatus): Booking = copy(
        bookingStatus = bookingStatus,
        cancellationDate = if (isTerminalCancellationStatus(bookingStatus)) {
            cancellationDate ?: LocalDateTime.now()
        } else {
            cancellationDate
        },
    )

    fun updateRefundInfo(bankName: BankName?, accountNumber: String?, accountHolder: String?): Booking = copy(
        bankName = bankName,
        accountNumber = accountNumber,
        accountHolder = accountHolder,
        bookingStatus = BookingStatus.REFUND_REQUESTED,
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
        fun create(
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
            birthDate: String?,
            password: String?,
            bankName: BankName?,
            accountNumber: String?,
            accountHolder: String?,
            scheduleId: Long?,
            userId: Long?,
        ): Booking {
            validatePurchaseTicketCount(purchaseTicketCount)
            requireNotNull(scheduleId) { "scheduleId must not be null" }
            requireNotNull(userId) { "userId must not be null" }

            return Booking(
                bookingId = null,
                purchaseTicketCount = purchaseTicketCount,
                bookerName = bookerName,
                bookerPhoneNumber = bookerPhoneNumber,
                bookingStatus = BookingStatus.CHECKING_PAYMENT,
                createdAt = LocalDateTime.now(),
                cancellationDate = null,
                birthDate = birthDate,
                password = password,
                bankName = bankName,
                accountNumber = accountNumber,
                accountHolder = accountHolder,
                linkedScheduleId = Schedule.Id.from(scheduleId),
                linkedUserId = Users.Id.from(userId),
            )
        }

        @JvmStatic
        fun rehydrate(
            id: Long?,
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
            bookingStatus: BookingStatus,
            createdAt: LocalDateTime,
            cancellationDate: LocalDateTime?,
            birthDate: String?,
            password: String?,
            bankName: BankName?,
            accountNumber: String?,
            accountHolder: String?,
            scheduleId: Long?,
            userId: Long?,
        ): Booking {
            requireNotNull(scheduleId) { "scheduleId must not be null" }
            requireNotNull(userId) { "userId must not be null" }

            return Booking(
                bookingId = Id.fromNullable(id),
                purchaseTicketCount = purchaseTicketCount,
                bookerName = bookerName,
                bookerPhoneNumber = bookerPhoneNumber,
                bookingStatus = bookingStatus,
                createdAt = createdAt,
                cancellationDate = cancellationDate,
                birthDate = birthDate,
                password = password,
                bankName = bankName,
                accountNumber = accountNumber,
                accountHolder = accountHolder,
                linkedScheduleId = Schedule.Id.from(scheduleId),
                linkedUserId = Users.Id.from(userId),
            )
        }

        private fun validatePurchaseTicketCount(purchaseTicketCount: Int) {
            if (purchaseTicketCount <= 0) {
                throw BadRequestException(BookingErrorCode.INVALID_DATA_FORMAT)
            }
        }

        private fun isTerminalCancellationStatus(bookingStatus: BookingStatus): Boolean =
            bookingStatus == BookingStatus.BOOKING_CANCELLED || bookingStatus == BookingStatus.BOOKING_DELETED
    }
}
