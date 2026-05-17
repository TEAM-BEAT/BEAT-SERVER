package com.beat.apis.ticket.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.ticket.application.exception.TicketApplicationErrorCode;
import com.beat.contracts.booking.MakerTicketReadPort;
import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus;
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel;
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber;
import com.beat.contracts.sms.SmsPort;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.support.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private MakerTicketReadPort makerTicketReadPort;

	@Mock
	private PerformanceRepository performanceRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private SmsPort smsPort;

	private TicketService ticketService;

	@BeforeEach
	void setUp() {
		ticketService = new TicketService(
			bookingRepository,
			makerTicketReadPort,
			performanceRepository,
			memberRepository,
			userRepository,
			scheduleRepository,
			smsPort
		);
	}

	@Test
	void findAllTicketsByConditionsUsesExplicitContractEnumsAndReturnsApiStatus() {
		Member member = Member.rehydrate(1L, "maker", null, null, 10L, 123L, SocialType.KAKAO);
		Users user = Users.rehydrate(10L, Role.USER);
		Performance performance = Performance.rehydrate(
			100L,
			"title",
			Genre.BAND,
			120,
			"description",
			"attention",
			BankName.KAKAOBANK,
			"123",
			"holder",
			"poster",
			"team",
			"venue",
			"road",
			"detail",
			"0",
			"0",
			"contact",
			"period",
			10000,
			1,
			10L
		);
		Schedule schedule = Schedule.rehydrate(
			200L,
			LocalDateTime.of(2026, 1, 1, 19, 0),
			100,
			1,
			true,
			ScheduleNumber.FIRST,
			100L
		);
		MakerTicketListItemReadModel ticket = new MakerTicketListItemReadModel(
			300L,
			"booker",
			"010-0000-0000",
			200L,
			1,
			LocalDateTime.of(2026, 1, 1, 12, 0),
			MakerTicketBookingStatus.CHECKING_PAYMENT,
			"카카오뱅크",
			"123",
			"holder"
		);

		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(userRepository.findById(10L)).thenReturn(Optional.of(user));
		when(performanceRepository.findById(100L)).thenReturn(Optional.of(performance));
		when(scheduleRepository.findAllByPerformanceId(100L)).thenReturn(List.of(schedule));
		when(makerTicketReadPort.findTickets(
			100L,
			List.of(MakerTicketScheduleNumber.FIRST),
			List.of(MakerTicketBookingStatus.CHECKING_PAYMENT)
		)).thenReturn(List.of(ticket));

		var response = ticketService.findAllTicketsByConditions(
			1L,
			100L,
			List.of(ScheduleNumberType.FIRST),
			List.of(BookingStatusType.CHECKING_PAYMENT)
		);

		assertEquals(BookingStatusType.CHECKING_PAYMENT, response.bookingList().get(0).bookingStatus());
		assertEquals("FIRST", response.bookingList().get(0).scheduleNumber());
		verify(makerTicketReadPort).findTickets(
			100L,
			List.of(MakerTicketScheduleNumber.FIRST),
			List.of(MakerTicketBookingStatus.CHECKING_PAYMENT)
		);
	}

	@Test
	void searchAllTicketsByConditionsRejectsNullSearchWord() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketService.searchAllTicketsByConditions(1L, 100L, null, null, null)
		);

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getBaseErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void searchAllTicketsByConditionsRejectsBlankSearchWord() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketService.searchAllTicketsByConditions(
				1L,
				100L,
				"",
				List.of(ScheduleNumberType.FIRST),
				List.of(BookingStatusType.CHECKING_PAYMENT)
			)
		);

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getBaseErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void searchAllTicketsByConditionsRejectsSingleCharacterSearchWord() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketService.searchAllTicketsByConditions(1L, 100L, "a", null, null)
		);

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getBaseErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void searchAllTicketsByConditionsRejectsDeletedBookingStatus() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketService.searchAllTicketsByConditions(
				1L,
				100L,
				"ab",
				null,
				List.of(BookingStatusType.BOOKING_DELETED)
			)
		);

		assertEquals(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED, exception.getBaseErrorCode());
		verifyNoDependencyInteractions();
	}

	@Test
	void findAllTicketsByConditionsRejectsDeletedBookingStatus() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketService.findAllTicketsByConditions(
				1L,
				100L,
				null,
				List.of(BookingStatusType.BOOKING_DELETED)
			)
		);

		assertEquals(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED, exception.getBaseErrorCode());
		verifyNoDependencyInteractions();
	}

	private void verifyNoDependencyInteractions() {
		verifyNoInteractions(
			bookingRepository,
			makerTicketReadPort,
			performanceRepository,
			memberRepository,
			userRepository,
			scheduleRepository,
			smsPort
		);
	}
}
