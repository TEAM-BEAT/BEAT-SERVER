package com.beat.domain.performance.port.in;

import com.beat.domain.performance.domain.Performance;

public interface PerformanceUseCase {
	Performance findById(Long performanceId);
}
