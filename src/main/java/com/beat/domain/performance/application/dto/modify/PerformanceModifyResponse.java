package com.beat.domain.performance.application.dto.modify;

import java.util.List;

import com.beat.domain.performance.application.dto.modify.cast.CastModifyResponse;
import com.beat.domain.performance.application.dto.modify.performanceImage.PerformanceImageModifyResponse;
import com.beat.domain.performance.application.dto.modify.schedule.ScheduleModifyResponse;
import com.beat.domain.performance.application.dto.modify.staff.StaffModifyResponse;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

public record PerformanceModifyResponse(
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
	String longtitude,
	String performanceContact,
	String performancePeriod,
	int ticketPrice,
	int totalScheduleCount,
	List<ScheduleModifyResponse> scheduleModifyResponses,
	List<CastModifyResponse> castModifyResponses,
	List<StaffModifyResponse> staffModifyResponses,
	List<PerformanceImageModifyResponse> performanceImageModifyResponses
) {
	public static PerformanceModifyResponse of(
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
		String longtitude,
		String performanceContact,
		String performancePeriod,
		int ticketPrice,
		int totalScheduleCount,
		List<ScheduleModifyResponse> scheduleModifyResponses,
		List<CastModifyResponse> castModifyResponses,
		List<StaffModifyResponse> staffModifyResponses,
		List<PerformanceImageModifyResponse> performanceImageModifyResponses) {

		return new PerformanceModifyResponse(
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
			longtitude,
			performanceContact,
			performancePeriod,
			ticketPrice,
			totalScheduleCount,
			scheduleModifyResponses,
			castModifyResponses,
			staffModifyResponses,
			performanceImageModifyResponses
		);
	}
}