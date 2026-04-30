package com.beat.infra.persistence.booking.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.booking.domain.Booking;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;

@Component
public class BookingPersistenceMapper {

	public Booking toDomain(BookingJpaEntity entity) {
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
			entity.getBankName(),
			entity.getAccountNumber(),
			entity.getAccountHolder(),
			entity.getScheduleId(),
			entity.getUserId()
		);
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
			domain.getBankName(),
			domain.getAccountNumber(),
			domain.getAccountHolder(),
			domain.getScheduleId(),
			domain.getUserId()
		);
	}
}
