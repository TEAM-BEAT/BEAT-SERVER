package com.beat.domain.booking;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.domain.BankName;
import com.beat.global.common.exception.BadRequestException;

class BookingDomainInvariantTest {

	@Test
	void createRejectsNonPositivePurchaseTicketCount() {
		BadRequestException exception = assertThrows(BadRequestException.class, () -> Booking.create(
			0,
			"booker",
			"010-1234-5678",
			BookingStatus.CHECKING_PAYMENT,
			"990101",
			"1234",
			null,
			null,
			null,
			2L,
			3L
		));

		assertEquals(BookingErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void createRejectsNullScheduleId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Booking.create(
			1,
			"booker",
			"010-1234-5678",
			BookingStatus.CHECKING_PAYMENT,
			"990101",
			"1234",
			null,
			null,
			null,
			null,
			1L
		));

		assertEquals("scheduleId must not be null", exception.getMessage());
	}

	@Test
	void createRejectsNullUserId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Booking.create(
			1,
			"booker",
			"010-1234-5678",
			BookingStatus.CHECKING_PAYMENT,
			"990101",
			"1234",
			null,
			null,
			null,
			2L,
			null
		));

		assertEquals("userId must not be null", exception.getMessage());
	}

	@Test
	void rehydrateRestoresPersistedFieldsForJavaCallers() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 4, 29, 19, 0);
		LocalDateTime cancellationDate = LocalDateTime.of(2026, 4, 30, 19, 0);

		Booking booking = Booking.rehydrate(
			10L,
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
			20L,
			30L
		);

		assertAll(
			() -> assertEquals(10L, booking.getId()),
			() -> assertEquals(2, booking.getPurchaseTicketCount()),
			() -> assertEquals("booker", booking.getBookerName()),
			() -> assertEquals("010-1234-5678", booking.getBookerPhoneNumber()),
			() -> assertEquals(BookingStatus.BOOKING_CANCELLED, booking.getBookingStatus()),
			() -> assertEquals(createdAt, booking.getCreatedAt()),
			() -> assertEquals(cancellationDate, booking.getCancellationDate()),
			() -> assertEquals("990101", booking.getBirthDate()),
			() -> assertEquals("1234", booking.getPassword()),
			() -> assertEquals(BankName.KAKAOBANK, booking.getBankName()),
			() -> assertEquals("111-222", booking.getAccountNumber()),
			() -> assertEquals("holder", booking.getAccountHolder()),
			() -> assertEquals(20L, booking.getScheduleId()),
			() -> assertEquals(30L, booking.getUserId())
		);
	}

	@Test
	void updateBookingStatusReturnsImmutableCopyAndSetsCancellationDateForTerminalStatuses() {
		Booking booking = Booking.create(
			1,
			"booker",
			"010-1234-5678",
			BookingStatus.CHECKING_PAYMENT,
			"990101",
			"1234",
			null,
			null,
			null,
			2L,
			3L
		);

		Booking updated = booking.updateBookingStatus(BookingStatus.BOOKING_CANCELLED);

		assertAll(
			() -> assertNotEquals(booking, updated),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, booking.getBookingStatus()),
			() -> assertNull(booking.getCancellationDate()),
			() -> assertEquals(BookingStatus.BOOKING_CANCELLED, updated.getBookingStatus()),
			() -> assertNotNull(updated.getCancellationDate())
		);
	}

	@Test
	void updateBookingStatusPreservesExistingCancellationDateForRepeatedTerminalStatuses() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
		LocalDateTime cancellationDate = LocalDateTime.of(2026, 1, 2, 12, 0);
		Booking booking = Booking.rehydrate(
			10L,
			1,
			"booker",
			"010-1234-5678",
			BookingStatus.BOOKING_CANCELLED,
			createdAt,
			cancellationDate,
			"990101",
			"1234",
			null,
			null,
			null,
			2L,
			3L
		);

		Booking updated = booking.updateBookingStatus(BookingStatus.BOOKING_DELETED);

		assertAll(
			() -> assertNotEquals(booking, updated),
			() -> assertEquals(BookingStatus.BOOKING_DELETED, updated.getBookingStatus()),
			() -> assertEquals(cancellationDate, updated.getCancellationDate())
		);
	}

	@Test
	void updateRefundInfoReturnsImmutableCopyWithRefundStatus() {
		Booking booking = Booking.create(
			1,
			"booker",
			"010-1234-5678",
			BookingStatus.CHECKING_PAYMENT,
			"990101",
			"1234",
			null,
			null,
			null,
			2L,
			3L
		);

		Booking updated = booking.updateRefundInfo(BankName.NH_NONGHYUP, "123-456", "holder");

		assertAll(
			() -> assertNotEquals(booking, updated),
			() -> assertNull(booking.getBankName()),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, booking.getBookingStatus()),
			() -> assertEquals(BankName.NH_NONGHYUP, updated.getBankName()),
			() -> assertEquals("123-456", updated.getAccountNumber()),
			() -> assertEquals("holder", updated.getAccountHolder()),
			() -> assertEquals(BookingStatus.REFUND_REQUESTED, updated.getBookingStatus())
		);
	}
}
