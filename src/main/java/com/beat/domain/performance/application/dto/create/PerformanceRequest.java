package com.beat.domain.performance.application.dto.create;

import java.util.List;

import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;

import jakarta.validation.constraints.Size;
public record PerformanceRequest(
	String performanceTitle,
	Genre genre,
	int runningTime,
	@Size(max = 1500, message = "공연 소개는 1500자를 초과할 수 없습니다.")
	String performanceDescription,
	@Size(max = 1500, message = "공연 유의사항은 1500자를 초과할 수 없습니다.")
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
	List<ScheduleRequest> scheduleList,
	List<CastRequest> castList,
	List<StaffRequest> staffList,
	List<PerformanceImageRequest> performanceImageList
) {
}