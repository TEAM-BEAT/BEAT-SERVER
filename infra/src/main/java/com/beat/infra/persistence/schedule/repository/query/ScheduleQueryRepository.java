package com.beat.infra.persistence.schedule.repository.query;

import com.beat.domain.schedule.repository.dto.MinPerformanceDateDto;
import java.util.List;

public interface ScheduleQueryRepository {
    List<MinPerformanceDateDto> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds);
}
