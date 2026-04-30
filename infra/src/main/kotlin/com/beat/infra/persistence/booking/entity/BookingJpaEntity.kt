package com.beat.infra.persistence.booking.entity

import com.beat.domain.booking.domain.BookingStatus
import com.beat.domain.performance.domain.BankName
import jakarta.persistence.Column
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
class BookingJpaEntity private constructor(
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

    @Column(name = "schedule_id", nullable = false)
    var scheduleId: Long = scheduleId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    companion object {
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
            scheduleId: Long,
            userId: Long,
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
            bankName = bankName,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
            scheduleId = scheduleId,
            userId = userId,
        )
    }
}
