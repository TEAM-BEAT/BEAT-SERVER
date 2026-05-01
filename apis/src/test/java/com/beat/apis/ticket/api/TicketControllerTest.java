package com.beat.apis.ticket.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.beat.apis.ticket.application.TicketService;
import com.beat.apis.ticket.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.apis.ticket.application.exception.TicketApplicationErrorCode;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

	@Mock
	private TicketService ticketService;

	private TicketController ticketController;

	@BeforeEach
	void setUp() {
		ticketController = new TicketController(ticketService);
	}

	@Test
	void searchTicketsRejectsBlankSearchWord() {
		BadRequestException exception = assertThrows(BadRequestException.class, () ->
			ticketController.searchTickets(
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
			ticketController.searchTickets(1L, 100L, "a", null, null)
		);

		assertEquals(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT, exception.getBaseErrorCode());
		verify(ticketService, never()).searchAllTicketsByConditions(anyLong(), anyLong(), any(), any(), any());
	}

	@Test
	void searchTicketsDelegatesForValidSearchWord() {
		when(ticketService.searchAllTicketsByConditions(anyLong(), anyLong(), any(), any(), any()))
			.thenReturn(TicketRetrieveResponse.of(
				"title",
				"team",
				1,
				2,
				3,
				List.of()
			));

		ResponseEntity<SuccessResponse<TicketRetrieveResponse>> response = ticketController.searchTickets(
			1L,
			100L,
			"ab",
			null,
			null
		);

		assertEquals(200, response.getStatusCode().value());
		verify(ticketService).searchAllTicketsByConditions(1L, 100L, "ab", null, null);
	}
}
