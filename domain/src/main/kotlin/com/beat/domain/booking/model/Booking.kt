package com.beat.domain.booking.model

import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.exception.DomainException
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.sharedkernel.model.AggregateRoot
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.user.model.Users
import java.time.LocalDateTime

class Booking
private constructor(
    private val bookingId: Id?,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val bookingStatus: BookingStatus,
    val createdAt: LocalDateTime,
    val cancellationDate: LocalDateTime?,
    val birthDate: String?,
    val password: String?,
    val refundAccount: RefundAccount?,
    val totalPaymentAmount: Int,
    private val linkedScheduleId: Schedule.Id,
    private val linkedUserId: Users.Id,
) : AggregateRoot {
    val id: Long?
        get() = bookingId?.value

    val bankName: BankName?
        get() = refundAccount?.bankName

    val accountNumber: String?
        get() = refundAccount?.accountNumber

    val accountHolder: String?
        get() = refundAccount?.accountHolder

    val scheduleId: Long
        get() = linkedScheduleId.value

    val userId: Long
        get() = linkedUserId.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Booking) return false
        return bookingId != null && bookingId == other.bookingId
    }

    override fun hashCode(): Int = bookingId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Booking(id=${bookingId?.value}, status=$bookingStatus)"

    fun hasActiveTicketAllocation(): Boolean = !bookingStatus.isInactiveForTicketAllocation()

    fun isOwnedBy(userId: Long): Boolean = linkedUserId.value == userId

    fun transitionTo(requestedStatus: BookingStatus): Booking {
        if (bookingStatus == requestedStatus) return this
        if (bookingStatus == BookingStatus.BOOKING_CONFIRMED) {
            throw DomainException(BookingErrorCode.CONFIRMED_STATUS_CHANGE_NOT_ALLOWED)
        }
        if (
            bookingStatus != BookingStatus.CHECKING_PAYMENT ||
                requestedStatus != BookingStatus.BOOKING_CONFIRMED
        ) {
            throw DomainException(BookingErrorCode.STATUS_TRANSITION_NOT_ALLOWED)
        }
        return confirmPayment()
    }

    fun confirmPayment(): Booking =
        when (bookingStatus) {
            BookingStatus.CHECKING_PAYMENT ->
                withState(bookingStatus = BookingStatus.BOOKING_CONFIRMED)
            BookingStatus.BOOKING_CONFIRMED -> this
            else -> throw DomainException(BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED)
        }

    fun requestRefund(refundAccount: RefundAccount): Booking =
        when (bookingStatus) {
            BookingStatus.CHECKING_PAYMENT ->
                withState(
                    refundAccount = refundAccount,
                    bookingStatus = BookingStatus.REFUND_REQUESTED,
                )
            BookingStatus.BOOKING_CONFIRMED -> {
                if (isFreeBooking()) {
                    throw DomainException(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED)
                }
                withState(
                    refundAccount = refundAccount,
                    bookingStatus = BookingStatus.REFUND_REQUESTED,
                )
            }
            BookingStatus.REFUND_REQUESTED -> {
                if (this.refundAccount == refundAccount) {
                    this
                } else {
                    throw DomainException(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED)
                }
            }
            else -> throw DomainException(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED)
        }

    fun cancelUnpaidOrFree(cancelledAt: LocalDateTime): Booking =
        when (bookingStatus) {
            BookingStatus.CHECKING_PAYMENT ->
                withState(
                    bookingStatus = BookingStatus.BOOKING_CANCELLED,
                    cancellationDate = cancelledAt,
                )
            BookingStatus.BOOKING_CONFIRMED -> {
                if (isFreeBooking()) {
                    withState(
                        bookingStatus = BookingStatus.BOOKING_CANCELLED,
                        cancellationDate = cancelledAt,
                    )
                } else {
                    throw DomainException(BookingErrorCode.CANCELLATION_NOT_ALLOWED)
                }
            }
            BookingStatus.BOOKING_CANCELLED -> this
            else -> throw DomainException(BookingErrorCode.CANCELLATION_NOT_ALLOWED)
        }

    fun completeRefund(completedAt: LocalDateTime): Booking =
        when (bookingStatus) {
            BookingStatus.REFUND_REQUESTED ->
                withState(
                    bookingStatus = BookingStatus.BOOKING_CANCELLED,
                    cancellationDate = completedAt,
                )
            BookingStatus.BOOKING_CANCELLED -> this
            else -> throw DomainException(BookingErrorCode.REFUND_COMPLETION_NOT_ALLOWED)
        }

    fun delete(): Booking =
        when (bookingStatus) {
            BookingStatus.BOOKING_DELETED -> this
            BookingStatus.BOOKING_CANCELLED ->
                withState(bookingStatus = BookingStatus.BOOKING_DELETED)
            else -> throw DomainException(BookingErrorCode.DELETION_NOT_ALLOWED)
        }

    fun deleteByMaker(deletedAt: LocalDateTime): Booking {
        if (!canDeleteByMaker(bookingStatus, totalPaymentAmount)) {
            throw DomainException(BookingErrorCode.DELETION_NOT_ALLOWED)
        }
        return if (hasActiveTicketAllocation()) {
            cancelUnpaidOrFree(deletedAt).delete()
        } else {
            delete()
        }
    }

    private fun isFreeBooking(): Boolean = totalPaymentAmount == 0

    private fun withState(
        bookingStatus: BookingStatus = this.bookingStatus,
        cancellationDate: LocalDateTime? = this.cancellationDate,
        refundAccount: RefundAccount? = this.refundAccount,
    ): Booking =
        Booking(
            bookingId = bookingId,
            purchaseTicketCount = purchaseTicketCount,
            bookerName = bookerName,
            bookerPhoneNumber = bookerPhoneNumber,
            bookingStatus = bookingStatus,
            createdAt = createdAt,
            cancellationDate = cancellationDate,
            birthDate = birthDate,
            password = password,
            refundAccount = refundAccount,
            totalPaymentAmount = totalPaymentAmount,
            linkedScheduleId = linkedScheduleId,
            linkedUserId = linkedUserId,
        )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            fun from(value: Long): Id = Id(value)

            fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
        fun canDeleteByMaker(bookingStatus: BookingStatus, totalPaymentAmount: Int): Boolean =
            if (totalPaymentAmount == 0) {
                bookingStatus != BookingStatus.REFUND_REQUESTED
            } else {
                bookingStatus == BookingStatus.CHECKING_PAYMENT ||
                    bookingStatus == BookingStatus.BOOKING_CANCELLED ||
                    bookingStatus == BookingStatus.BOOKING_DELETED
            }

        fun create(
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
            birthDate: String?,
            password: String?,
            scheduleId: Long,
            userId: Long,
            createdAt: LocalDateTime,
            totalPaymentAmount: Int,
        ): Booking {
            validatePurchaseTicketCount(purchaseTicketCount)
            validateTotalPaymentAmount(totalPaymentAmount)

            return Booking(
                bookingId = null,
                purchaseTicketCount = purchaseTicketCount,
                bookerName = bookerName,
                bookerPhoneNumber = bookerPhoneNumber,
                bookingStatus =
                    if (totalPaymentAmount == 0) {
                        BookingStatus.BOOKING_CONFIRMED
                    } else {
                        BookingStatus.CHECKING_PAYMENT
                    },
                createdAt = createdAt,
                cancellationDate = null,
                birthDate = birthDate,
                password = password,
                refundAccount = null,
                totalPaymentAmount = totalPaymentAmount,
                linkedScheduleId = Schedule.Id.from(scheduleId),
                linkedUserId = Users.Id.from(userId),
            )
        }

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
            refundAccount: RefundAccount?,
            scheduleId: Long,
            userId: Long,
            totalPaymentAmount: Int,
        ): Booking {
            validateTotalPaymentAmount(totalPaymentAmount)

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
                refundAccount = refundAccount,
                totalPaymentAmount = totalPaymentAmount,
                linkedScheduleId = Schedule.Id.from(scheduleId),
                linkedUserId = Users.Id.from(userId),
            )
        }

        private fun validatePurchaseTicketCount(purchaseTicketCount: Int) {
            if (purchaseTicketCount <= 0) {
                throw DomainException(BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT)
            }
            if (purchaseTicketCount > MAX_PURCHASE_TICKET_COUNT) {
                throw DomainException(BookingErrorCode.PURCHASE_TICKET_COUNT_EXCEEDED)
            }
        }

        private fun validateTotalPaymentAmount(totalPaymentAmount: Int) {
            if (totalPaymentAmount < 0) {
                throw DomainException(BookingErrorCode.NEGATIVE_TOTAL_PAYMENT_AMOUNT)
            }
        }

        private const val MAX_PURCHASE_TICKET_COUNT = 10
    }
}
