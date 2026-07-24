package com.beat.apis.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.beat.apis.booking.application.command.GuestBookingCommandService;
import com.beat.apis.booking.application.command.GuestBookingCommand;
import com.beat.apis.booking.application.credential.GuestBookingCredentialAuthenticator;
import com.beat.apis.booking.application.command.MemberBookingCommandService;
import com.beat.apis.booking.application.command.MemberBookingCommand;
import com.beat.apis.booking.exception.BookingApplicationErrorCode;
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode;
import com.beat.domain.booking.model.Booking;
import com.beat.domain.booking.model.BookingStatus;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.exception.DomainException;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.member.model.Member;
import com.beat.domain.member.vo.SocialIdentity;
import com.beat.domain.member.model.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.domain.performance.model.Genre;
import com.beat.contracts.performance.PerformanceSummaryReadPort;
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel;
import com.beat.domain.performance.vo.PaymentAccount;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.user.model.Role;
import com.beat.domain.user.model.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.apis.exception.ApiApplicationException;

@ExtendWith(MockitoExtension.class)
class BookingCreationStatusServiceTest {

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PerformanceSummaryReadPort performanceSummaryReadPort;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private GuestBookingCredentialAuthenticator credentialAuthenticator;

	private GuestBookingCommandService guestBookingService;
	private MemberBookingCommandService memberBookingService;

	@BeforeEach
	void setUp() {
		guestBookingService = new GuestBookingCommandService(
			scheduleRepository,
			bookingRepository,
			userRepository,
			performanceSummaryReadPort,
			eventPublisher,
			credentialAuthenticator,
			Clock.systemUTC()
		);
		memberBookingService = new MemberBookingCommandService(
			scheduleRepository,
			bookingRepository,
			memberRepository,
			performanceSummaryReadPort,
			eventPublisher,
			Clock.systemUTC()
		);
	}

