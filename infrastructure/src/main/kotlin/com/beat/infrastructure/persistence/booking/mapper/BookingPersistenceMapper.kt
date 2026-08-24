package com.beat.infrastructure.persistence.booking.mapper

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.exception.DomainException
import com.beat.infrastructure.persistence.booking.entity.BookingJpaEntity
import com.beat.infrastructure.persistence.booking.entity.RefundAccountJpaValue
import com.beat.infrastructure.persistence.exception.PersistenceMappingException
import org.springframework.stereotype.Component

@Component
internal class BookingPersistenceMapper {
    fun toDomain(entity: BookingJpaEntity): Booking =
        try {
            Booking.rehydrate(
                entity.id,
                entity.purchaseTicketCount,
                entity.bookerName,
                entity.bookerPhoneNumber,
                entity.bookingStatus,
                entity.createdAt,
                entity.cancellationDate,
                entity.birthDate,
                entity.password,
                toDomain(entity.refundAccount),
                entity.scheduleId,
                entity.userId,
                entity.totalPaymentAmount,
            )
        } catch (exception: DomainException) {
            throw PersistenceMappingException.invalidStoredState("Booking", entity.id, exception)
        }

    fun toEntity(domain: Booking): BookingJpaEntity =
        BookingJpaEntity.rehydrate(
            domain.id,
            domain.purchaseTicketCount,
            domain.bookerName,
            domain.bookerPhoneNumber,
            domain.bookingStatus,
            domain.createdAt,
            domain.cancellationDate,
            domain.birthDate,
            domain.password,
            toEntity(domain.refundAccount),
            domain.scheduleId,
            domain.userId,
            domain.totalPaymentAmount,
        )

    private fun toDomain(value: RefundAccountJpaValue?): RefundAccount? =
        value?.let { RefundAccount.of(it.bankName, it.accountNumber, it.accountHolder) }

    private fun toEntity(value: RefundAccount?): RefundAccountJpaValue? =
        value?.let { RefundAccountJpaValue(it.bankName, it.accountNumber, it.accountHolder) }
}
