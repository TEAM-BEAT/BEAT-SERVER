package com.beat.infra.persistence.booking.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.domain.BankName;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;

class BookingPersistenceMapperTest {

	private final BookingPersistenceMapper mapper = new BookingPersistenceMapper();

	@Test
	void toDomainPreservesJpaEntityFields() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 4, 29, 19, 10);
		LocalDateTime cancellationDate = LocalDateTime.of(2026, 4, 30, 19, 10);
		BookingJpaEntity entity = BookingJpaEntity.rehydrate(
			11L,
			2,
			"booker",
			"010-1234-5678",
			BookingStatus.BOOKING_CANCELLED,
			createdAt,
			cancellationDate,
			"990101",
			"1234",
			BankName.KAKAOBANK,
			"111-222",
			"holder",
			22L,
			33L
		);

		Booking booking = mapper.toDomain(entity);

		assertAll(
			() -> assertEquals(11L, booking.getId()),
			() -> assertEquals(2, booking.getPurchaseTicketCount()),
			() -> assertEquals("booker", booking.getBookerName()),
			() -> assertEquals("010-1234-5678", booking.getBookerPhoneNumber()),
			() -> assertEquals(BookingStatus.BOOKING_CANCELLED, booking.getBookingStatus()),
			() -> assertEquals(createdAt, booking.getCreatedAt()),
			() -> assertEquals(cancellationDate, booking.getCancellationDate()),
			() -> assertEquals(BankName.KAKAOBANK, booking.getBankName()),
			() -> assertEquals("111-222", booking.getAccountNumber()),
			() -> assertEquals("holder", booking.getAccountHolder()),
			() -> assertEquals(22L, booking.getScheduleId()),
			() -> assertEquals(33L, booking.getUserId())
		);
	}

	@Test
	void toEntityKeepsGeneratedIdNullForNewBooking() {
		Booking booking = Booking.create(
			1,
			"new-booker",
			"010-0000-0000",
			"000101",
			"pw",
			null,
			null,
			null,
			44L,
			55L
		);

		BookingJpaEntity entity = mapper.toEntity(booking);

		assertAll(
			() -> assertNull(entity.getId()),
			() -> assertEquals(1, entity.getPurchaseTicketCount()),
			() -> assertEquals("new-booker", entity.getBookerName()),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, entity.getBookingStatus()),
			() -> assertEquals(44L, entity.getScheduleId()),
			() -> assertEquals(55L, entity.getUserId())
		);
	}

	@Test
	void roundTripPreservesRefundFields() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 4, 29, 19, 20);
		Booking booking = Booking.rehydrate(
			31L,
			3,
			"refund-booker",
			"010-9999-9999",
			BookingStatus.REFUND_REQUESTED,
			createdAt,
			null,
			"991231",
			"pw",
			BankName.TOSSBANK,
			"999-888",
			"refund-holder",
			41L,
			51L
		);

		Booking roundTrip = mapper.toDomain(mapper.toEntity(booking));

		assertAll(
			() -> assertEquals(booking.getId(), roundTrip.getId()),
			() -> assertEquals(booking.getBookingStatus(), roundTrip.getBookingStatus()),
			() -> assertEquals(booking.getCreatedAt(), roundTrip.getCreatedAt()),
			() -> assertEquals(booking.getBankName(), roundTrip.getBankName()),
			() -> assertEquals(booking.getAccountNumber(), roundTrip.getAccountNumber()),
			() -> assertEquals(booking.getAccountHolder(), roundTrip.getAccountHolder()),
			() -> assertEquals(booking.getScheduleId(), roundTrip.getScheduleId()),
			() -> assertEquals(booking.getUserId(), roundTrip.getUserId())
		);
	}
}
