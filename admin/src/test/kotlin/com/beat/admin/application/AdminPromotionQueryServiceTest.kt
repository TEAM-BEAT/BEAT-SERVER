package com.beat.admin.application

import com.beat.admin.promotion.application.query.AdminPromotionQueryService
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult
import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult
import com.beat.contracts.storage.BannerPresignedUrl
import com.beat.contracts.storage.CarouselPresignedUpload
import com.beat.contracts.storage.CarouselPresignedUrls
import com.beat.contracts.storage.FileStoragePort
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminPromotionQueryServiceTest {

    @Mock
    private lateinit var fileStoragePort: FileStoragePort

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var promotionRepository: PromotionRepository

    @InjectMocks
    private lateinit var adminPromotionQueryService: AdminPromotionQueryService

    @Test
    fun findAllPromotionsPreservesCarouselSortingAndResponseShape() {
        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        `when`(promotionRepository.findAll()).thenReturn(
            listOf(
                promotion(2L, "image-two", null, "url-two", true, CarouselNumber.TWO),
                promotion(1L, "image-one", 11L, "url-one", false, CarouselNumber.ONE),
            ),
        )

        val response = adminPromotionQueryService.findAllPromotionsSortedByCarouselNumber(MEMBER_ID)

        assertEquals(2, response.promotionResults.size)
        assertEquals(1L, response.promotionResults[0].promotionId)
        assertEquals("ONE", response.promotionResults[0].carouselNumber)
        assertEquals("image-one", response.promotionResults[0].newImageUrl)
        assertEquals(11L, response.promotionResults[0].performanceId)
        assertEquals(2L, response.promotionResults[1].promotionId)
        assertEquals("TWO", response.promotionResults[1].carouselNumber)
        assertEquals("image-two", response.promotionResults[1].newImageUrl)
    }

    @Test
    fun presignedUrlQueriesStillValidateMemberAndDelegateToStoragePort() {
        `when`(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member()))
        `when`(fileStoragePort.issueAllPresignedUrlsForCarousel(listOf("carousel.png")))
            .thenReturn(
                CarouselPresignedUrls(
                    mapOf("carousel.png" to CarouselPresignedUpload.of("carousel-upload-url", "dev/carousel/carousel.png")),
                ),
            )
        `when`(fileStoragePort.issuePresignedUrlForBanner("banner.png"))
            .thenReturn(BannerPresignedUrl("banner-url", "prod/banner/banner.png"))

        val carouselResponse: CarouselPresignedUrlsResult =
            adminPromotionQueryService.issueAllPresignedUrlsForCarousel(MEMBER_ID, listOf("carousel.png"))
        val bannerResponse: BannerPresignedUrlResult =
            adminPromotionQueryService.issuePresignedUrlForBanner(MEMBER_ID, "banner.png")

        assertEquals(
            mapOf("carousel.png" to CarouselPresignedUpload.of("carousel-upload-url", "dev/carousel/carousel.png")),
            carouselResponse.carouselPresignedUploads,
        )
        assertEquals("banner-url", bannerResponse.bannerPresignedUrl)
        assertEquals("prod/banner/banner.png", bannerResponse.bannerImageKey)
        verify(fileStoragePort).issueAllPresignedUrlsForCarousel(listOf("carousel.png"))
        verify(fileStoragePort).issuePresignedUrlForBanner("banner.png")
    }

    companion object {
        private const val MEMBER_ID = 7L

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
    }
}