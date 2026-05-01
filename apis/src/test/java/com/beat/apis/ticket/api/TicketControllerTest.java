package com.beat.apis.ticket.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.beat.apis.ticket.api.response.TicketSuccessCode;
import com.beat.apis.ticket.application.dto.TicketRetrieveResponse;
import com.beat.apis.ticket.facade.TicketFacade;
import com.beat.global.common.dto.SuccessResponse;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

	@Mock
	private TicketFacade ticketFacade;

	private TicketController ticketController;

	@BeforeEach
	void setUp() {
		ticketController = new TicketController(ticketFacade);
	}

	@Test
	void searchTicketsDelegatesToFacade() {
		TicketRetrieveResponse expected = TicketRetrieveResponse.of(
			"title",
			"team",
			1,
			2,
			3,
			List.of()
		);
		when(ticketFacade.searchTickets(anyLong(), anyLong(), any(), any(), any()))
			.thenReturn(expected);

		ResponseEntity<SuccessResponse<TicketRetrieveResponse>> response = ticketController.searchTickets(
			1L,
			100L,
			"ab",
			null,
			null
		);

		assertEquals(200, response.getStatusCode().value());
		assertEquals("no-cache", response.getHeaders().getCacheControl());
		assertEquals(TicketSuccessCode.TICKET_SEARCH_SUCCESS.getStatus(), response.getBody().status());
		assertEquals(TicketSuccessCode.TICKET_SEARCH_SUCCESS.getMessage(), response.getBody().message());
		assertSame(expected, response.getBody().data());
		verify(ticketFacade).searchTickets(1L, 100L, "ab", null, null);
	}
}
