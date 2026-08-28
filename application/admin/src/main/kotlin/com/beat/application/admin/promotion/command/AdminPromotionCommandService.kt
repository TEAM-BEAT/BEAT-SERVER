package com.beat.application.admin.promotion.command

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.exception.translateDomainFailure
import com.beat.application.admin.promotion.AdminPromotionResultAssembler
import com.beat.application.admin.promotion.AdminPromotionResults
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionGenerateCommand
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionModifyCommand
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.promotion.service.PromotionCarouselDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminPromotionCommandService
internal constructor(
    private val memberRepository: MemberRepository,
    private val promotionRepository: PromotionRepository,
    private val performanceRepository: PerformanceRepository,
    private val promotionImageCache: PromotionImageCache,
    private val promotionImageStorage: PromotionImageStorage,
    private val promotionCarouselDomainService: PromotionCarouselDomainService,
) {
    @Transactional
    fun processAllPromotionsSortedByCarouselNumber(
        memberId: Long,
        command: CarouselHandleCommand,
    ): AdminPromotionResults {
        return translateDomainFailure {
            validateMemberExists(memberId)

            val classifiedPromotions = classifyCarouselPromotions(command)
            validateCarouselAssignments(classifiedPromotions)
            validateCarouselImageObjects(classifiedPromotions)
            lockReferencedPerformances(classifiedPromotions)

            val allExistingPromotions = promotionRepository.lockAll()
            val deletePromotionIds =
                extractDeletePromotionIds(
                    allExistingPromotions,
                    classifiedPromotions.requestPromotionIds,
                )
            val changedPromotions =
                processPromotions(
                    classifiedPromotions.modifyRequests,
                    classifiedPromotions.generateRequests,
                    deletePromotionIds,
                )

            AdminPromotionResultAssembler.assemble(changedPromotions)
        }
    }

    private fun validateCarouselImageObjects(promotions: ClassifiedCarouselPromotions) {
        promotions.modifyRequests.forEach { validateCarouselImageObject(it.newImageUrl) }
        promotions.generateRequests.forEach { validateCarouselImageObject(it.newImageUrl) }
    }

    private fun validateCarouselImageObject(imageUrl: String) {
        val imageKey = requireNotNull(ImageKeyExtractor.extract(imageUrl))
        if (!promotionImageStorage.exists(imageKey)) {
            throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD)
        }
    }

    private fun lockReferencedPerformances(promotions: ClassifiedCarouselPromotions) {
        val performanceIds =
            (promotions.modifyRequests.mapNotNull { it.performanceId } +
                    promotions.generateRequests.mapNotNull { it.performanceId })
                .distinct()
                .sorted()
        performanceIds.forEach { performanceId ->
            performanceRepository.lockById(performanceId)
                ?: throw AdminApplicationException(
                    PromotionApplicationErrorCode.PERFORMANCE_NOT_FOUND
                )
        }
    }

    private fun classifyCarouselPromotions(
        command: CarouselHandleCommand
    ): ClassifiedCarouselPromotions {
        val modifyRequests = command.carousels.filterIsInstance<PromotionModifyCommand>()
        val generateRequests = command.carousels.filterIsInstance<PromotionGenerateCommand>()

        // null이거나 sealed 하위 타입이 아닌 항목은 두 필터에서 모두 빠지므로 개수 합으로 감지
        if (modifyRequests.size + generateRequests.size != command.carousels.size) {
            throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
        }

        val requestPromotionIds = modifyRequests.map { it.promotionId }
        if (requestPromotionIds.distinct().size != requestPromotionIds.size) {
            throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
        }

        return ClassifiedCarouselPromotions(
            modifyRequests = modifyRequests,
            generateRequests = generateRequests,
            requestPromotionIds = requestPromotionIds.toSet(),
        )
    }

    private fun validateCarouselAssignments(promotions: ClassifiedCarouselPromotions) {
        val carouselNumbers =
            promotions.modifyRequests.map { toCarouselNumber(it.carouselNumber) } +
                promotions.generateRequests.map { toCarouselNumber(it.carouselNumber) }

        if (!promotionCarouselDomainService.hasValidCarouselAssignments(carouselNumbers)) {
            throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
        }
    }

    private fun extractDeletePromotionIds(
        allExistingPromotions: List<Promotion>,
        requestPromotionIds: Set<Long>,
    ): List<Long> {
        val allExistingPromotionIds = allExistingPromotions.map { requireNotNull(it.id) }.toSet()
        return allExistingPromotionIds.filter { it !in requestPromotionIds }
    }

    private fun processPromotions(
        modifyRequests: List<PromotionModifyCommand>,
        generateRequests: List<PromotionGenerateCommand>,
        deletePromotionIds: List<Long>,
    ): List<Promotion> {
        handlePromotionDeletion(deletePromotionIds)
        val modifiedDomainPromotions = handlePromotionModification(modifyRequests)
        val addedPromotions = handlePromotionGeneration(generateRequests)
        return modifiedDomainPromotions + addedPromotions
    }

    private fun handlePromotionDeletion(deletePromotionIds: List<Long>) {
        if (deletePromotionIds.isNotEmpty()) {
            promotionRepository.deleteByPromotionIds(deletePromotionIds)
        }
    }

    private fun handlePromotionModification(
        modifyRequests: List<PromotionModifyCommand>
    ): List<Promotion> = modifyRequests.map { modifyRequest ->
        val promotion = findPromotionById(modifyRequest.promotionId)
        val imageKey = requireNotNull(ImageKeyExtractor.extract(modifyRequest.newImageUrl))
        val updatedPromotion =
            promotion.updatePromotionDetails(
                carouselNumber = toCarouselNumber(modifyRequest.carouselNumber),
                newImageUrl = imageKey,
                isExternal = modifyRequest.isExternal,
                redirectUrl = modifyRequest.redirectUrl,
                performanceId = modifyRequest.performanceId,
            )
        promotionRepository.save(updatedPromotion).also { promotionImageCache.preWarm(imageKey) }
    }

    private fun handlePromotionGeneration(
        generateRequests: List<PromotionGenerateCommand>
    ): List<Promotion> = generateRequests.map { generateRequest ->
        val imageKey = requireNotNull(ImageKeyExtractor.extract(generateRequest.newImageUrl))
        val newPromotion =
            Promotion.create(
                promotionPhoto = imageKey,
                performanceId = generateRequest.performanceId,
                redirectUrl = generateRequest.redirectUrl,
                isExternal = generateRequest.isExternal,
                carouselNumber = toCarouselNumber(generateRequest.carouselNumber),
            )
        promotionRepository.save(newPromotion).also { promotionImageCache.preWarm(imageKey) }
    }

    private fun toCarouselNumber(carouselNumber: String): CarouselNumber =
        CarouselNumber.valueOf(carouselNumber)

    private fun findPromotionById(promotionId: Long): Promotion =
        promotionRepository.findById(promotionId)
            ?: throw AdminApplicationException(PromotionApplicationErrorCode.PROMOTION_NOT_FOUND)

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            ?: throw AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND)
    }

    private data class ClassifiedCarouselPromotions(
        val modifyRequests: List<PromotionModifyCommand>,
        val generateRequests: List<PromotionGenerateCommand>,
        val requestPromotionIds: Set<Long>,
    )
}
