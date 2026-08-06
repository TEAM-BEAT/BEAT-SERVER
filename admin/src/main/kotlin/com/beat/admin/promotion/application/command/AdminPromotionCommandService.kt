package com.beat.admin.promotion.application.command

import com.beat.admin.exception.AdminApplicationException
import com.beat.admin.promotion.application.AdminPromotionResultAssembler
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionGenerateCommand
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionModifyCommand
import com.beat.admin.promotion.application.result.AdminPromotionResults
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.contracts.cdn.ImageCachePort
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.storage.FileStoragePort
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.promotion.service.PromotionCarouselDomainService
import com.beat.global.support.utils.ImageKeyExtractor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminPromotionCommandService(
    private val memberRepository: MemberRepository,
    private val promotionRepository: PromotionRepository,
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val imageCachePort: ImageCachePort,
    private val fileStoragePort: FileStoragePort,
    private val promotionCarouselDomainService: PromotionCarouselDomainService,
) {
    @Transactional
    fun processAllPromotionsSortedByCarouselNumber(
        memberId: Long,
        command: CarouselHandleCommand,
    ): AdminPromotionResults {
        validateMemberExists(memberId)

        val classifiedPromotions = classifyCarouselPromotions(command)
        validateCarouselAssignments(classifiedPromotions)
        validateCarouselImageObjects(classifiedPromotions)

        val allExistingPromotions = promotionRepository.findAll()
        val deletePromotionIds = extractDeletePromotionIds(
            allExistingPromotions,
            classifiedPromotions.requestPromotionIds,
        )
        val changedPromotions = processPromotions(
            classifiedPromotions.modifyRequests,
            classifiedPromotions.generateRequests,
            deletePromotionIds,
        )

        return AdminPromotionResultAssembler.assemble(changedPromotions)
    }

    private fun validateCarouselImageObjects(promotions: ClassifiedCarouselPromotions) {
        promotions.modifyRequests.forEach { validateCarouselImageObject(it.newImageUrl) }
        promotions.generateRequests.forEach { validateCarouselImageObject(it.newImageUrl) }
    }

    private fun validateCarouselImageObject(imageUrl: String) {
        val imageKey = requireNotNull(ImageKeyExtractor.extract(imageUrl))
        fileStoragePort.findImageObjectMetadata(imageKey)
            ?: throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD)
    }

    private fun classifyCarouselPromotions(command: CarouselHandleCommand): ClassifiedCarouselPromotions {
        val modifyRequests = mutableListOf<PromotionModifyCommand>()
        val generateRequests = mutableListOf<PromotionGenerateCommand>()
        val requestPromotionIds = mutableSetOf<Long>()

        for (promotionCommand in command.carousels) {
            when (promotionCommand) {
                is PromotionModifyCommand -> {
                    if (!requestPromotionIds.add(promotionCommand.promotionId)) {
                        throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
                    }
                    modifyRequests.add(promotionCommand)
                }
                is PromotionGenerateCommand -> generateRequests.add(promotionCommand)
                else -> throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
            }
        }

        return ClassifiedCarouselPromotions(
            modifyRequests = modifyRequests.toList(),
            generateRequests = generateRequests.toList(),
            requestPromotionIds = requestPromotionIds.toSet(),
        )
    }

    private fun validateCarouselAssignments(promotions: ClassifiedCarouselPromotions) {
        val carouselNumbers = promotions.modifyRequests.map { toCarouselNumber(it.carouselNumber) } +
            promotions.generateRequests.map { toCarouselNumber(it.carouselNumber) }

        if (!promotionCarouselDomainService.hasValidCarouselAssignments(carouselNumbers)) {
            throw AdminApplicationException(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT)
        }
    }

    private fun extractDeletePromotionIds(
        allExistingPromotions: List<Promotion>,
        requestPromotionIds: Set<Long>,
    ): List<Long> {
        val allExistingPromotionIds = allExistingPromotions.map { requireNotNull(it.getId()) }.toSet()
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

    private fun handlePromotionModification(modifyRequests: List<PromotionModifyCommand>): List<Promotion> =
        modifyRequests.map { modifyRequest ->
            val promotion = findPromotionById(modifyRequest.promotionId)
            val performanceId = validatePerformanceId(modifyRequest.performanceId)

            val imageKey = requireNotNull(ImageKeyExtractor.extract(modifyRequest.newImageUrl))
            val updatedPromotion = promotion.updatePromotionDetails(
                carouselNumber = toCarouselNumber(modifyRequest.carouselNumber),
                newImageUrl = imageKey,
                isExternal = modifyRequest.isExternal,
                redirectUrl = modifyRequest.redirectUrl,
                performanceId = performanceId,
            )
            val saved = promotionRepository.save(updatedPromotion)
            imageCachePort.preWarm(imageKey)
            saved
        }

    private fun handlePromotionGeneration(generateRequests: List<PromotionGenerateCommand>): List<Promotion> =
        generateRequests.map { generateRequest ->
            val performanceId = validatePerformanceId(generateRequest.performanceId)

            val imageKey = requireNotNull(ImageKeyExtractor.extract(generateRequest.newImageUrl))
            val newPromotion = Promotion.create(
                promotionPhoto = imageKey,
                performanceId = performanceId,
                redirectUrl = generateRequest.redirectUrl,
                isExternal = generateRequest.isExternal,
                carouselNumber = toCarouselNumber(generateRequest.carouselNumber),
            )
            val saved = promotionRepository.save(newPromotion)
            imageCachePort.preWarm(imageKey)
            saved
        }

    private fun toCarouselNumber(carouselNumber: String): CarouselNumber = CarouselNumber.valueOf(carouselNumber)

    private fun findPromotionById(promotionId: Long): Promotion =
        promotionRepository.findById(promotionId)
            .orElseThrow { AdminApplicationException(PromotionApplicationErrorCode.PROMOTION_NOT_FOUND) }

    private fun validatePerformanceId(performanceId: Long?): Long? {
        if (performanceId == null) return null
        performanceSummaryReadPort.findById(performanceId)
            .orElseThrow { AdminApplicationException(PromotionApplicationErrorCode.PERFORMANCE_NOT_FOUND) }
        return performanceId
    }

    private fun validateMemberExists(memberId: Long) {
        memberRepository.findById(memberId)
            .orElseThrow { AdminApplicationException(PromotionApplicationErrorCode.MEMBER_NOT_FOUND) }
    }

    private data class ClassifiedCarouselPromotions(
        val modifyRequests: List<PromotionModifyCommand>,
        val generateRequests: List<PromotionGenerateCommand>,
        val requestPromotionIds: Set<Long>,
    )
}