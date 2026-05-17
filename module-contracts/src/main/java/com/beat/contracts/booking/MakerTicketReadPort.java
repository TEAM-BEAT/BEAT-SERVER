package com.beat.contracts.booking;

import java.util.List;

import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus;
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel;
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber;

public interface MakerTicketReadPort {

	List<MakerTicketListItemReadModel> findTickets(Long performanceId, List<MakerTicketScheduleNumber> scheduleNumbers,
		List<MakerTicketBookingStatus> bookingStatuses);

	List<MakerTicketListItemReadModel> searchTickets(Long performanceId, String searchWord,
		List<MakerTicketScheduleNumber> scheduleNumbers, List<MakerTicketBookingStatus> bookingStatuses);
}
