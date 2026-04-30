package com.beat.contracts.schedule;

import java.util.List;

import com.beat.contracts.schedule.readmodel.MinPerformanceDate;

public interface ScheduleReadPort {

	List<MinPerformanceDate> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds);
}
