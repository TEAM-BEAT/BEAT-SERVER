package com.beat.apis.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.beat.apis.booking.application.GuestBookingService;
import com.beat.apis.booking.application.MemberBookingService;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.booking.application.dto.GuestBookingRequest;
import com.beat.apis.booking.application.dto.MemberBookingRequest;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BookingCreationStatusServiceTest {

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PerformanceRepository performanceRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private GuestBookingService guestBookingService;
	private MemberBookingService memberBookingService;

	@BeforeEach
	void setUp() {
		guestBookingService = new GuestBookingService(
			scheduleRepository,
			bookingRepository,
			userRepository,
			performanceRepository,
			eventPublisher
		);
		memberBookingService = new MemberBookingService(
			scheduleRepository,
			bookingRepository,
			memberRepository,
			userRepository,
			performanceRepository,
			eventPublisher
		);
	}

	@Test
	void createGuestBookingShouldIgnoreClientBookingStatusAndStartWithCheckingPayment() {
		Schedule schedule = schedule();
		Performance performance = performance();
		Users user = Users.rehydrate(30L, Role.USER);
		GuestBookingRequest request = GuestBookingRequest.of(
			1L,
			1,
			ScheduleNumberType.FIRST,
			"booker",
			"010-0000-0000",
			"990101",
			"password",
			10000,
			BookingStatusType.BOOKING_CONFIRMED
		);

		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule));
		when(bookingRepository.findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
			"booker", "010-0000-0000", "990101", "password"
		)).thenReturn(Optional.empty());
		when(userRepository.save(any(Users.class))).thenReturn(user);
		when(performanceRepository.findById(20L)).thenReturn(Optional.of(performance));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		guestBookingService.createGuestBooking(request);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		assertEquals(BookingStatus.CHECKING_PAYMENT, bookingCaptor.getValue().getBookingStatus());
	}

	@Test
	void createMemberBookingShouldIgnoreClientBookingStatusAndStartWithCheckingPayment() {
		Schedule schedule = schedule();
		Performance performance = performance();
		Member member = Member.rehydrate(10L, "nickname", "email@test.com", null, 30L, 123L, SocialType.KAKAO);
		MemberBookingRequest request = new MemberBookingRequest(
			1L,
			ScheduleNumberType.FIRST,
			1,
			"booker",
			"010-0000-0000",
			BookingStatusType.BOOKING_DELETED,
			10000
		);

		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule));
		when(performanceRepository.findById(20L)).thenReturn(Optional.of(performance));
		when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		memberBookingService.createMemberBooking(10L, request);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		assertEquals(BookingStatus.CHECKING_PAYMENT, bookingCaptor.getValue().getBookingStatus());
	}

	private Schedule schedule() {
		return Schedule.rehydrate(
			1L,
			LocalDateTime.now().plusDays(1),
			10,
			0,
			true,
			ScheduleNumber.FIRST,
			20L
		);
	}

	private Performance performance() {
		return Performance.rehydrate(
			20L,
			"Performance Title",
			Genre.BAND,
			120,
			"description",
			"attention",
			BankName.BUSAN,
			"123-456",
			"holder",
			"poster.jpg",
			"team",
			"venue",
			"road",
			"detail",
			"37.1",
			"127.1",
			"010-1111-1111",
			"2024-01-01~2024-01-02",
			10000,
			1,
			30L
		);
	}
}
