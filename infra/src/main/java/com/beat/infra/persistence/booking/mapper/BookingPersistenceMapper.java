package com.beat.infra.persistence.booking.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.booking.model.Booking;
import com.beat.domain.booking.vo.RefundAccount;
import com.beat.domain.exception.DomainException;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;
import com.beat.infra.persistence.booking.entity.RefundAccountJpaValue;
import com.beat.infra.persistence.exception.PersistenceMappingException;

@Component
public class BookingPersistenceMapper {

	public Booking toDomain(BookingJpaEntity entity) {
		try {
			return Booking.rehydrate(
				entity.getId(),
				entity.getPurchaseTicketCount(),
				entity.getBookerName(),
				entity.getBookerPhoneNumber(),
				entity.getBookingStatus(),
				entity.getCreatedAt(),
				entity.getCancellationDate(),
				entity.getBirthDate(),
				entity.getPassword(),
				toDomain(entity.getRefundAccount()),
				entity.getScheduleId(),
				entity.getUserId(),
				entity.getTotalPaymentAmount()
			);
		} catch (DomainException exception) {
			throw PersistenceMappingException.invalidStoredState("Booking", entity.getId(), exception);
		}
	}

	public BookingJpaEntity toEntity(Booking domain) {
		return BookingJpaEntity.rehydrate(
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
			domain.getTotalPaymentAmount()
		);
	}

	private RefundAccount toDomain(RefundAccountJpaValue value) {
		if (value == null) {
			return null;
		}
		return RefundAccount.of(value.getBankName(), value.getAccountNumber(), value.getAccountHolder());
	}

	private RefundAccountJpaValue toEntity(RefundAccount value) {
		if (value == null) {
			return null;
		}
		return new RefundAccountJpaValue(value.getBankName(), value.getAccountNumber(), value.getAccountHolder());
	}
}
