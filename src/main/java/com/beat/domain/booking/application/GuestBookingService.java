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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestBookingService {

    private static final Logger logger = LoggerFactory.getLogger(GuestBookingService.class);

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

        schedule.setSoldTicketCount(schedule.getSoldTicketCount() + guestBookingRequest.purchaseTicketCount());

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
                guestBookingRequest.isPaymentCompleted(),
                guestBookingRequest.birthDate(),
                guestBookingRequest.password(),
                schedule,
                users
        );
        bookingRepository.save(booking);

        logger.info("Booking created: {}", booking);

        return GuestBookingResponse.of(
                booking.getId(),
                schedule.getId(),
                booking.getUsers().getId(),
                booking.getPurchaseTicketCount(),
                schedule.getScheduleNumber(),
                booking.getBookerName(),
                booking.getBookerPhoneNumber(),
                booking.isPaymentCompleted(),
                schedule.getPerformance().getBankName(),
                schedule.getPerformance().getAccountNumber(),
                totalPaymentAmount, // 회원 예매랑 다른 부분 확인하기
                booking.getCreatedAt()
        );
    }
}