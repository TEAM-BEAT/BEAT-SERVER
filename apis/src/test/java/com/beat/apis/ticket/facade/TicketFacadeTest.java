package com.beat.apis.ticket.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.ticket.application.TicketService;
import com.beat.apis.ticket.application.exception.TicketApplicationErrorCode;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.global.common.exception.BadRequestException;

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
	void searchTicketsRejectsBlankSearchWord() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketFacade.searchTickets(
				1L,
				100L,
				"",
				List.of(ScheduleNumber.FIRST),
				List.of(BookingStatus.CHECKING_PAYMENT)
			)
		);

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getBaseErrorCode());
		verify(ticketService, never()).searchAllTicketsByConditions(anyLong(), anyLong(), any(), any(), any());
	}

	@Test
	void searchTicketsRejectsSingleCharacterSearchWord() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketFacade.searchTickets(1L, 100L, "a", null, null)
		);

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getBaseErrorCode());
		verify(ticketService, never()).searchAllTicketsByConditions(anyLong(), anyLong(), any(), any(), any());
	}

	@Test
	void searchTicketsRejectsDeletedBookingStatus() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketFacade.searchTickets(1L, 100L, "ab", null, List.of(BookingStatus.BOOKING_DELETED))
		);

		assertEquals(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED, exception.getBaseErrorCode());
		verify(ticketService, never()).searchAllTicketsByConditions(anyLong(), anyLong(), any(), any(), any());
	}
}
