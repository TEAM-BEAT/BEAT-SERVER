package com.beat.apis.ticket.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import com.beat.apis.ticket.application.TicketService;
import com.beat.apis.ticket.application.dto.TicketDeleteRequest;
import com.beat.apis.ticket.application.dto.TicketRefundRequest;
import com.beat.apis.ticket.application.dto.TicketRetrieveResponse;
import com.beat.apis.ticket.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketFacade {
	private final TicketService ticketService;

	public TicketRetrieveResponse findTickets(Long memberId, Long performanceId, List<ScheduleNumber> scheduleNumbers,
		List<BookingStatus> bookingStatuses) {
		return ticketService.findAllTicketsByConditions(memberId, performanceId, scheduleNumbers, bookingStatuses);
	}

	public TicketRetrieveResponse searchTickets(Long memberId, Long performanceId, String searchWord,
		List<ScheduleNumber> scheduleNumbers, List<BookingStatus> bookingStatuses) {
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
}
