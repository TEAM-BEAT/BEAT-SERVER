package com.beat.domain.booking;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import com.beat.domain.booking.model.Booking;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.booking.model.BookingStatus;
import com.beat.domain.booking.vo.RefundAccount;
import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.domain.exception.DomainException;

class BookingDomainInvariantTest {

	@Test
	void createRejectsNonPositivePurchaseTicketCount() {
		DomainException exception = assertThrows(DomainException.class, () -> Booking.create(
			0,
			"booker",
			"010-1234-5678",
			"990101",
			"1234",
			2L,
			3L,
			LocalDateTime.of(2026, 1, 1, 12, 0)
		));

		assertEquals(BookingErrorCode.INVALID_PURCHASE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void createAcceptsOneToTenTicketsAndRejectsMoreThanTen() {
		Booking minimum = booking(1);
		Booking maximum = booking(10);
		DomainException exception = assertThrows(DomainException.class, () -> booking(11));

		assertAll(
			() -> assertEquals(1, minimum.getPurchaseTicketCount()),
			() -> assertEquals(10, maximum.getPurchaseTicketCount()),
			() -> assertEquals(BookingErrorCode.PURCHASE_TICKET_COUNT_EXCEEDED, exception.getErrorCode())
		);
	}

	@Test
	void createConfirmsFreeBookingAndLeavesPaidBookingCheckingPayment() {
		Booking free = Booking.create(
			1, "booker", "010-1234-5678", null, null, 2L, 3L,
			LocalDateTime.of(2026, 1, 1, 12, 0), 0
		);
		Booking paid = Booking.create(
			1, "booker", "010-1234-5678", null, null, 2L, 3L,
			LocalDateTime.of(2026, 1, 1, 12, 0), 10000
		);
		Booking paymentAmountUnknown = Booking.create(
			1, "booker", "010-1234-5678", null, null, 2L, 3L,
			LocalDateTime.of(2026, 1, 1, 12, 0), null
		);

		assertAll(
			() -> assertEquals(BookingStatus.BOOKING_CONFIRMED, free.getBookingStatus()),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, paid.getBookingStatus()),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, paymentAmountUnknown.getBookingStatus())
		);
	}

	@Test
	void toStringDoesNotExposeGuestPersonalOrAuthenticationData() {
		Booking booking = Booking.create(
			1,
			"private-name",
			"010-9876-5432",
			"990101",
			"secret-password",
			2L,
			3L,
			LocalDateTime.of(2026, 1, 1, 12, 0)
		);

		String rendered = booking.toString();

		assertAll(
			() -> assertFalse(rendered.contains("private-name")),
			() -> assertFalse(rendered.contains("010-9876-5432")),
			() -> assertFalse(rendered.contains("990101")),
			() -> assertFalse(rendered.contains("secret-password"))
		);
	}

