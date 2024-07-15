package com.beat.domain.booking.application.dto;

import java.util.List;

public record TicketDeleteRequest(
        Long performanceId,
        List<Long> bookingList
) {
    public static TicketDeleteRequest of(Long performanceId, List<Long> bookingList) {
        return new TicketDeleteRequest(performanceId, bookingList);
    }
}
