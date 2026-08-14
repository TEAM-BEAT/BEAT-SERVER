package com.beat.infra.persistence.booking.mapper

import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.exception.DomainException
import com.beat.infra.persistence.booking.entity.BookingJpaEntity
import com.beat.infra.persistence.booking.entity.RefundAccountJpaValue
import com.beat.infra.persistence.exception.PersistenceMappingException
import org.springframework.stereotype.Component

@Component
class BookingPersistenceMapper {
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
            domain.getId(),
            domain.getPurchaseTicketCount(),
            domain.getBookerName(),
            domain.getBookerPhoneNumber(),
            domain.getBookingStatus(),
            domain.getCreatedAt(),
            domain.getCancellationDate(),
            domain.getBirthDate(),
            domain.getPassword(),
            toEntity(domain.getRefundAccount()),
            domain.getScheduleId(),
            domain.getUserId(),
            domain.getTotalPaymentAmount(),
        )

    private fun toDomain(value: RefundAccountJpaValue?): RefundAccount? =
        value?.let { RefundAccount.of(it.bankName, it.accountNumber, it.accountHolder) }

    private fun toEntity(value: RefundAccount?): RefundAccountJpaValue? =
        value?.let { RefundAccountJpaValue(it.bankName, it.accountNumber, it.accountHolder) }
}
