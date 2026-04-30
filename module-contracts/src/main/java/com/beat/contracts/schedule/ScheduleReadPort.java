package com.beat.contracts.schedule;

import java.util.List;

import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel;

public interface ScheduleReadPort {

	List<MinPerformanceDateReadModel> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds);
}
