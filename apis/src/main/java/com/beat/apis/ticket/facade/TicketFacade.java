package com.beat.apis.ticket.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.apis.ticket.application.TicketService;
import com.beat.apis.ticket.application.dto.TicketDeleteRequest;
import com.beat.apis.ticket.application.dto.TicketRefundRequest;
import com.beat.apis.ticket.application.dto.TicketRetrieveResponse;
import com.beat.apis.ticket.application.dto.TicketUpdateRequest;
import com.beat.apis.ticket.application.exception.TicketApplicationErrorCode;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.global.common.exception.BadRequestException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketFacade {
	private final TicketService ticketService;

	public TicketRetrieveResponse findTickets(Long memberId, Long performanceId, List<ScheduleNumber> scheduleNumbers,
		List<BookingStatus> bookingStatuses) {
		validateDeletedTicketsAreNotRequested(bookingStatuses);
		return ticketService.findAllTicketsByConditions(memberId, performanceId, scheduleNumbers, bookingStatuses);
	}

	public TicketRetrieveResponse searchTickets(Long memberId, Long performanceId, String searchWord,
		List<ScheduleNumber> scheduleNumbers, List<BookingStatus> bookingStatuses) {
		validateSearchWord(searchWord);
		validateDeletedTicketsAreNotRequested(bookingStatuses);
		return ticketService.searchAllTicketsByConditions(memberId, performanceId, searchWord, scheduleNumbers,
			bookingStatuses);
	}

	public void updateTickets(Long memberId, TicketUpdateRequest request) {
		ticketService.updateTickets(memberId, request);
	}

	public void refundTickets(Long memberId, TicketRefundRequest request) {
		ticketService.refundTicketsByBookingIds(memberId, request);
	}

	public void deleteTickets(Long memberId, TicketDeleteRequest request) {
		ticketService.deleteTicketsByBookingIds(memberId, request);
	}

	private void validateSearchWord(String searchWord) {
		if (searchWord == null || searchWord.length() < 2) {
			throw new BadRequestException(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT);
		}
	}

	private void validateDeletedTicketsAreNotRequested(List<BookingStatus> bookingStatuses) {
		if (bookingStatuses != null && bookingStatuses.contains(BookingStatus.BOOKING_DELETED)) {
			throw new BadRequestException(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED);
		}
	}
}
