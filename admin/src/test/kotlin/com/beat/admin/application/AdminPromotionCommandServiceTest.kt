package com.beat.admin.application

import com.beat.admin.exception.AdminApplicationException
import com.beat.admin.promotion.application.command.AdminPromotionCommandService
import com.beat.admin.promotion.application.command.CarouselHandleCommand
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionGenerateCommand
import com.beat.admin.promotion.application.command.CarouselHandleCommand.PromotionModifyCommand
import com.beat.admin.promotion.application.command.PromotionHandleCommand
import com.beat.admin.promotion.application.result.AdminPromotionResults
import com.beat.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.admin.support.any
import com.beat.admin.support.capture
import com.beat.contracts.cdn.ImageCachePort
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import com.beat.contracts.storage.FileStoragePort
import com.beat.contracts.storage.ImageObjectMetadata
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.promotion.service.PromotionCarouselDomainService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.Collections
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminPromotionCommandServiceTest {

    @Mock
    private lateinit var imageCachePort: ImageCachePort

    @Mock
    private lateinit var fileStoragePort: FileStoragePort

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var promotionRepository: PromotionRepository

    @Mock
    private lateinit var performanceSummaryReadPort: PerformanceSummaryReadPort

    @Spy
    private var promotionCarouselDomainService: PromotionCarouselDomainService = PromotionCarouselDomainService()

    @InjectMocks
    private lateinit var adminPromotionCommandService: AdminPromotionCommandService

    @Captor
    private lateinit var deleteIdsCaptor: ArgumentCaptor<List<Long>>

    @Test
    fun processAllPromotionsRejectsInvalidCarouselItemBeforeMutation() {
        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        val command = CarouselHandleCommand.from(Collections.singletonList<PromotionHandleCommand>(null))

        val exception = assertThrows(AdminApplicationException::class.java) {
            adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)
        }

        assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.errorCode)
        verify(promotionRepository, never()).findAll()
        verify(promotionRepository, never()).deleteByPromotionIds(any())
        verify(promotionRepository, never()).save(any())
        verifyNoInteractions(performanceSummaryReadPort)
    }

    @Test
    fun processAllPromotionsRejectsInvalidCarouselAssignmentsBeforeMutation() {
        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        val command = CarouselHandleCommand.from(
            listOf(
                PromotionGenerateCommand.of("ONE", "image-1", true, "url-1", null),
                PromotionGenerateCommand.of("ONE", "image-2", true, "url-2", null),
            ),
        )

        val exception = assertThrows(AdminApplicationException::class.java) {
            adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)
        }

        assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.errorCode)
        verify(promotionRepository, never()).findAll()
        verify(promotionRepository, never()).deleteByPromotionIds(any())
        verify(promotionRepository, never()).save(any())
        verifyNoInteractions(performanceSummaryReadPort)
    }

    @Test
    fun processAllPromotionsRejectsDuplicateModifyIdsBeforeMutation() {
        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        val command = CarouselHandleCommand.from(
            listOf(
                PromotionModifyCommand.of(1L, "ONE", "image-1", true, "url-1", null),
                PromotionModifyCommand.of(1L, "TWO", "image-2", true, "url-2", null),
            ),
        )

        val exception = assertThrows(AdminApplicationException::class.java) {
            adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)
        }

        assertEquals(PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT, exception.errorCode)
        verify(promotionRepository, never()).findAll()
        verifyNoInteractions(performanceSummaryReadPort)
    }

    @Test
    fun processAllPromotionsPreservesDeletionSaveAndSortedResponseBehavior() {
        val existingPromotion = promotion(1L, "old-image", PERFORMANCE_ID, "old-url", false, CarouselNumber.TWO)
        val omittedPromotion = promotion(2L, "delete-image", null, "delete-url", true, CarouselNumber.FIVE)

        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        `when`(promotionRepository.findAll()).thenReturn(listOf(existingPromotion, omittedPromotion))
        `when`(promotionRepository.findById(1L)).thenReturn(Optional.of(existingPromotion))
        `when`(performanceSummaryReadPort.findById(PERFORMANCE_ID)).thenReturn(Optional.of(performance()))
        `when`(fileStoragePort.findImageObjectMetadata("prod/carousel/modified-image"))
            .thenReturn(validImage())
        `when`(fileStoragePort.findImageObjectMetadata("prod/carousel/created-image"))
            .thenReturn(validImage())
        `when`(promotionRepository.save(any(Promotion::class.java))).thenAnswer { invocation ->
            val savedPromotion = invocation.getArgument<Promotion>(0)
            if (savedPromotion.getId() == null) {
                promotion(
                    3L,
                    savedPromotion.promotionPhoto,
                    savedPromotion.getPerformanceId(),
                    savedPromotion.redirectUrl,
                    savedPromotion.isExternal,
                    savedPromotion.carouselNumber,
                )
            } else {
                savedPromotion
            }
        }

        val command = CarouselHandleCommand.from(
            listOf(
                PromotionModifyCommand.of(1L, "THREE", "prod/carousel/modified-image", true, "modified-url", PERFORMANCE_ID),
                PromotionGenerateCommand.of("ONE", "prod/carousel/created-image", false, "created-url", null),
            ),
        )

        val response: AdminPromotionResults =
            adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)

        verify(promotionRepository).deleteByPromotionIds(capture(deleteIdsCaptor))
        assertEquals(listOf(2L), deleteIdsCaptor.value)
        verify(performanceSummaryReadPort).findById(PERFORMANCE_ID)

        assertEquals(2, response.promotionResults.size)
        assertEquals(3L, response.promotionResults[0].promotionId)
        assertEquals("prod/carousel/created-image", response.promotionResults[0].newImageUrl)
        assertEquals("ONE", response.promotionResults[0].carouselNumber)
        assertEquals(1L, response.promotionResults[1].promotionId)
        assertEquals("prod/carousel/modified-image", response.promotionResults[1].newImageUrl)
        assertEquals("THREE", response.promotionResults[1].carouselNumber)
    }

    @Test
    fun processAllPromotionsRejectsMissingUploadedImageBeforeMutation() {
        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        `when`(fileStoragePort.findImageObjectMetadata("prod/carousel/missing-image")).thenReturn(null)

        val command = CarouselHandleCommand.from(
            listOf(PromotionGenerateCommand.of("ONE", "prod/carousel/missing-image", false, "created-url", null)),
        )

        val exception = assertThrows(AdminApplicationException::class.java) {
            adminPromotionCommandService.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)
        }

        assertEquals(PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD, exception.errorCode)
        verify(promotionRepository, never()).findAll()
        verify(promotionRepository, never()).deleteByPromotionIds(any())
        verify(promotionRepository, never()).save(any())
    }

    companion object {
        private const val MEMBER_ID = 7L
        private const val PERFORMANCE_ID = 11L

        private fun validImage(): ImageObjectMetadata = ImageObjectMetadata.of("image/png", 1024L)

        private fun promotion(
            id: Long,
            imageUrl: String,
            performanceId: Long?,
            redirectUrl: String,
            isExternal: Boolean,
            carouselNumber: CarouselNumber,
        ): Promotion = Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber)

        private fun member(): Member =
            Member.rehydrate(MEMBER_ID, "admin", "admin@example.com", null, 1L, SocialIdentity.of(SocialType.KAKAO, 10L))

        private fun performance(): PerformanceSummaryReadModel = PerformanceSummaryReadModel(
            PERFORMANCE_ID,
            1L,
            "title",
            "PLAY",
            0,
            null,
            null,
            null,
            "poster",
            "team",
            "venue",
            "contact",
            1,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 1),
        )
    }
}