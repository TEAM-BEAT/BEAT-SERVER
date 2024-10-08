package com.beat.domain.booking.application;

import com.beat.domain.booking.application.dto.GuestBookingRequest;
import com.beat.domain.booking.application.dto.GuestBookingResponse;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestBookingService {

    private final ScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public GuestBookingResponse createGuestBooking(GuestBookingRequest guestBookingRequest) {
        Schedule schedule = scheduleRepository.lockById(guestBookingRequest.scheduleId())
                .orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));

        int availableTicketCount = schedule.getTotalTicketCount() - schedule.getSoldTicketCount();
        if (availableTicketCount < guestBookingRequest.purchaseTicketCount()) {
            throw new BadRequestException(ScheduleErrorCode.INSUFFICIENT_TICKETS);
        }

        updateSoldTicketCountAndIsBooking(schedule, guestBookingRequest.purchaseTicketCount());

        Users users = bookingRepository.findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
                guestBookingRequest.bookerName(),
                guestBookingRequest.bookerPhoneNumber(),
                guestBookingRequest.birthDate(),
                guestBookingRequest.password()
        ).map(Booking::getUsers).orElseGet(() -> {
            Users newUser = Users.create();
            userRepository.save(newUser);
            return newUser;
        });

        int ticketPrice = schedule.getPerformance().getTicketPrice();
        int totalPaymentAmount = ticketPrice * guestBookingRequest.purchaseTicketCount();
        scheduleRepository.save(schedule);

        Booking booking = Booking.create(
                guestBookingRequest.purchaseTicketCount(),
                guestBookingRequest.bookerName(),
                guestBookingRequest.bookerPhoneNumber(),
                guestBookingRequest.bookingStatus(),
                guestBookingRequest.birthDate(),
                guestBookingRequest.password(),
                schedule,
                users
        );
        bookingRepository.save(booking);

        log.info("Guest Booking created: {}", booking);

        return GuestBookingResponse.of(
                booking.getId(),
                schedule.getId(),
                booking.getUsers().getId(),
                booking.getPurchaseTicketCount(),
                schedule.getScheduleNumber(),
                booking.getBookerName(),
                booking.getBookerPhoneNumber(),
                booking.getBookingStatus(),
                schedule.getPerformance().getBankName(),
                schedule.getPerformance().getAccountNumber(),
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