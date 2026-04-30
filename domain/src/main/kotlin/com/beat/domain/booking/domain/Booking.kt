package com.beat.domain.booking.domain

import com.beat.domain.performance.domain.BankName
import java.time.LocalDateTime
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class Booking private constructor(
    private val bookingId: Long?,
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
    private val scheduleId: Long,
    private val userId: Long,
) {
    fun getId(): Long? = bookingId

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

    fun getScheduleId(): Long = scheduleId

    fun getUserId(): Long = userId

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

    companion object {
        @JvmStatic
        fun create(
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
            bookingStatus: BookingStatus,
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
                bookingId = null,
                purchaseTicketCount = purchaseTicketCount,
                bookerName = bookerName,
                bookerPhoneNumber = bookerPhoneNumber,
                bookingStatus = bookingStatus,
                createdAt = LocalDateTime.now(),
                cancellationDate = null,
                birthDate = birthDate,
                password = password,
                bankName = bankName,
                accountNumber = accountNumber,
                accountHolder = accountHolder,
                scheduleId = scheduleId,
                userId = userId,
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
                bookingId = id,
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
                scheduleId = scheduleId,
                userId = userId,
            )
        }

        private fun isTerminalCancellationStatus(bookingStatus: BookingStatus): Boolean =
            bookingStatus == BookingStatus.BOOKING_CANCELLED || bookingStatus == BookingStatus.BOOKING_DELETED
    }
}
