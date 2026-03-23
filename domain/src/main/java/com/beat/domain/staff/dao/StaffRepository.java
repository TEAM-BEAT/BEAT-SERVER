package com.beat.domain.staff.dao;

import com.beat.domain.staff.domain.Staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {
	List<Staff> findByPerformanceId(Long performanceId);

	List<Staff> findAllByPerformanceId(Long performanceId);

	@Query("SELECT s.id FROM Staff s WHERE s.performance.id = :performanceId")
	List<Long> findIdsByPerformanceId(Long performanceId);
}
