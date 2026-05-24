package com.beat.apis.performance.application.dto.modify;

import java.util.List;

import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.apis.performance.application.dto.GenreType;
import com.beat.apis.performance.application.dto.modify.cast.CastModifyResponse;
import com.beat.apis.performance.application.dto.modify.performanceImage.PerformanceImageModifyResponse;
import com.beat.apis.performance.application.dto.modify.schedule.ScheduleModifyResponse;
import com.beat.apis.performance.application.dto.modify.staff.StaffModifyResponse;
import com.beat.global.support.jackson.CdnImageUrl;

public record PerformanceModifyResponse(
	Long userId,
	Long performanceId,
	String performanceTitle,
	GenreType genre,
	int runningTime,
	String performanceDescription,
	String performanceAttentionNote,
	BankNameType bankName,
	String accountNumber,
	String accountHolder,
	@CdnImageUrl String posterImage,
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
	List<ScheduleModifyResponse> scheduleModifyResponses,
	List<CastModifyResponse> castModifyResponses,
	List<StaffModifyResponse> staffModifyResponses,
	List<PerformanceImageModifyResponse> performanceImageModifyResponses
) {
	public static PerformanceModifyResponse of(
		Long userId,
		Long performanceId,
		String performanceTitle,
		GenreType genre,
		int runningTime,
		String performanceDescription,
		String performanceAttentionNote,
		BankNameType bankName,
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
			longitude,
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
