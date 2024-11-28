package com.beat.domain.booking.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.domain.booking.application.TicketService;
import com.beat.domain.booking.application.dto.TicketDeleteRequest;
import com.beat.domain.booking.application.dto.TicketRefundRequest;
import com.beat.domain.booking.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.exception.BookingSuccessCode;
import com.beat.domain.booking.exception.TicketErrorCode;
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
		if (bookingStatus == BookingStatus.BOOKING_DELETED) {
			throw new IllegalArgumentException(TicketErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED.getMessage());
		}
		TicketRetrieveResponse response = ticketService.getTickets(memberId, performanceId, scheduleNumber,
			bookingStatus);
		return ResponseEntity.ok(SuccessResponse.of(BookingSuccessCode.TICKET_RETRIEVE_SUCCESS, response));
	}

	@Override
	@GetMapping("/search/{performanceId}")
	public ResponseEntity<SuccessResponse<TicketRetrieveResponse>> searchTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam String searchWord,
		@RequestParam(required = false) ScheduleNumber scheduleNumber,
		@RequestParam(required = false) BookingStatus bookingStatus) {
		if (searchWord.length() < 2) {
			throw new IllegalArgumentException(TicketErrorCode.SEARCH_WORD_TOO_SHORT.getMessage());
		}
		if (bookingStatus == BookingStatus.BOOKING_DELETED) {
			throw new IllegalArgumentException(TicketErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED.getMessage());
		}

		TicketRetrieveResponse response = ticketService.searchTickets(memberId, performanceId, searchWord,
			scheduleNumber, bookingStatus);
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noCache())
			.body(SuccessResponse.of(BookingSuccessCode.TICKET_SEARCH_SUCCESS, response));
	}

	@Override
	@PutMapping("/update")
	public ResponseEntity<SuccessResponse<Void>> updateTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketUpdateRequest request) {
		ticketService.updateTickets(memberId, request);
		return ResponseEntity.ok(SuccessResponse.from(BookingSuccessCode.TICKET_UPDATE_SUCCESS));
	}

	@Override
	@PutMapping("/refund")
	public ResponseEntity<SuccessResponse<Void>> refundTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketRefundRequest ticketRefundRequest) {
		ticketService.refundTickets(memberId, ticketRefundRequest);
		return ResponseEntity.ok(SuccessResponse.from(BookingSuccessCode.TICKET_REFUND_SUCCESS));
	}

	@Override
	@PutMapping("/delete")
	public ResponseEntity<SuccessResponse<Void>> deleteTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketDeleteRequest ticketDeleteRequest) {
		ticketService.deleteTickets(memberId, ticketDeleteRequest);
		return ResponseEntity.ok(SuccessResponse.from(BookingSuccessCode.TICKET_DELETE_SUCCESS));
	}

}
