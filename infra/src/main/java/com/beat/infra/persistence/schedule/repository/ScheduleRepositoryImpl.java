package com.beat.infra.persistence.schedule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.repository.dto.MinPerformanceDateDto;
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity;
import com.beat.infra.persistence.schedule.mapper.SchedulePersistenceMapper;
import com.beat.infra.persistence.schedule.repository.query.ScheduleQueryRepository;

@Repository
public class ScheduleRepositoryImpl implements ScheduleRepository {

	private final ScheduleJpaRepository scheduleJpaRepository;
	private final SchedulePersistenceMapper schedulePersistenceMapper;
	private final ScheduleQueryRepository scheduleQueryRepository;

	public ScheduleRepositoryImpl(ScheduleJpaRepository scheduleJpaRepository,
		SchedulePersistenceMapper schedulePersistenceMapper,
		ScheduleQueryRepository scheduleQueryRepository) {
		this.scheduleJpaRepository = scheduleJpaRepository;
		this.schedulePersistenceMapper = schedulePersistenceMapper;
		this.scheduleQueryRepository = scheduleQueryRepository;
	}

	@Override
	public Optional<Schedule> findById(Long id) {
		return scheduleJpaRepository.findById(id)
			.map(schedulePersistenceMapper::toDomain);
	}

	@Override
	public Optional<Schedule> lockById(Long id) {
		return scheduleJpaRepository.lockById(id)
			.map(schedulePersistenceMapper::toDomain);
	}

	@Override
	public List<Schedule> findAllByPerformanceId(Long performanceId) {
		return scheduleJpaRepository.findAllByPerformanceId(performanceId).stream()
			.map(schedulePersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public List<Long> findIdsByPerformanceId(Long performanceId) {
		return scheduleJpaRepository.findIdsByPerformanceId(performanceId);
	}

	@Override
	public int countByPerformanceId(Long performanceId) {
		return scheduleJpaRepository.countByPerformanceId(performanceId);
	}

	@Override
	public Schedule save(Schedule schedule) {
		ScheduleJpaEntity savedEntity = scheduleJpaRepository.save(schedulePersistenceMapper.toEntity(schedule));
		return schedulePersistenceMapper.toDomain(savedEntity);
	}

	@Override
	public List<Schedule> saveAll(List<Schedule> schedules) {
		List<ScheduleJpaEntity> entities = schedules.stream()
			.map(schedulePersistenceMapper::toEntity)
			.toList();
		return scheduleJpaRepository.saveAll(entities).stream()
			.map(schedulePersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public void delete(Schedule schedule) {
		if (schedule.getId() == null) {
			throw new IllegalArgumentException("Cannot delete unpersisted Schedule");
		}
		scheduleJpaRepository.deleteById(schedule.getId());
	}

	@Override
	public void deleteByPerformanceId(Long performanceId) {
		scheduleJpaRepository.deleteByPerformanceId(performanceId);
	}

	@Override
	public List<Schedule> findPendingSchedules() {
		return scheduleJpaRepository.findPendingSchedules().stream()
			.map(schedulePersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public List<MinPerformanceDateDto> findMinPerformanceDateByPerformanceIds(List<Long> performanceIds) {
		return scheduleQueryRepository.findMinPerformanceDateByPerformanceIds(performanceIds);
	}
}
