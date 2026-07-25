package com.beat.infra.persistence.performance.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.beat.domain.performance.model.Cast;
import com.beat.domain.performance.model.Performance;
import com.beat.domain.performance.model.PerformanceImage;
import com.beat.domain.performance.model.Staff;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.infra.persistence.cast.mapper.CastPersistenceMapper;
import com.beat.infra.persistence.cast.repository.CastJpaRepository;
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity;
import com.beat.infra.persistence.performance.mapper.PerformancePersistenceMapper;
import com.beat.infra.persistence.performanceimage.mapper.PerformanceImagePersistenceMapper;
import com.beat.infra.persistence.performanceimage.repository.PerformanceImageJpaRepository;
import com.beat.infra.persistence.staff.mapper.StaffPersistenceMapper;
import com.beat.infra.persistence.staff.repository.StaffJpaRepository;

@Repository
public class PerformanceRepositoryImpl implements PerformanceRepository {

	private final PerformanceJpaRepository performanceJpaRepository;
	private final PerformancePersistenceMapper performancePersistenceMapper;
	private final CastJpaRepository castJpaRepository;
	private final CastPersistenceMapper castPersistenceMapper;
	private final StaffJpaRepository staffJpaRepository;
	private final StaffPersistenceMapper staffPersistenceMapper;
	private final PerformanceImageJpaRepository performanceImageJpaRepository;
	private final PerformanceImagePersistenceMapper performanceImagePersistenceMapper;

	public PerformanceRepositoryImpl(PerformanceJpaRepository performanceJpaRepository,
		PerformancePersistenceMapper performancePersistenceMapper,
		CastJpaRepository castJpaRepository,
		CastPersistenceMapper castPersistenceMapper,
		StaffJpaRepository staffJpaRepository,
		StaffPersistenceMapper staffPersistenceMapper,
		PerformanceImageJpaRepository performanceImageJpaRepository,
		PerformanceImagePersistenceMapper performanceImagePersistenceMapper) {
		this.performanceJpaRepository = performanceJpaRepository;
		this.performancePersistenceMapper = performancePersistenceMapper;
		this.castJpaRepository = castJpaRepository;
		this.castPersistenceMapper = castPersistenceMapper;
		this.staffJpaRepository = staffJpaRepository;
		this.staffPersistenceMapper = staffPersistenceMapper;
		this.performanceImageJpaRepository = performanceImageJpaRepository;
		this.performanceImagePersistenceMapper = performanceImagePersistenceMapper;
	}

	@Override
	public Optional<Performance> findById(Long id) {
		return performanceJpaRepository.findById(id).map(this::toAggregate);
	}

	@Override
	public Optional<Performance> lockById(Long id) {
		return performanceJpaRepository.lockById(id).map(this::toAggregate);
	}

	@Override
	public Performance save(Performance performance) {
		PerformanceJpaEntity entity = performancePersistenceMapper.toEntity(performance);
		PerformanceJpaEntity savedEntity = performanceJpaRepository.save(entity);
		long performanceId = Objects.requireNonNull(savedEntity.getId());
		List<Cast> casts = synchronizeCasts(performanceId, performance.getCasts());
		List<Staff> staffs = synchronizeStaffs(performanceId, performance.getStaffs());
		List<PerformanceImage> images = synchronizeImages(performanceId, performance.getImages());
		return performancePersistenceMapper.toDomain(savedEntity, casts, staffs, images);
	}

	@Override
	public void deleteById(Long id) {
		castJpaRepository.deleteByPerformanceId(id);
		staffJpaRepository.deleteByPerformanceId(id);
		performanceImageJpaRepository.deleteByPerformanceId(id);
		performanceJpaRepository.deleteById(id);
	}

	private Performance toAggregate(PerformanceJpaEntity entity) {
		Long performanceId = Objects.requireNonNull(entity.getId());
		List<Cast> casts = castJpaRepository.findAllByPerformanceId(performanceId).stream()
			.map(castPersistenceMapper::toDomain)
			.toList();
		List<Staff> staffs = staffJpaRepository.findAllByPerformanceId(performanceId).stream()
			.map(staffPersistenceMapper::toDomain)
			.toList();
		List<PerformanceImage> images = performanceImageJpaRepository.findAllByPerformanceId(performanceId).stream()
			.map(performanceImagePersistenceMapper::toDomain)
			.toList();
		return performancePersistenceMapper.toDomain(entity, casts, staffs, images);
	}

	private List<Cast> synchronizeCasts(long performanceId, List<Cast> casts) {
		Set<Long> requestedIds = casts.stream().map(Cast::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		List<Long> existingIds = castJpaRepository.findIdsByPerformanceId(performanceId);
		validateOwnedIds("Cast", requestedIds, existingIds);
		castJpaRepository.deleteAllByIdInBatch(existingIds.stream().filter(id -> !requestedIds.contains(id)).toList());
		return castJpaRepository.saveAll(casts.stream().map(cast -> castPersistenceMapper.toEntity(cast, performanceId)).toList())
			.stream().map(castPersistenceMapper::toDomain).toList();
	}

	private List<Staff> synchronizeStaffs(long performanceId, List<Staff> staffs) {
		Set<Long> requestedIds = staffs.stream().map(Staff::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		List<Long> existingIds = staffJpaRepository.findIdsByPerformanceId(performanceId);
		validateOwnedIds("Staff", requestedIds, existingIds);
		staffJpaRepository.deleteAllByIdInBatch(existingIds.stream().filter(id -> !requestedIds.contains(id)).toList());
		return staffJpaRepository.saveAll(staffs.stream().map(staff -> staffPersistenceMapper.toEntity(staff, performanceId)).toList())
			.stream().map(staffPersistenceMapper::toDomain).toList();
	}

	private List<PerformanceImage> synchronizeImages(long performanceId, List<PerformanceImage> images) {
		Set<Long> requestedIds = images.stream().map(PerformanceImage::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		List<Long> existingIds = performanceImageJpaRepository.findIdsByPerformanceId(performanceId);
		validateOwnedIds("PerformanceImage", requestedIds, existingIds);
		performanceImageJpaRepository.deleteAllByIdInBatch(existingIds.stream().filter(id -> !requestedIds.contains(id)).toList());
		return performanceImageJpaRepository.saveAll(images.stream()
			.map(image -> performanceImagePersistenceMapper.toEntity(image, performanceId)).toList())
			.stream().map(performanceImagePersistenceMapper::toDomain).toList();
	}

	private void validateOwnedIds(String childType, Set<Long> requestedIds, List<Long> existingIds) {
		if (!existingIds.containsAll(requestedIds)) {
			throw new IllegalStateException(childType + " does not belong to the Performance aggregate");
		}
	}
}
