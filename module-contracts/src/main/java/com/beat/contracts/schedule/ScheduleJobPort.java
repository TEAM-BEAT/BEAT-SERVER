package com.beat.contracts.schedule;

import com.beat.domain.schedule.domain.Schedule;

public interface ScheduleJobPort {

	void registerOrRefresh(Schedule schedule);

	void cancel(Schedule schedule);
}