	@Test
	void createRejectsNullScheduleId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Booking.create(
			1,
			"booker",
			"010-1234-5678",
			"990101",
			"1234",
			null,
			1L,
			LocalDateTime.of(2026, 1, 1, 12, 0)
		));

		assertEquals("scheduleId must not be null", exception.getMessage());
	}

	@Test
	void createRejectsNullUserId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Booking.create(
			1,
			"booker",
			"010-1234-5678",
			"990101",
			"1234",
			2L,
			null,
			LocalDateTime.of(2026, 1, 1, 12, 0)
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
			RefundAccount.of(BankName.KAKAOBANK, "111-222", "holder"),
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
	void refundAccountRejectsNoneBank() {
		assertThrows(DomainException.class, () -> RefundAccount.of(BankName.NONE, "111-222", "holder"));
	}

	@Test
	void cancelReturnsImmutableCopyAndUsesSuppliedCancellationTime() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
		LocalDateTime cancelledAt = LocalDateTime.of(2026, 1, 2, 12, 0);
		Booking booking = Booking.create(
			1,
			"booker",
			"010-1234-5678",
			"990101",
			"1234",
			2L,
			3L,
			createdAt
		);

		Booking updated = booking.cancelUnpaidOrFree(cancelledAt);

		assertAll(
			() -> assertNotSame(booking, updated),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, booking.getBookingStatus()),
			() -> assertNull(booking.getCancellationDate()),
			() -> assertEquals(BookingStatus.BOOKING_CANCELLED, updated.getBookingStatus()),
			() -> assertEquals(cancelledAt, updated.getCancellationDate())
		);
	}

	@Test
	void deletePreservesExistingCancellationDateAfterCancellation() {
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
			2L,
			3L
		);

		Booking updated = booking.delete();

		assertAll(
			() -> assertNotSame(booking, updated),
			() -> assertEquals(BookingStatus.BOOKING_DELETED, updated.getBookingStatus()),
			() -> assertEquals(cancellationDate, updated.getCancellationDate())
		);
	}

	@Test
	void deleteOnlyAcceptsCancelledAndDeletedBookings() {
		LocalDateTime deletedAt = LocalDateTime.of(2026, 1, 2, 12, 0);
		Booking checking = booking();
		Booking cancelled = checking.cancelUnpaidOrFree(deletedAt.minusHours(1));
		Booking deleted = cancelled.delete();
		Booking confirmed = checking.confirmPayment();
		Booking refundRequested = confirmed.requestRefund(
			RefundAccount.of(BankName.NH_NONGHYUP, "123-456", "holder")
		);

		DomainException checkingError = assertThrows(DomainException.class,
			() -> checking.delete());
		DomainException confirmedError = assertThrows(DomainException.class,
			() -> confirmed.delete());
		DomainException refundError = assertThrows(DomainException.class,
			() -> refundRequested.delete());

		assertAll(
			() -> assertEquals(BookingStatus.BOOKING_DELETED, deleted.getBookingStatus()),
			() -> assertEquals(deletedAt.minusHours(1), deleted.getCancellationDate()),
			() -> assertEquals(BookingStatus.BOOKING_DELETED, cancelled.delete().getBookingStatus()),
			() -> assertSame(deleted, deleted.delete()),
			() -> assertEquals(BookingErrorCode.DELETION_NOT_ALLOWED, checkingError.getErrorCode()),
			() -> assertEquals(BookingErrorCode.DELETION_NOT_ALLOWED, confirmedError.getErrorCode()),
			() -> assertEquals(BookingErrorCode.DELETION_NOT_ALLOWED, refundError.getErrorCode())
		);
	}

	@Test
	void requestRefundReturnsImmutableCopyWithRefundStatus() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
		Booking booking = Booking.create(
			1,
			"booker",
			"010-1234-5678",
			"990101",
			"1234",
			2L,
			3L,
			createdAt
		);

		Booking confirmed = booking.confirmPayment();
		Booking updated = confirmed.requestRefund(RefundAccount.of(BankName.NH_NONGHYUP, "123-456", "holder"));

		assertAll(
			() -> assertNotSame(confirmed, updated),
			() -> assertNull(booking.getBankName()),
			() -> assertEquals(BookingStatus.CHECKING_PAYMENT, booking.getBookingStatus()),
			() -> assertEquals(BookingStatus.BOOKING_CONFIRMED, confirmed.getBookingStatus()),
			() -> assertEquals(BankName.NH_NONGHYUP, updated.getBankName()),
			() -> assertEquals("123-456", updated.getAccountNumber()),
			() -> assertEquals("holder", updated.getAccountHolder()),
			() -> assertEquals(BookingStatus.REFUND_REQUESTED, updated.getBookingStatus())
		);
	}

	@Test
	void transitionToConfirmsCheckingPaymentAndIsIdempotent() {
		Booking booking = Booking.create(1, "booker", "010-1234-5678", "990101", "1234", 2L, 3L,
			LocalDateTime.of(2026, 1, 1, 12, 0));

		Booking confirmed = booking.transitionTo(BookingStatus.BOOKING_CONFIRMED);

		assertEquals(BookingStatus.BOOKING_CONFIRMED, confirmed.getBookingStatus());
		assertSame(confirmed, confirmed.transitionTo(BookingStatus.BOOKING_CONFIRMED));
	}

	@Test
	void transitionToRejectsChangingConfirmedBooking() {
		Booking confirmed = Booking.create(1, "booker", "010-1234-5678", "990101", "1234", 2L, 3L,
			LocalDateTime.of(2026, 1, 1, 12, 0)).confirmPayment();

		DomainException exception = assertThrows(DomainException.class,
			() -> confirmed.transitionTo(BookingStatus.BOOKING_CANCELLED));

		assertEquals(BookingErrorCode.CONFIRMED_STATUS_CHANGE_NOT_ALLOWED, exception.getErrorCode());
	}

	@Test
	void confirmPaymentIsIdempotentAndRejectsTerminalStatus() {
		Booking booking = booking();
		Booking confirmed = booking.confirmPayment();
		Booking cancelled = booking.cancelUnpaidOrFree(LocalDateTime.of(2026, 1, 2, 12, 0));

		assertSame(confirmed, confirmed.confirmPayment());
		DomainException exception = assertThrows(DomainException.class,
			cancelled::confirmPayment);
		assertEquals(BookingErrorCode.PAYMENT_CONFIRMATION_NOT_ALLOWED, exception.getErrorCode());
	}

	@Test
	void requestRefundIsIdempotentOnlyForSameAccount() {
		Booking booking = booking();
		RefundAccount account = RefundAccount.of(BankName.NH_NONGHYUP, "123-456", "holder");
		Booking requested = booking.requestRefund(account);

		assertSame(requested, requested.requestRefund(account));
		DomainException accountChange = assertThrows(DomainException.class,
			() -> requested.requestRefund(RefundAccount.of(BankName.KAKAOBANK, "999", "other")));
		assertAll(
			() -> assertEquals(BookingStatus.REFUND_REQUESTED, requested.getBookingStatus()),
			() -> assertEquals(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED, accountChange.getErrorCode())
		);
	}

	@Test
	void requestRefundRejectsConfirmedFreeBooking() {
		Booking free = Booking.create(
			1, "booker", "010-1234-5678", null, null, 2L, 3L,
			LocalDateTime.of(2026, 1, 1, 12, 0), 0
		);
		RefundAccount account = RefundAccount.of(BankName.NH_NONGHYUP, "123-456", "holder");

		DomainException exception = assertThrows(DomainException.class,
			() -> free.requestRefund(account));

		assertEquals(BookingErrorCode.REFUND_REQUEST_NOT_ALLOWED, exception.getErrorCode());
	}

	@Test
	void generalCancellationRejectsConfirmedAndRefundRequestedBookings() {
		Booking confirmed = booking().confirmPayment();
		Booking refundRequested = confirmed.requestRefund(
			RefundAccount.of(BankName.NH_NONGHYUP, "123-456", "holder")
		);

		DomainException confirmedError = assertThrows(DomainException.class,
			() -> confirmed.cancelUnpaidOrFree(LocalDateTime.of(2026, 1, 2, 12, 0)));
		DomainException refundError = assertThrows(DomainException.class,
			() -> refundRequested.cancelUnpaidOrFree(LocalDateTime.of(2026, 1, 2, 12, 0)));

		assertAll(
			() -> assertEquals(BookingErrorCode.CANCELLATION_NOT_ALLOWED, confirmedError.getErrorCode()),
			() -> assertEquals(BookingErrorCode.CANCELLATION_NOT_ALLOWED, refundError.getErrorCode())
		);
	}

	@Test
	void generalCancellationAcceptsConfirmedFreeBooking() {
		LocalDateTime cancelledAt = LocalDateTime.of(2026, 1, 2, 12, 0);
		Booking free = Booking.create(
			1, "booker", "010-1234-5678", null, null, 2L, 3L,
			LocalDateTime.of(2026, 1, 1, 12, 0), 0
		);

		Booking cancelled = free.cancelUnpaidOrFree(cancelledAt);

		assertAll(
			() -> assertEquals(BookingStatus.BOOKING_CANCELLED, cancelled.getBookingStatus()),
			() -> assertEquals(cancelledAt, cancelled.getCancellationDate())
		);
	}

	@Test
	void refundCompletionOnlyAcceptsRefundRequestedBookingAndIsIdempotent() {
		LocalDateTime completedAt = LocalDateTime.of(2026, 1, 2, 12, 0);
		Booking confirmed = booking().confirmPayment();
		Booking requested = confirmed.requestRefund(
			RefundAccount.of(BankName.NH_NONGHYUP, "123-456", "holder")
		);

		Booking completed = requested.completeRefund(completedAt);
		DomainException error = assertThrows(DomainException.class,
			() -> confirmed.completeRefund(completedAt));

		assertAll(
			() -> assertEquals(BookingStatus.BOOKING_CANCELLED, completed.getBookingStatus()),
			() -> assertEquals(completedAt, completed.getCancellationDate()),
			() -> assertSame(completed, completed.completeRefund(completedAt.plusDays(1))),
			() -> assertEquals(BookingErrorCode.REFUND_COMPLETION_NOT_ALLOWED, error.getErrorCode())
		);
	}

	@Test
	void repeatedCancellationIsIdempotent() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
		LocalDateTime cancelledAt = LocalDateTime.of(2026, 1, 2, 12, 0);
		Booking booking = Booking.create(1, "booker", "010-1234-5678", "990101", "1234", 2L, 3L, createdAt);

		Booking cancelled = booking.cancelUnpaidOrFree(cancelledAt);
		Booking repeated = cancelled.cancelUnpaidOrFree(cancelledAt.plusDays(1));

		assertAll(
			() -> assertEquals(cancelled, repeated),
			() -> assertEquals(cancelledAt, repeated.getCancellationDate()),
			() -> assertEquals(false, repeated.hasActiveTicketAllocation())
		);
	}

	@Test
	void refundAccountRejectsPartialOrBlankValues() {
		assertAll(
			() -> assertThrows(DomainException.class, () -> RefundAccount.fromNullable(BankName.KAKAOBANK, null, "holder")),
			() -> assertThrows(DomainException.class, () -> RefundAccount.of(BankName.KAKAOBANK, " ", "holder")),
			() -> assertNull(RefundAccount.fromNullable(null, null, null))
		);
	}

	private Booking booking() {
		return booking(1);
	}

	private Booking booking(int purchaseTicketCount) {
		return Booking.create(
			purchaseTicketCount,
			"booker",
			"010-1234-5678",
			"990101",
			"1234",
			2L,
			3L,
			LocalDateTime.of(2026, 1, 1, 12, 0)
		);
	}
}
