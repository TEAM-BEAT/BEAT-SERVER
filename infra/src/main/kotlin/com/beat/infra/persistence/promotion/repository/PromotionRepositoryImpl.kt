package com.beat.infra.persistence.promotion.repository

import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.infra.persistence.promotion.mapper.PromotionPersistenceMapper
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class PromotionRepositoryImpl(
    private val promotionJpaRepository: PromotionJpaRepository,
    private val promotionPersistenceMapper: PromotionPersistenceMapper,
) : PromotionRepository {

    override fun findAll(): List<Promotion> =
        promotionJpaRepository.findAll().map(promotionPersistenceMapper::toDomain)

    override fun findById(promotionId: Long?): Optional<Promotion> =
        promotionJpaRepository.findById(requireRepositoryId(promotionId)).map(promotionPersistenceMapper::toDomain)

    override fun save(promotion: Promotion): Promotion =
        promotionJpaRepository.save(promotionPersistenceMapper.toEntity(promotion))
            .let(promotionPersistenceMapper::toDomain)

    override fun saveAll(promotions: List<Promotion>): List<Promotion> =
        promotionJpaRepository.saveAll(promotions.map(promotionPersistenceMapper::toEntity))
            .map(promotionPersistenceMapper::toDomain)

    override fun deleteByPromotionIds(promotionIds: List<Long>) {
        promotionJpaRepository.deleteByPromotionIds(promotionIds)
    }

    override fun deleteByPerformanceId(performanceId: Long?) {
        promotionJpaRepository.deleteByPerformanceId(performanceId)
    }

    override fun findByCarouselNumber(carouselNumber: CarouselNumber): Optional<Promotion> =
        promotionJpaRepository.findByCarouselNumber(carouselNumber).map(promotionPersistenceMapper::toDomain)

    private fun requireRepositoryId(id: Long?): Long =
        requireNotNull(id) { "The given id must not be null" }
}
