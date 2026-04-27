package com.beat.infra.persistence.staff.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.staff.domain.Staff;
import com.beat.domain.staff.repository.StaffRepository;
import com.beat.infra.persistence.staff.entity.StaffJpaEntity;
import com.beat.infra.persistence.staff.mapper.StaffPersistenceMapper;

@Repository
public class StaffRepositoryImpl implements StaffRepository {

	private final StaffJpaRepository staffJpaRepository;
	private final StaffPersistenceMapper staffPersistenceMapper;

	public StaffRepositoryImpl(StaffJpaRepository staffJpaRepository, StaffPersistenceMapper staffPersistenceMapper) {
		this.staffJpaRepository = staffJpaRepository;
		this.staffPersistenceMapper = staffPersistenceMapper;
	}

	@Override
	public Optional<Staff> findById(Long staffId) {
		return staffJpaRepository.findById(staffId)
			.map(staffPersistenceMapper::toDomain);
	}

	@Override
	public List<Staff> findByPerformanceId(Long performanceId) {
		return staffJpaRepository.findByPerformanceId(performanceId).stream()
			.map(staffPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public List<Staff> findAllByPerformanceId(Long performanceId) {
		return staffJpaRepository.findAllByPerformanceId(performanceId).stream()
			.map(staffPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public List<Long> findIdsByPerformanceId(Long performanceId) {
		return staffJpaRepository.findIdsByPerformanceId(performanceId);
	}

	@Override
	public Staff save(Staff staff) {
		StaffJpaEntity savedStaff = staffJpaRepository.save(staffPersistenceMapper.toEntity(staff));
		return staffPersistenceMapper.toDomain(savedStaff);
	}

	@Override
	public List<Staff> saveAll(List<Staff> staffs) {
		List<StaffJpaEntity> staffEntities = staffs.stream()
			.map(staffPersistenceMapper::toEntity)
			.toList();
		return staffJpaRepository.saveAll(staffEntities).stream()
			.map(staffPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public void delete(Staff staff) {
		staffJpaRepository.deleteById(staff.getId());
	}

	@Override
	public void deleteByPerformanceId(Long performanceId) {
		staffJpaRepository.deleteByPerformanceId(performanceId);
	}
}
