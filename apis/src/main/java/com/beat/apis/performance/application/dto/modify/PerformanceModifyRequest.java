package com.beat.apis.performance.application.dto.modify;

import java.util.List;

import com.beat.apis.performance.application.dto.modify.cast.CastModifyRequest;
import com.beat.apis.performance.application.dto.modify.performanceImage.PerformanceImageModifyRequest;
import com.beat.apis.performance.application.dto.modify.schedule.ScheduleModifyRequest;
import com.beat.apis.performance.application.dto.modify.staff.StaffModifyRequest;
import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.apis.performance.application.dto.GenreType;

import jakarta.validation.constraints.Size;

public record PerformanceModifyRequest(
	Long performanceId,
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
	int totalScheduleCount,
	int ticketPrice,
	List<ScheduleModifyRequest> scheduleModifyRequests,
	List<CastModifyRequest> castModifyRequests,
	List<StaffModifyRequest> staffModifyRequests,
	List<PerformanceImageModifyRequest> performanceImageModifyRequests
) {
}
