package com.beat.contracts.booking;

import java.util.List;

import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel;

public interface MakerTicketReadPort {

	List<MakerTicketListItemReadModel> findTickets(Long performanceId, List<String> scheduleNumbers,
		List<String> bookingStatuses);

	List<MakerTicketListItemReadModel> searchTickets(Long performanceId, String searchWord, List<String> scheduleNumbers,
		List<String> bookingStatuses);
}
