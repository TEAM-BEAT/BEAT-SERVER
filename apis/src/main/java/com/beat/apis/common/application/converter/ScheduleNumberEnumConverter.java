package com.beat.apis.common.application.converter;

import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber;

public final class ScheduleNumberEnumConverter {

	private ScheduleNumberEnumConverter() {
	}

	public static ScheduleNumber toDomain(final ScheduleNumberType scheduleNumberType) {
		if (scheduleNumberType == null) {
			return null;
		}

		return switch (scheduleNumberType) {
			case FIRST -> ScheduleNumber.FIRST;
			case SECOND -> ScheduleNumber.SECOND;
			case THIRD -> ScheduleNumber.THIRD;
			case FOURTH -> ScheduleNumber.FOURTH;
			case FIFTH -> ScheduleNumber.FIFTH;
			case SIXTH -> ScheduleNumber.SIXTH;
			case SEVENTH -> ScheduleNumber.SEVENTH;
			case EIGHTH -> ScheduleNumber.EIGHTH;
			case NINTH -> ScheduleNumber.NINTH;
			case TENTH -> ScheduleNumber.TENTH;
		};
	}

	public static MakerTicketScheduleNumber toMakerTicketScheduleNumber(final ScheduleNumberType scheduleNumberType) {
		if (scheduleNumberType == null) {
			return null;
		}

		return switch (scheduleNumberType) {
			case FIRST -> MakerTicketScheduleNumber.FIRST;
			case SECOND -> MakerTicketScheduleNumber.SECOND;
			case THIRD -> MakerTicketScheduleNumber.THIRD;
			case FOURTH -> MakerTicketScheduleNumber.FOURTH;
			case FIFTH -> MakerTicketScheduleNumber.FIFTH;
			case SIXTH -> MakerTicketScheduleNumber.SIXTH;
			case SEVENTH -> MakerTicketScheduleNumber.SEVENTH;
			case EIGHTH -> MakerTicketScheduleNumber.EIGHTH;
			case NINTH -> MakerTicketScheduleNumber.NINTH;
			case TENTH -> MakerTicketScheduleNumber.TENTH;
		};
	}

	public static MakerTicketScheduleNumber toMakerTicketScheduleNumber(final ScheduleNumber scheduleNumber) {
		if (scheduleNumber == null) {
			return null;
		}

		return switch (scheduleNumber) {
			case FIRST -> MakerTicketScheduleNumber.FIRST;
			case SECOND -> MakerTicketScheduleNumber.SECOND;
			case THIRD -> MakerTicketScheduleNumber.THIRD;
			case FOURTH -> MakerTicketScheduleNumber.FOURTH;
			case FIFTH -> MakerTicketScheduleNumber.FIFTH;
			case SIXTH -> MakerTicketScheduleNumber.SIXTH;
			case SEVENTH -> MakerTicketScheduleNumber.SEVENTH;
			case EIGHTH -> MakerTicketScheduleNumber.EIGHTH;
			case NINTH -> MakerTicketScheduleNumber.NINTH;
			case TENTH -> MakerTicketScheduleNumber.TENTH;
		};
	}

	public static String toApiName(final ScheduleNumber scheduleNumber) {
		ScheduleNumberType scheduleNumberType = toApi(scheduleNumber);
		return scheduleNumberType == null ? null : scheduleNumberType.name();
	}

	public static ScheduleNumberType toApi(final ScheduleNumber scheduleNumber) {
		if (scheduleNumber == null) {
			return null;
		}

		return switch (scheduleNumber) {
			case FIRST -> ScheduleNumberType.FIRST;
			case SECOND -> ScheduleNumberType.SECOND;
			case THIRD -> ScheduleNumberType.THIRD;
			case FOURTH -> ScheduleNumberType.FOURTH;
			case FIFTH -> ScheduleNumberType.FIFTH;
			case SIXTH -> ScheduleNumberType.SIXTH;
			case SEVENTH -> ScheduleNumberType.SEVENTH;
			case EIGHTH -> ScheduleNumberType.EIGHTH;
			case NINTH -> ScheduleNumberType.NINTH;
			case TENTH -> ScheduleNumberType.TENTH;
		};
	}
}
