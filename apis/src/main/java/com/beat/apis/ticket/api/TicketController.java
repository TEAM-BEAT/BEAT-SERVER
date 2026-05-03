package com.beat.apis.ticket.api;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.apis.ticket.application.dto.TicketDeleteRequest;
import com.beat.apis.ticket.application.dto.TicketRefundRequest;
import com.beat.apis.ticket.application.dto.TicketRetrieveResponse;
import com.beat.apis.ticket.application.dto.TicketUpdateRequest;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.ticket.api.response.TicketSuccessCode;
import com.beat.apis.ticket.facade.TicketFacade;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.gateway.security.servlet.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController implements TicketApi {

	private final TicketFacade ticketFacade;

	@Override
	@GetMapping("/{performanceId}")
	public ResponseEntity<SuccessResponse<TicketRetrieveResponse>> getTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam(required = false) List<ScheduleNumberType> scheduleNumbers,
		@RequestParam(required = false) List<BookingStatusType> bookingStatuses) {
		TicketRetrieveResponse response = ticketFacade.findTickets(memberId, performanceId,
			scheduleNumbers, bookingStatuses);
		return ResponseEntity.ok(SuccessResponse.of(TicketSuccessCode.TICKET_RETRIEVE_SUCCESS, response));
	}

	@Override
	@GetMapping("/search/{performanceId}")
	public ResponseEntity<SuccessResponse<TicketRetrieveResponse>> searchTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam String searchWord,
		@RequestParam(required = false) List<ScheduleNumberType> scheduleNumbers,
		@RequestParam(required = false) List<BookingStatusType> bookingStatuses) {
		TicketRetrieveResponse response = ticketFacade.searchTickets(memberId, performanceId,
			searchWord, scheduleNumbers, bookingStatuses);
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noCache())
			.body(SuccessResponse.of(TicketSuccessCode.TICKET_SEARCH_SUCCESS, response));
	}

	@Override
	@PutMapping("/update")
	public ResponseEntity<SuccessResponse<Void>> updateTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketUpdateRequest ticketUpdateRequest) {
		ticketFacade.updateTickets(memberId, ticketUpdateRequest);
		return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_UPDATE_SUCCESS));
	}

	@Override
	@PutMapping("/refund")
	public ResponseEntity<SuccessResponse<Void>> refundTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketRefundRequest ticketRefundRequest) {
		ticketFacade.refundTickets(memberId, ticketRefundRequest);
		return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_REFUND_SUCCESS));
	}

	@Override
	@PutMapping("/delete")
	public ResponseEntity<SuccessResponse<Void>> deleteTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketDeleteRequest ticketDeleteRequest) {
		ticketFacade.deleteTickets(memberId, ticketDeleteRequest);
		return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_DELETE_SUCCESS));
	}

}
