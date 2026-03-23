package com.beat.domain.schedule.application.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record MinPerformanceDateResponse(
	Map<Long, LocalDateTime> performanceDateMap
) {
	public static MinPerformanceDateResponse from(Map<Long, LocalDateTime> performanceDateMap) {
		return new MinPerformanceDateResponse(performanceDateMap);
	}
}
