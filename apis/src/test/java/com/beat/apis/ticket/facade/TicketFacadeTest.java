package com.beat.apis.ticket.facade;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.apis.ticket.application.TicketService;
import com.beat.apis.ticket.application.dto.TicketRetrieveResponse;
import com.beat.apis.booking.application.dto.BookingStatusType;

@ExtendWith(MockitoExtension.class)
class TicketFacadeTest {

	@Mock
	private TicketService ticketService;

	private TicketFacade ticketFacade;

	@BeforeEach
	void setUp() {
		ticketFacade = new TicketFacade(ticketService);
	}

	@Test
	void findTicketsDelegatesToService() {
		Long memberId = 1L;
		Long performanceId = 100L;
		List<ScheduleNumberType> scheduleNumbers = List.of(ScheduleNumberType.FIRST);
		List<BookingStatusType> bookingStatuses = List.of(BookingStatusType.CHECKING_PAYMENT);
		TicketRetrieveResponse expected = TicketRetrieveResponse.of("title", "team", 1, 100, 10, List.of());
		when(ticketService.findAllTicketsByConditions(memberId, performanceId, scheduleNumbers, bookingStatuses))
			.thenReturn(expected);

		ticketFacade.findTickets(memberId, performanceId, scheduleNumbers, bookingStatuses);

		verify(ticketService).findAllTicketsByConditions(memberId, performanceId, scheduleNumbers, bookingStatuses);
	}

	@Test
	void searchTicketsDelegatesToService() {
		Long memberId = 1L;
		Long performanceId = 100L;
		String searchWord = "홍길동";
		List<ScheduleNumberType> scheduleNumbers = List.of(ScheduleNumberType.FIRST);
		List<BookingStatusType> bookingStatuses = List.of(BookingStatusType.CHECKING_PAYMENT);
		TicketRetrieveResponse expected = TicketRetrieveResponse.of("title", "team", 1, 100, 10, List.of());
		when(ticketService.searchAllTicketsByConditions(memberId, performanceId, searchWord, scheduleNumbers,
			bookingStatuses)).thenReturn(expected);

		ticketFacade.searchTickets(memberId, performanceId, searchWord, scheduleNumbers, bookingStatuses);

		verify(ticketService).searchAllTicketsByConditions(memberId, performanceId, searchWord, scheduleNumbers,
			bookingStatuses);
	}
}
