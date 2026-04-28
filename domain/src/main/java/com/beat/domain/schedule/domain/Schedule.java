package com.beat.domain.schedule.domain;

import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.global.common.exception.ConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private LocalDateTime performanceDate;

	@Column(nullable = false)
	private int totalTicketCount;

	@Column(nullable = false)
	private int soldTicketCount = 0;

	@Column(nullable = false)
	private boolean isBooking = true;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ScheduleNumber scheduleNumber;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Builder
	private Schedule(LocalDateTime performanceDate, int totalTicketCount, int soldTicketCount, boolean isBooking,
		ScheduleNumber scheduleNumber, Long performanceId) {
		this.performanceDate = performanceDate;
		this.totalTicketCount = totalTicketCount;
		this.soldTicketCount = soldTicketCount;
		this.isBooking = isBooking;
		this.scheduleNumber = scheduleNumber;
		this.performanceId = performanceId;
	}

	public static Schedule create(LocalDateTime performanceDate, int totalTicketCount, int soldTicketCount,
		boolean isBooking, ScheduleNumber scheduleNumber, Long performanceId) {
		return Schedule.builder()
			.performanceDate(performanceDate)
			.totalTicketCount(totalTicketCount)
			.soldTicketCount(soldTicketCount)
			.isBooking(isBooking)
			.scheduleNumber(scheduleNumber)
			.performanceId(performanceId)
			.build();
	}

	public void update(LocalDateTime performanceDate, int totalTicketCount, ScheduleNumber scheduleNumber) {
		this.performanceDate = performanceDate;
		this.totalTicketCount = totalTicketCount;
		this.scheduleNumber = scheduleNumber;
	}

	public void decreaseSoldTicketCount(int count) {
		if (this.soldTicketCount >= count) {
			this.soldTicketCount -= count;
		} else {
			throw new ConflictException(ScheduleErrorCode.EXCESS_TICKET_DELETE);
		}
	}

	public void updateScheduleNumber(ScheduleNumber scheduleNumber) {
		this.scheduleNumber = scheduleNumber;
	}

	public void updateIsBooking(boolean isBooking) {
		this.isBooking = isBooking;
	}
}
