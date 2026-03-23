package com.beat.domain.schedule.dao;


import java.util.List;

import com.beat.domain.schedule.dao.dto.MinPerformanceDateDto;

public interface ScheduleRepositoryCustom {
	List<MinPerformanceDateDto> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds);
}
