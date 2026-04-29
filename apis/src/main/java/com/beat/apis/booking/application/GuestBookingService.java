package com.beat.apis.booking.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.booking.application.dto.GuestBookingRequest;
import com.beat.apis.booking.application.dto.GuestBookingResponse;
import com.beat.apis.booking.application.dto.event.BookingCreatedEvent;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestBookingService {

	private final ScheduleRepository scheduleRepository;
	private final BookingRepository bookingRepository;
	private final UserRepository userRepository;
	private final PerformanceRepository performanceRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public GuestBookingResponse createGuestBooking(GuestBookingRequest guestBookingRequest) {
		Schedule schedule = scheduleRepository.lockById(guestBookingRequest.scheduleId())
			.orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));

		int availableTicketCount = schedule.getTotalTicketCount() - schedule.getSoldTicketCount();
		if (availableTicketCount < guestBookingRequest.purchaseTicketCount()) {
			throw new BadRequestException(ScheduleErrorCode.INSUFFICIENT_TICKETS);
		}

		updateSoldTicketCountAndIsBooking(schedule, guestBookingRequest.purchaseTicketCount());

		Long userId = bookingRepository.findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
			guestBookingRequest.bookerName(),
			guestBookingRequest.bookerPhoneNumber(),
			guestBookingRequest.birthDate(),
			guestBookingRequest.password()
		).map(Booking::getUserId).orElseGet(() -> {
			Users newUser = Users.create();
			Users savedUser = userRepository.save(newUser);
			return savedUser.getId();
		});

		Performance performance = performanceRepository.findById(schedule.getPerformanceId())
			.orElseThrow(() -> new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
		int ticketPrice = performance.getTicketPrice();
		int totalPaymentAmount = ticketPrice * guestBookingRequest.purchaseTicketCount();
		scheduleRepository.save(schedule);

		Booking booking = Booking.create(
			guestBookingRequest.purchaseTicketCount(),
			guestBookingRequest.bookerName(),
			guestBookingRequest.bookerPhoneNumber(),
			guestBookingRequest.bookingStatus(),
			guestBookingRequest.birthDate(),
			guestBookingRequest.password(),
			null,
			null,
			null,
			schedule,
			userId
		);
		bookingRepository.save(booking);

		log.info("Guest Booking created: {}", booking);

		eventPublisher.publishEvent(BookingCreatedEvent.of(booking, schedule, performance.getPerformanceTitle()));

		return GuestBookingResponse.of(
			booking.getId(),
			schedule.getId(),
			booking.getUserId(),
			booking.getPurchaseTicketCount(),
			schedule.getScheduleNumber(),
			booking.getBookerName(),
			booking.getBookerPhoneNumber(),
			booking.getBookingStatus(),
			performance.getBankName(),
			performance.getAccountNumber(),
			totalPaymentAmount,
			booking.getCreatedAt()
		);
	}

	private void updateSoldTicketCountAndIsBooking(Schedule schedule, int purchaseTicketCount) {
		schedule.setSoldTicketCount(schedule.getSoldTicketCount() + purchaseTicketCount);

		if (schedule.getTotalTicketCount() == schedule.getSoldTicketCount()) {
			schedule.updateIsBooking(false);
		}
	}
}
