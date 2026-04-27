package com.beat.domain.staff.repository;

import java.util.List;
import java.util.Optional;

import com.beat.domain.staff.domain.Staff;

public interface StaffRepository {

	Optional<Staff> findById(Long staffId);

	List<Staff> findByPerformanceId(Long performanceId);

	List<Staff> findAllByPerformanceId(Long performanceId);

	List<Long> findIdsByPerformanceId(Long performanceId);

	Staff save(Staff staff);

	List<Staff> saveAll(List<Staff> staffs);

	void delete(Staff staff);

	void deleteByPerformanceId(Long performanceId);
}
