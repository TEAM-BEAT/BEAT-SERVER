package com.beat.domain.schedule.domain;

import com.beat.domain.performance.domain.Performance;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.global.common.exception.ConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "performance_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Performance performance;

	@Builder
	public Schedule(LocalDateTime performanceDate, int totalTicketCount, int soldTicketCount, boolean isBooking,
		ScheduleNumber scheduleNumber, Performance performance) {
		this.performanceDate = performanceDate;
		this.totalTicketCount = totalTicketCount;
		this.soldTicketCount = soldTicketCount;
		this.isBooking = isBooking;
		this.scheduleNumber = scheduleNumber;
		this.performance = performance;
	}

	public static Schedule create(LocalDateTime performanceDate, int totalTicketCount, int soldTicketCount,
		boolean isBooking, ScheduleNumber scheduleNumber, Performance performance) {
		return Schedule.builder()
			.performanceDate(performanceDate)
			.totalTicketCount(totalTicketCount)
			.soldTicketCount(soldTicketCount)
			.isBooking(isBooking)
			.scheduleNumber(scheduleNumber)
			.performance(performance)
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