package com.beat.apis.performance.application.dto.create;

import java.util.List;

import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.apis.performance.application.dto.GenreType;

import jakarta.validation.constraints.Size;
public record PerformanceRequest(
	String performanceTitle,
	GenreType genre,
	int runningTime,
	@Size(max = 1500, message = "공연 소개는 1500자를 초과할 수 없습니다.")
	String performanceDescription,
	@Size(max = 1500, message = "공연 유의사항은 1500자를 초과할 수 없습니다.")
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
	List<ScheduleRequest> scheduleList,
	List<CastRequest> castList,
	List<StaffRequest> staffList,
	List<PerformanceImageRequest> performanceImageList
) {
}
