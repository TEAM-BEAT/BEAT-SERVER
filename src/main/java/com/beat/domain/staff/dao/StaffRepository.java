package com.beat.domain.staff.dao;

import com.beat.domain.staff.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByPerformanceId(Long performanceId);
}
