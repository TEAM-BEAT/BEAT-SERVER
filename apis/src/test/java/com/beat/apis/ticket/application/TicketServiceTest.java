package com.beat.apis.ticket.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.ticket.application.exception.TicketApplicationErrorCode;
import com.beat.contracts.booking.MakerTicketReadPort;
import com.beat.contracts.sms.SmsPort;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.BadRequestException;

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
