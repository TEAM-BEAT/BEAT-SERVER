package com.beat.apis.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.booking.application.command.BookingCancellationCommandService;
import com.beat.apis.booking.application.command.BookingCancelCommand;
import com.beat.apis.booking.application.command.BookingRefundCommand;
import com.beat.apis.booking.exception.BookingApplicationErrorCode;
import com.beat.apis.exception.ApiApplicationException;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.booking.model.Booking;
import com.beat.domain.booking.model.BookingStatus;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.exception.DomainException;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.repository.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class BookingCancelServiceTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private ScheduleRepository scheduleRepository;

	private BookingCancellationCommandService service;

	@BeforeEach
	void setUp() {
		service = new BookingCancellationCommandService(bookingRepository, Clock.systemUTC(), scheduleRepository);
	}

	@Test
	void repeatedCancellationDoesNotReleaseInventoryAgain() {
		Booking cancelled = booking(BookingStatus.BOOKING_CANCELLED, LocalDateTime.of(2026, 1, 2, 12, 0));
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(cancelled));
		when(bookingRepository.lockById(1L)).thenReturn(Optional.of(cancelled));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.cancelBooking(20L, BookingCancelCommand.from(1L));

		verify(bookingRepository).lockById(1L);
		verify(scheduleRepository, never()).lockById(any());
		verify(scheduleRepository, never()).save(any(Schedule.class));
	}

	@Test
	void activeCancellationLocksScheduleAndReleasesInventoryOnce() {
		Booking active = booking(BookingStatus.CHECKING_PAYMENT, null);
		Schedule schedule = schedule();
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(active));
		when(bookingRepository.lockById(1L)).thenReturn(Optional.of(active));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(scheduleRepository.lockById(10L)).thenReturn(Optional.of(schedule));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.cancelBooking(20L, BookingCancelCommand.from(1L));

		ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
		verify(scheduleRepository).save(scheduleCaptor.capture());
		assertEquals(1, scheduleCaptor.getValue().getAllocatedTicketCount());
	}

	@Test
	void cancellationDoesNotRevealOrMutateAnotherUsersBooking() {
		Booking booking = booking(BookingStatus.CHECKING_PAYMENT, null);
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

		ApiApplicationException exception = assertThrows(
			ApiApplicationException.class,
			() -> service.cancelBooking(999L, BookingCancelCommand.from(1L))
		);

		assertEquals(BookingApplicationErrorCode.NO_BOOKING_FOUND, exception.getErrorCode());
		verify(bookingRepository, never()).lockById(any());
		verify(scheduleRepository, never()).lockById(any());
	}

	@Test
	void cancellationRejectsConfirmedBookingWithoutReleasingInventory() {
		Booking confirmed = booking(BookingStatus.BOOKING_CONFIRMED, null);
		Schedule schedule = schedule();
		when(bookingRepository.findById(1L)).thenReturn(Optional.of(confirmed));
		when(bookingRepository.lockById(1L)).thenReturn(Optional.of(confirmed));
		when(scheduleRepository.lockById(10L)).thenReturn(Optional.of(schedule));

		DomainException exception = assertThrows(
			DomainException.class,
			() -> service.cancelBooking(20L, BookingCancelCommand.from(1L))
		);

		assertEquals(BookingErrorCode.CANCELLATION_NOT_ALLOWED, exception.getErrorCode());
		verify(bookingRepository, never()).save(any());
		verify(scheduleRepository, never()).save(any());
	}

	@Test
	void checkingPaymentBookingCanRequestRefund() {
		Booking checkingPayment = booking(BookingStatus.CHECKING_PAYMENT, null);
		when(bookingRepository.lockById(1L)).thenReturn(Optional.of(checkingPayment));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var result = service.refundBooking(
			20L,
			BookingRefundCommand.of(1L, "KAKAOBANK", "123-456", "holder")
		);

		assertEquals(BookingStatus.REFUND_REQUESTED.name(), result.getBookingStatus());
		verify(scheduleRepository, never()).save(any());
	}

	private Booking booking(BookingStatus status, LocalDateTime cancelledAt) {
		return Booking.rehydrate(
			1L, 1, "booker", "010-1234-5678", status,
			LocalDateTime.of(2026, 1, 1, 12, 0), cancelledAt,
			"990101", "1234", null, 10L, 20L
		);
	}

	private Schedule schedule() {
		LocalDateTime performanceDate = LocalDateTime.of(2026, 2, 1, 18, 0);
		return Schedule.rehydrate(
			10L, performanceDate, performanceDate.plusHours(2), 10, 2,
			com.beat.domain.schedule.model.ScheduleNumber.FIRST, 30L
		);
	}
}
