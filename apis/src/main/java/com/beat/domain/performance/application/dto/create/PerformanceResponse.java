package com.beat.domain.performance.application.dto.create;

import java.util.List;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

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
	String roadAddressName,
	String placeDetailAddress,
	String latitude,
	String longitude,
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
		String roadAddressName,
		String placeDetailAddress,
		String latitude,
		String longitude,
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
			roadAddressName,
			placeDetailAddress,
			latitude,
			longitude,
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
