package com.beat.domain.performance.application.dto.create;

import java.util.List;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

public record PerformanceRequest(
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
	List<ScheduleRequest> scheduleList,
	List<CastRequest> castList,
	List<StaffRequest> staffList,
	List<PerformanceImageRequest> performanceImageList
) {
}