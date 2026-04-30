package com.beat.domain.performanceimage.repository;

import java.util.List;
import java.util.Optional;

import com.beat.domain.performanceimage.domain.PerformanceImage;

public interface PerformanceImageRepository {

	Optional<PerformanceImage> findById(Long id);

	List<PerformanceImage> findAllByPerformanceId(Long performanceId);

	List<Long> findIdsByPerformanceId(Long performanceId);

	PerformanceImage save(PerformanceImage performanceImage);

	List<PerformanceImage> saveAll(List<PerformanceImage> performanceImages);

	void delete(PerformanceImage performanceImage);

	void deleteByPerformanceId(Long performanceId);
}
