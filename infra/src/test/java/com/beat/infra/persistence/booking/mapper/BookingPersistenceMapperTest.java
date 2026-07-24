package com.beat.infra.persistence.booking.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.beat.domain.booking.model.Booking;
import com.beat.domain.booking.model.BookingStatus;
import com.beat.domain.booking.vo.RefundAccount;
import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;
import com.beat.infra.persistence.booking.entity.RefundAccountJpaValue;

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
			new RefundAccountJpaValue(BankName.KAKAOBANK, "111-222", "holder"),
			22L,
			33L,
			30_000
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
			() -> assertEquals(30_000, booking.getTotalPaymentAmount()),
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
			44L,
			55L,
			LocalDateTime.of(2026, 4, 29, 19, 10),
			20_000
		);

		BookingJpaEntity entity = mapper.toEntity(booking);

		assertAll(
			() -> assertNull(entity.getId()),
			() -> assertEquals(1, entity.getPurchaseTicketCount()),
			() -> assertEquals("new-booker", entity.getBookerName()),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, entity.getBookingStatus()),
			() -> assertEquals(20_000, entity.getTotalPaymentAmount()),
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
			RefundAccount.of(BankName.TOSSBANK, "999-888", "refund-holder"),
			41L,
			51L,
			45_000
		);

		Booking roundTrip = mapper.toDomain(mapper.toEntity(booking));

		assertAll(
			() -> assertEquals(booking.getId(), roundTrip.getId()),
			() -> assertEquals(booking.getBookingStatus(), roundTrip.getBookingStatus()),
			() -> assertEquals(booking.getCreatedAt(), roundTrip.getCreatedAt()),
			() -> assertEquals(booking.getBankName(), roundTrip.getBankName()),
			() -> assertEquals(booking.getAccountNumber(), roundTrip.getAccountNumber()),
			() -> assertEquals(booking.getAccountHolder(), roundTrip.getAccountHolder()),
			() -> assertEquals(booking.getTotalPaymentAmount(), roundTrip.getTotalPaymentAmount()),
			() -> assertEquals(booking.getScheduleId(), roundTrip.getScheduleId()),
			() -> assertEquals(booking.getUserId(), roundTrip.getUserId())
		);
	}
}
