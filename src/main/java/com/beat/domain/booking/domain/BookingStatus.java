package com.beat.domain.booking.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingStatus {
	CHECKING_PAYMENT("입금확인중"),
	BOOKING_CONFIRMED("예매 확정"),
	BOOKING_CANCELLED("예매 취소"),
	REFUND_REQUESTED("환불 요청"),
	BOOKING_DELETED("예매 삭제");

	private final String displayname;
}
