package com.beat.infrastructure.persistence.performance.repository

import com.beat.domain.performance.model.Cast
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.model.PerformanceImage
import com.beat.domain.performance.model.Staff
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.infrastructure.persistence.cast.mapper.CastPersistenceMapper
import com.beat.infrastructure.persistence.cast.repository.CastJpaRepository
import com.beat.infrastructure.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infrastructure.persistence.performance.mapper.PerformancePersistenceMapper
import com.beat.infrastructure.persistence.performanceimage.mapper.PerformanceImagePersistenceMapper
import com.beat.infrastructure.persistence.performanceimage.repository.PerformanceImageJpaRepository
import com.beat.infrastructure.persistence.staff.mapper.StaffPersistenceMapper
import com.beat.infrastructure.persistence.staff.repository.StaffJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.repository.findByIdOrNull

@Repository
internal class PerformanceRepositoryImpl(
    private val performanceJpaRepository: PerformanceJpaRepository,
    private val performancePersistenceMapper: PerformancePersistenceMapper,
    private val castJpaRepository: CastJpaRepository,
    private val castPersistenceMapper: CastPersistenceMapper,
    private val staffJpaRepository: StaffJpaRepository,
    private val staffPersistenceMapper: StaffPersistenceMapper,
    private val performanceImageJpaRepository: PerformanceImageJpaRepository,
    private val performanceImagePersistenceMapper: PerformanceImagePersistenceMapper,
) : PerformanceRepository {
    override fun findById(id: Long): Performance? =
        performanceJpaRepository.findByIdOrNull(id)?.let(::toAggregate)

    override fun lockById(id: Long): Performance? =
        performanceJpaRepository.lockById(id)?.let(::toAggregate)

    override fun save(performance: Performance): Performance {
        val entity = performancePersistenceMapper.toEntity(performance)
        val savedEntity = performanceJpaRepository.save(entity)
        val performanceId = checkNotNull(savedEntity.id) { "Saved Performance must have an id" }
        val casts = synchronizeCasts(performanceId, performance.casts)
        val staffs = synchronizeStaffs(performanceId, performance.staffs)
        val images = synchronizeImages(performanceId, performance.images)
        return performancePersistenceMapper.toDomain(savedEntity, casts, staffs, images)
    }

    override fun deleteById(id: Long) {
        castJpaRepository.deleteByPerformanceId(id)
        staffJpaRepository.deleteByPerformanceId(id)
        performanceImageJpaRepository.deleteByPerformanceId(id)
        performanceJpaRepository.deleteById(id)
    }

    private fun toAggregate(entity: PerformanceJpaEntity): Performance {
        val performanceId = checkNotNull(entity.id) { "Loaded Performance must have an id" }
        val casts = castJpaRepository.findAllByPerformanceId(performanceId).map(castPersistenceMapper::toDomain)
        val staffs = staffJpaRepository.findAllByPerformanceId(performanceId).map(staffPersistenceMapper::toDomain)
        val images = performanceImageJpaRepository.findAllByPerformanceId(performanceId)
            .map(performanceImagePersistenceMapper::toDomain)
        return performancePersistenceMapper.toDomain(entity, casts, staffs, images)
    }

    private fun synchronizeCasts(performanceId: Long, casts: List<Cast>): List<Cast> {
        val requestedIds = casts.mapNotNull(Cast::id).toSet()
        val existingIds = castJpaRepository.findIdsByPerformanceId(performanceId)
        validateOwnedIds("Cast", requestedIds, existingIds)
        castJpaRepository.deleteAllByIdInBatch(existingIds.filterNot(requestedIds::contains))
        return castJpaRepository.saveAll(casts.map { castPersistenceMapper.toEntity(it, performanceId) })
            .map(castPersistenceMapper::toDomain)
    }

    private fun synchronizeStaffs(performanceId: Long, staffs: List<Staff>): List<Staff> {
        val requestedIds = staffs.mapNotNull(Staff::id).toSet()
        val existingIds = staffJpaRepository.findIdsByPerformanceId(performanceId)
        validateOwnedIds("Staff", requestedIds, existingIds)
        staffJpaRepository.deleteAllByIdInBatch(existingIds.filterNot(requestedIds::contains))
        return staffJpaRepository.saveAll(staffs.map { staffPersistenceMapper.toEntity(it, performanceId) })
            .map(staffPersistenceMapper::toDomain)
    }

    private fun synchronizeImages(performanceId: Long, images: List<PerformanceImage>): List<PerformanceImage> {
        val requestedIds = images.mapNotNull(PerformanceImage::id).toSet()
        val existingIds = performanceImageJpaRepository.findIdsByPerformanceId(performanceId)
        validateOwnedIds("PerformanceImage", requestedIds, existingIds)
        performanceImageJpaRepository.deleteAllByIdInBatch(existingIds.filterNot(requestedIds::contains))
        return performanceImageJpaRepository.saveAll(
            images.map { performanceImagePersistenceMapper.toEntity(it, performanceId) },
        ).map(performanceImagePersistenceMapper::toDomain)
    }

    private fun validateOwnedIds(childType: String, requestedIds: Set<Long>, existingIds: List<Long>) {
        if (!existingIds.containsAll(requestedIds)) {
            throw IllegalStateException("$childType does not belong to the Performance aggregate")
        }
    }

}