	@Test
	void createGuestBookingShouldIgnoreClientBookingStatusAndStartWithCheckingPayment() {
		Schedule schedule = schedule();
		PerformanceSummaryReadModel performance = performance();
		Users user = Users.rehydrate(30L, Role.USER);
		GuestBookingCommand request = GuestBookingCommand.of(
			1L,
			1,
			"booker",
			"010-0000-0000",
			"990101",
			"1234"
		);

		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule));
		when(scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(true);
		when(credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234")).thenReturn(null);
		when(credentialAuthenticator.encode("1234")).thenReturn("encoded-password");
		when(userRepository.save(any(Users.class))).thenReturn(user);
		when(performanceSummaryReadPort.findById(20L)).thenReturn(Optional.of(performance));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = guestBookingService.createGuestBooking(request);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		assertEquals(BookingStatus.CHECKING_PAYMENT, bookingCaptor.getValue().getBookingStatus());
		assertEquals("encoded-password", bookingCaptor.getValue().getPassword());
		assertEquals(10_000, response.getTotalPaymentAmount());
	}

	@Test
	void createMemberBookingShouldIgnoreClientBookingStatusAndStartWithCheckingPayment() {
		Schedule schedule = schedule();
		PerformanceSummaryReadModel performance = performance();
		Member member = Member.rehydrate(10L, "nickname", "email@test.com", null, 30L, SocialIdentity.of(SocialType.KAKAO, 123L));
		MemberBookingCommand request = MemberBookingCommand.of(
			1L,
			1,
			"booker",
			"010-0000-0000"
		);

		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule));
		when(scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(true);
		when(performanceSummaryReadPort.findById(20L)).thenReturn(Optional.of(performance));
		when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = memberBookingService.createMemberBooking(10L, request);

		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		assertEquals(BookingStatus.CHECKING_PAYMENT, bookingCaptor.getValue().getBookingStatus());
		assertEquals(10_000, response.getTotalPaymentAmount());
	}

	@Test
	void createMemberBookingRejectsRequestWhenDatabaseCloseTimeHasPassed() {
		Schedule schedule = schedule();
		Member member = Member.rehydrate(10L, "nickname", "email@test.com", null, 30L, SocialIdentity.of(SocialType.KAKAO, 123L));
		MemberBookingCommand request = MemberBookingCommand.of(
			1L,
			1,
			"booker",
			"010-0000-0000"
		);

		when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule));
		when(scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(false);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () -> memberBookingService.createMemberBooking(10L, request));

		assertEquals(ScheduleApplicationErrorCode.BOOKING_CLOSED, exception.getErrorCode());
	}

	@Test
	void createMemberBookingPreservesLegacyBadRequestForInsufficientTickets() {
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
		Schedule soldOutSchedule = Schedule.rehydrate(
			1L, performanceDate, performanceDate.plusHours(2), 10, 10, ScheduleNumber.FIRST, 20L);
		Member member = Member.rehydrate(
			10L, "nickname", "email@test.com", null, 30L, SocialIdentity.of(SocialType.KAKAO, 123L));
		MemberBookingCommand request = MemberBookingCommand.of(
			1L, 1, "booker", "010-0000-0000");

		when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(soldOutSchedule));
		when(scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(true);

		DomainException exception = assertThrows(DomainException.class,
			() -> memberBookingService.createMemberBooking(10L, request));

		assertEquals(ScheduleErrorCode.INSUFFICIENT_TICKETS, exception.getErrorCode());
	}

	@Test
	void createMemberBookingLeavesZeroTicketValidationToDomain() {
		Member member = Member.rehydrate(
			10L, "nickname", "email@test.com", null, 30L, SocialIdentity.of(SocialType.KAKAO, 123L));
		MemberBookingCommand request = MemberBookingCommand.of(
			1L, 0, "booker", "010-0000-0000");

		when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule()));
		when(scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(true);

		DomainException exception = assertThrows(DomainException.class,
			() -> memberBookingService.createMemberBooking(10L, request));

		assertEquals(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void createGuestBookingLeavesNegativeTicketValidationToDomain() {
		GuestBookingCommand request = GuestBookingCommand.of(
			1L, -1, "booker", "010-0000-0000", "990101", "1234");

		when(credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234")).thenReturn(null);
		when(userRepository.save(any(Users.class))).thenReturn(Users.rehydrate(30L, Role.USER));
		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule()));
		when(scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(true);

		DomainException exception = assertThrows(DomainException.class,
			() -> guestBookingService.createGuestBooking(request));

		assertEquals(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void createGuestBookingRejectsRequestWhenDatabaseCloseTimeHasPassed() {
		Schedule schedule = schedule();
		Users user = Users.rehydrate(30L, Role.USER);
		GuestBookingCommand request = GuestBookingCommand.of(
			1L,
			1,
			"booker",
			"010-0000-0000",
			"990101",
			"1234"
		);

		when(credentialAuthenticator.findUserId("booker", "010-0000-0000", "990101", "1234")).thenReturn(null);
		when(userRepository.save(any(Users.class))).thenReturn(user);
		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule));
		when(scheduleRepository.isBeforeBookingCloseAt(1L)).thenReturn(false);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () -> guestBookingService.createGuestBooking(request));

		assertEquals(ScheduleApplicationErrorCode.BOOKING_CLOSED, exception.getErrorCode());
	}

	private Schedule schedule() {
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
		return Schedule.rehydrate(
			1L,
			performanceDate,
			performanceDate.plusHours(2),
			10,
			0,
			ScheduleNumber.FIRST,
			20L
		);
	}

	private PerformanceSummaryReadModel performance() {
		return new PerformanceSummaryReadModel(
			20L,
			30L,
			"Performance Title",
			"BAND",
			10000,
			"BUSAN",
			"123-456",
			"holder",
			"poster.jpg",
			"team",
			"venue",
			"010-1111-1111",
			1,
			LocalDate.of(2024, 1, 1),
			LocalDate.of(2024, 1, 2)
		);
	}
}
