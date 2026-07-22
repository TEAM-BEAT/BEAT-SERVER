package com.beat.apis.ticket.facade;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.schedule.api.type.ScheduleNumberType;
import com.beat.apis.ticket.application.command.TicketCommandService;
import com.beat.apis.ticket.application.query.TicketListQuery;
import com.beat.apis.ticket.application.query.TicketQueryService;
import com.beat.apis.ticket.application.result.TicketRetrieveResult;
import com.beat.apis.booking.api.type.BookingStatusType;

@ExtendWith(MockitoExtension.class)
class TicketFacadeTest {

	@Mock
	private TicketQueryService ticketQueryService;

	@Mock
	private TicketCommandService ticketCommandService;

	private TicketFacade ticketFacade;

	@BeforeEach
	void setUp() {
		ticketFacade = new TicketFacade(ticketQueryService, ticketCommandService);
	}

	@Test
	void findTicketsDelegatesToService() {
		Long memberId = 1L;
		Long performanceId = 100L;
		List<ScheduleNumberType> scheduleNumbers = List.of(ScheduleNumberType.FIRST);
		List<BookingStatusType> bookingStatuses = List.of(BookingStatusType.CHECKING_PAYMENT);
		TicketListQuery query = new TicketListQuery(null, List.of("FIRST"), List.of("CHECKING_PAYMENT"));
		TicketRetrieveResult expected = new TicketRetrieveResult("title", "team", 1, 100, 10, List.of());
		when(ticketQueryService.findAllTicketsByConditions(memberId, performanceId, query))
			.thenReturn(expected);

		ticketFacade.findTickets(memberId, performanceId, scheduleNumbers, bookingStatuses);

		verify(ticketQueryService).findAllTicketsByConditions(memberId, performanceId, query);
	}

	@Test
	void searchTicketsDelegatesToService() {
		Long memberId = 1L;
		Long performanceId = 100L;
		String searchWord = "홍길동";
		List<ScheduleNumberType> scheduleNumbers = List.of(ScheduleNumberType.FIRST);
		List<BookingStatusType> bookingStatuses = List.of(BookingStatusType.CHECKING_PAYMENT);
		TicketListQuery query = new TicketListQuery(searchWord, List.of("FIRST"), List.of("CHECKING_PAYMENT"));
		TicketRetrieveResult expected = new TicketRetrieveResult("title", "team", 1, 100, 10, List.of());
		when(ticketQueryService.searchAllTicketsByConditions(memberId, performanceId, query)).thenReturn(expected);

		ticketFacade.searchTickets(memberId, performanceId, searchWord, scheduleNumbers, bookingStatuses);

		verify(ticketQueryService).searchAllTicketsByConditions(memberId, performanceId, query);
	}
}
