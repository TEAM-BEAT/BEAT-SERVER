package com.beat.infrastructure.persistence.booking.entity

import com.beat.domain.booking.model.BookingStatus
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity(name = "Booking")
@Table(name = "booking")
internal class BookingJpaEntity private constructor(
    id: Long?,
    purchaseTicketCount: Int,
    bookerName: String,
    bookerPhoneNumber: String,
    bookingStatus: BookingStatus,
    createdAt: LocalDateTime,
    cancellationDate: LocalDateTime?,
    birthDate: String?,
    password: String?,
    refundAccount: RefundAccountJpaValue?,
    totalPaymentAmount: Int?,
    scheduleId: Long,
    userId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(nullable = false)
    var purchaseTicketCount: Int = purchaseTicketCount
        protected set

    @Column(nullable = false)
    var bookerName: String = bookerName
        protected set

    @Column(nullable = false)
    var bookerPhoneNumber: String = bookerPhoneNumber
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var bookingStatus: BookingStatus = bookingStatus
        protected set

    @Column(nullable = false)
    var createdAt: LocalDateTime = createdAt
        protected set

    @Column(nullable = true)
    var cancellationDate: LocalDateTime? = cancellationDate
        protected set

    @Column(nullable = true)
    var birthDate: String? = birthDate
        protected set

    @Column(nullable = true)
    var password: String? = password
        protected set

    @Embedded
    var refundAccount: RefundAccountJpaValue? = refundAccount
        protected set

    @Column(name = "total_payment_amount")
    var totalPaymentAmount: Int? = totalPaymentAmount
        protected set

    @Column(name = "schedule_id", nullable = false)
    var scheduleId: Long = scheduleId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    companion object {
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
            refundAccount: RefundAccountJpaValue?,
            scheduleId: Long,
            userId: Long,
            totalPaymentAmount: Int? = null,
        ): BookingJpaEntity = BookingJpaEntity(
            id = id,
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
            scheduleId = scheduleId,
            userId = userId,
        )
    }
}
