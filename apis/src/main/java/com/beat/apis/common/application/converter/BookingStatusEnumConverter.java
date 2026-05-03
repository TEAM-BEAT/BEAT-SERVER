package com.beat.apis.common.application.converter;

import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus;

public final class BookingStatusEnumConverter {

	private BookingStatusEnumConverter() {
	}

	public static BookingStatus toDomainForTicketUpdate(final BookingStatusType bookingStatusType) {
		if (bookingStatusType == null) {
			return null;
		}

		return switch (bookingStatusType) {
			case CHECKING_PAYMENT -> BookingStatus.CHECKING_PAYMENT;
			case BOOKING_CONFIRMED -> BookingStatus.BOOKING_CONFIRMED;
			case BOOKING_CANCELLED -> BookingStatus.BOOKING_CANCELLED;
			case REFUND_REQUESTED -> BookingStatus.REFUND_REQUESTED;
			case BOOKING_DELETED -> BookingStatus.BOOKING_DELETED;
		};
	}

	public static MakerTicketBookingStatus toMakerTicketStatus(final BookingStatusType bookingStatusType) {
		if (bookingStatusType == null) {
			return null;
		}

		return switch (bookingStatusType) {
			case CHECKING_PAYMENT -> MakerTicketBookingStatus.CHECKING_PAYMENT;
			case BOOKING_CONFIRMED -> MakerTicketBookingStatus.BOOKING_CONFIRMED;
			case BOOKING_CANCELLED -> MakerTicketBookingStatus.BOOKING_CANCELLED;
			case REFUND_REQUESTED -> MakerTicketBookingStatus.REFUND_REQUESTED;
			case BOOKING_DELETED -> MakerTicketBookingStatus.BOOKING_DELETED;
		};
	}

	public static BookingStatusType toApi(final MakerTicketBookingStatus bookingStatus) {
		if (bookingStatus == null) {
			return null;
		}

		return switch (bookingStatus) {
			case CHECKING_PAYMENT -> BookingStatusType.CHECKING_PAYMENT;
			case BOOKING_CONFIRMED -> BookingStatusType.BOOKING_CONFIRMED;
			case BOOKING_CANCELLED -> BookingStatusType.BOOKING_CANCELLED;
			case REFUND_REQUESTED -> BookingStatusType.REFUND_REQUESTED;
			case BOOKING_DELETED -> BookingStatusType.BOOKING_DELETED;
		};
	}

	public static BookingStatusType toApi(final BookingStatus bookingStatus) {
		if (bookingStatus == null) {
			return null;
		}

		return switch (bookingStatus) {
			case CHECKING_PAYMENT -> BookingStatusType.CHECKING_PAYMENT;
			case BOOKING_CONFIRMED -> BookingStatusType.BOOKING_CONFIRMED;
			case BOOKING_CANCELLED -> BookingStatusType.BOOKING_CANCELLED;
			case REFUND_REQUESTED -> BookingStatusType.REFUND_REQUESTED;
			case BOOKING_DELETED -> BookingStatusType.BOOKING_DELETED;
		};
	}
}
