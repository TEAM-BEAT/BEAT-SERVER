package com.beat.domain.booking.api;

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

import com.beat.domain.booking.application.TicketService;
import com.beat.domain.booking.application.dto.TicketDeleteRequest;
import com.beat.domain.booking.application.dto.TicketRefundRequest;
import com.beat.domain.booking.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.booking.exception.TicketErrorCode;
import com.beat.domain.booking.exception.TicketSuccessCode;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.common.exception.BadRequestException;

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
		@RequestParam(required = false) List<ScheduleNumber> scheduleNumbers,
		@RequestParam(required = false) List<BookingStatus> bookingStatuses) {
		if (bookingStatuses != null && bookingStatuses.contains(BookingStatus.BOOKING_DELETED)) {
			throw new BadRequestException(TicketErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED);
		}
		TicketRetrieveResponse response = ticketService.findAllTicketsByConditions(memberId, performanceId,
			scheduleNumbers, bookingStatuses);
		return ResponseEntity.ok(SuccessResponse.of(TicketSuccessCode.TICKET_RETRIEVE_SUCCESS, response));
	}

	@Override
	@GetMapping("/search/{performanceId}")
	public ResponseEntity<SuccessResponse<TicketRetrieveResponse>> searchTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam String searchWord,
		@RequestParam(required = false) List<ScheduleNumber> scheduleNumbers,
		@RequestParam(required = false) List<BookingStatus> bookingStatuses) {
		if (searchWord.length() < 2) {
			throw new BadRequestException(TicketErrorCode.SEARCH_WORD_TOO_SHORT);
		}
		if (bookingStatuses != null && bookingStatuses.contains(BookingStatus.BOOKING_DELETED)) {
			throw new BadRequestException(TicketErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED);
		}

		TicketRetrieveResponse response = ticketService.searchAllTicketsByConditions(memberId, performanceId,
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
		ticketService.updateTickets(memberId, ticketUpdateRequest);
		return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_UPDATE_SUCCESS));
	}

	@Override
	@PutMapping("/refund")
	public ResponseEntity<SuccessResponse<Void>> refundTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketRefundRequest ticketRefundRequest) {
		ticketService.refundTicketsByBookingIds(memberId, ticketRefundRequest);
		return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_REFUND_SUCCESS));
	}

	@Override
	@PutMapping("/delete")
	public ResponseEntity<SuccessResponse<Void>> deleteTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketDeleteRequest ticketDeleteRequest) {
		ticketService.deleteTicketsByBookingIds(memberId, ticketDeleteRequest);
		return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_DELETE_SUCCESS));
	}

}
