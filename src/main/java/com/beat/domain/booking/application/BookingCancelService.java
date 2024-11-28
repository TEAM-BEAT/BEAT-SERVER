package com.beat.domain.booking.application;

import org.springframework.stereotype.Service;

import com.beat.domain.booking.application.dto.BookingCancelRequest;
import com.beat.domain.booking.application.dto.BookingCancelResponse;
import com.beat.domain.booking.application.dto.BookingRefundRequest;
import com.beat.domain.booking.application.dto.BookingRefundResponse;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.exception.BookingErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingCancelService {

	private final BookingRepository bookingRepository;
	private final ScheduleRepository scheduleRepository;

	public BookingRefundResponse refundBooking(BookingRefundRequest request) {
		Booking booking = bookingRepository.findById(request.bookingId())
			.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

		booking.updateRefundInfo(request.bankName(), request.accountNumber(), request.accountHolder());
		bookingRepository.save(booking);

		return BookingRefundResponse.of(
			booking.getId(),
			booking.getBookingStatus(),
			booking.getBankName(),
			booking.getAccountNumber(),
			booking.getAccountHolder()
		);
	}

	public BookingCancelResponse cancelBooking(BookingCancelRequest request) {
		Booking booking = bookingRepository.findById(request.bookingId())
			.orElseThrow(() -> new NotFoundException(BookingErrorCode.NO_BOOKING_FOUND));

		booking.updateBookingStatus(BookingStatus.BOOKING_CANCELLED);
		bookingRepository.save(booking);

		Schedule schedule = booking.getSchedule();
		schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount());
		scheduleRepository.save(schedule);

		return BookingCancelResponse.of(
			booking.getId(),
			booking.getBookingStatus()
		);
	}
}
