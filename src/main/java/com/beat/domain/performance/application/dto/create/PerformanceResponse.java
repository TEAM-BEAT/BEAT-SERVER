package com.beat.domain.performance.application.dto.create;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

import java.util.List;

public record PerformanceResponse(
	Long userId,
	Long performanceId,
	String performanceTitle,
	Genre genre,
	int runningTime,
	String performanceDescription,
	String performanceAttentionNote,
	BankName bankName,
	String accountNumber,
	String accountHolder,
	String posterImage,
	String performanceTeamName,
	String performanceVenue,
	String performanceContact,
	String performancePeriod,
	int ticketPrice,
	int totalScheduleCount,
	List<ScheduleResponse> scheduleList,
	List<CastResponse> castList,
	List<StaffResponse> staffList,
	List<PerformanceImageResponse> performanceImageList
) {
	public static PerformanceResponse of(
		Long userId,
		Long performanceId,
		String performanceTitle,
		Genre genre,
		int runningTime,
		String performanceDescription,
		String performanceAttentionNote,
		BankName bankName,
		String accountNumber,
		String accountHolder,
		String posterImage,
		String performanceTeamName,
		String performanceVenue,
		String performanceContact,
		String performancePeriod,
		int ticketPrice,
		int totalScheduleCount,
		List<ScheduleResponse> scheduleList,
		List<CastResponse> castList,
		List<StaffResponse> staffList,
		List<PerformanceImageResponse> performanceImageList
	) {
		return new PerformanceResponse(
			userId,
			performanceId,
			performanceTitle,
			genre,
			runningTime,
			performanceDescription,
			performanceAttentionNote,
			bankName,
			accountNumber,
			accountHolder,
			posterImage,
			performanceTeamName,
			performanceVenue,
			performanceContact,
			performancePeriod,
			ticketPrice,
			totalScheduleCount,
			scheduleList,
			castList,
			staffList,
			performanceImageList
		);
	}
}
