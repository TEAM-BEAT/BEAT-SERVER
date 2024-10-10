package com.beat.domain.booking.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.domain.booking.application.TicketService;
import com.beat.domain.booking.application.dto.TicketCancelRequest;
import com.beat.domain.booking.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.exception.BookingSuccessCode;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController implements TicketApi {

	private final TicketService ticketService;

	@Override
	@GetMapping("/{performanceId}")
	public ResponseEntity<SuccessResponse<TicketRetrieveResponse>> getTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam(required = false) ScheduleNumber scheduleNumber,
		@RequestParam(required = false) BookingStatus bookingStatus) {
		TicketRetrieveResponse response = ticketService.getTickets(memberId, performanceId, scheduleNumber,
			bookingStatus);
		return ResponseEntity.ok(SuccessResponse.of(BookingSuccessCode.TICKET_RETRIEVE_SUCCESS, response));
	}

	@Override
	@PutMapping
	public ResponseEntity<SuccessResponse<Void>> updateTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketUpdateRequest request) {
		ticketService.updateTickets(memberId, request);
		return ResponseEntity.ok(SuccessResponse.from(BookingSuccessCode.TICKET_UPDATE_SUCCESS));
	}

	@Override
	@PatchMapping
	public ResponseEntity<SuccessResponse<Void>> cancelTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketCancelRequest ticketCancelRequest) {
		ticketService.cancelTickets(memberId, ticketCancelRequest);
		return ResponseEntity.ok(SuccessResponse.from(BookingSuccessCode.TICKET_CANCEL_SUCCESS));
	}
}
