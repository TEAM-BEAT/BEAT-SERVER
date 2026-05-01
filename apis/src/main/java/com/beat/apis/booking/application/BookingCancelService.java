package com.beat.apis.booking.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.booking.application.dto.BookingCancelRequest;
import com.beat.apis.booking.application.dto.BookingCancelResponse;
import com.beat.apis.booking.application.dto.BookingRefundRequest;
import com.beat.apis.booking.application.dto.BookingRefundResponse;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import com.beat.apis.booking.application.exception.BookingApplicationErrorCode;
import com.beat.apis.schedule.application.exception.ScheduleApplicationErrorCode;

@Service
@RequiredArgsConstructor
public class BookingCancelService {

	private final BookingRepository bookingRepository;
	private final ScheduleRepository scheduleRepository;

	public BookingRefundResponse refundBooking(BookingRefundRequest request) {
		Booking booking = bookingRepository.findById(request.bookingId())
			.orElseThrow(() -> new NotFoundException(BookingApplicationErrorCode.NO_BOOKING_FOUND));

		booking = booking.updateRefundInfo(request.bankName(), request.accountNumber(), request.accountHolder());
		booking = bookingRepository.save(booking);

		return BookingRefundResponse.of(
			booking.getId(),
			booking.getBookingStatus(),
			booking.getBankName(),
			booking.getAccountNumber(),
			booking.getAccountHolder()
		);
	}

	@Transactional
	public BookingCancelResponse cancelBooking(BookingCancelRequest request) {
		Booking booking = bookingRepository.findById(request.bookingId())
			.orElseThrow(() -> new NotFoundException(BookingApplicationErrorCode.NO_BOOKING_FOUND));

		booking = booking.updateBookingStatus(BookingStatus.BOOKING_CANCELLED);
		booking = bookingRepository.save(booking);

		Schedule schedule = scheduleRepository.lockById(booking.getScheduleId())
			.orElseThrow(() -> new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND));
		scheduleRepository.save(schedule.decreaseSoldTicketCount(booking.getPurchaseTicketCount()));

		return BookingCancelResponse.of(
			booking.getId(),
			booking.getBookingStatus()
		);
	}
}
