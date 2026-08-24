package com.beat.application.admin.promotion.query

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.fixture.adminMemberFixture
import com.beat.application.admin.promotion.PromotionImageStorage
import com.beat.application.admin.promotion.PromotionImageUpload
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.domain.exception.DomainException
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.exception.PromotionErrorCode
import com.beat.domain.promotion.repository.PromotionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

class AdminPromotionQueryApplicationSpec : FunSpec({

    context("관리자 회원이 존재하면") {
        test("Promotion을 carousel 의미 순서로 조회 결과에 매핑한다") {
            val service = AdminPromotionQueryService(
                promotionImageStorage = RecordingQueryImageStorage(),
                memberRepository = QueryMemberRepository(member()),
                promotionRepository = QueryPromotionRepository(
                    listOf(
                        promotion(2L, "image-two", null, "url-two", true, CarouselNumber.TWO),
                        promotion(1L, "image-one", 11L, "url-one", false, CarouselNumber.ONE),
                    ),
                ),
            )

            val result = service.findAllPromotionsSortedByCarouselNumber(MEMBER_ID)

            result.promotionResults.map { it.promotionId } shouldContainExactly listOf(1L, 2L)
            result.promotionResults.map { it.carouselNumber } shouldContainExactly listOf("ONE", "TWO")
            result.promotionResults.first().performanceId shouldBe 11L
        }

        test("carousel과 banner upload 요청을 Promotion storage vocabulary로 위임한다") {
            val storage = RecordingQueryImageStorage()
            val service = AdminPromotionQueryService(
                promotionImageStorage = storage,
                memberRepository = QueryMemberRepository(member()),
                promotionRepository = QueryPromotionRepository(emptyList()),
            )

            val carousel = service.issueAllPresignedUrlsForCarousel(MEMBER_ID, listOf("carousel.png"))
            val banner = service.issuePresignedUrlForBanner(MEMBER_ID, "banner.png")

            carousel.carouselPresignedUploads.shouldContainExactly(
                mapOf("carousel.png" to PromotionImageUpload("carousel-upload-url", "dev/carousel/carousel.png")),
            )
            banner.bannerPresignedUrl shouldBe "banner-upload-url"
            banner.bannerImageKey shouldBe "dev/banner/banner.png"
            storage.carouselRequests shouldContainExactly listOf(listOf("carousel.png"))
            storage.bannerRequests shouldContainExactly listOf("banner.png")
        }
    }

    context("관리자 회원이 존재하지 않으면") {
        test("storage와 Promotion 조회 전에 회원 없음 failure를 반환한다") {
            val storage = RecordingQueryImageStorage()
            val promotions = QueryPromotionRepository(emptyList())
            val service = AdminPromotionQueryService(
                promotionImageStorage = storage,
                memberRepository = QueryMemberRepository(null),
                promotionRepository = promotions,
            )

            val exception = shouldThrow<AdminApplicationException> {
                service.findAllPromotionsSortedByCarouselNumber(MEMBER_ID)
            }

            exception.errorCode shouldBe PromotionApplicationErrorCode.MEMBER_NOT_FOUND
            promotions.findAllCalls shouldBe 0
            storage.carouselRequests shouldBe emptyList()
        }
    }

    test("promotion 조회 중 발생한 domain failure를 변환한다") {
        val domainFailure = DomainException(PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS)
        val service = AdminPromotionQueryService(
            promotionImageStorage = RecordingQueryImageStorage(),
            memberRepository = QueryMemberRepository(member()),
            promotionRepository = QueryPromotionRepository(emptyList(), domainFailure),
        )

        val exception = shouldThrow<AdminApplicationException> {
            service.findAllPromotionsSortedByCarouselNumber(MEMBER_ID)
        }

        exception.cause shouldBe domainFailure
        exception.errorCode.code shouldBe PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS.code
        exception.errorCode.message shouldBe PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS.message
    }
})

private class QueryMemberRepository(
    private val member: Member?,
) : MemberRepository {
    override fun findById(id: Long): Member? = member?.takeIf { it.id == id }
    override fun save(member: Member): Member = member
    override fun findBySocialIdentity(socialIdentity: SocialIdentity): Member? = null
    override fun count(): Long = if (member == null) 0 else 1
}

private class QueryPromotionRepository(
    private val promotions: List<Promotion>,
    private val domainFailure: DomainException? = null,
) : PromotionRepository {
    var findAllCalls = 0
        private set
    override fun findAll(): List<Promotion> {
        findAllCalls += 1
        domainFailure?.let { throw it }
        return promotions
    }
    override fun lockAll(): List<Promotion> = promotions
    override fun findById(promotionId: Long): Promotion? = null
    override fun save(promotion: Promotion): Promotion = promotion
    override fun saveAll(promotions: List<Promotion>): List<Promotion> = promotions
    override fun deleteByPromotionIds(promotionIds: List<Long>) = Unit
    override fun deleteByPerformanceId(performanceId: Long) = Unit
    override fun findByCarouselNumber(carouselNumber: CarouselNumber): Promotion? = null
}

private class RecordingQueryImageStorage : PromotionImageStorage {
    val carouselRequests = mutableListOf<List<String>>()
    val bannerRequests = mutableListOf<String>()
    override fun issueCarouselUploads(imageNames: List<String>): Map<String, PromotionImageUpload> {
        carouselRequests += imageNames
        return imageNames.associateWith { PromotionImageUpload("carousel-upload-url", "dev/carousel/$it") }
    }
    override fun issueBannerUpload(imageName: String): PromotionImageUpload {
        bannerRequests += imageName
        return PromotionImageUpload("banner-upload-url", "dev/banner/$imageName")
    }
    override fun exists(imageKey: String): Boolean = false
}

private const val MEMBER_ID = 7L

private fun member() = adminMemberFixture(id = MEMBER_ID)

private fun promotion(
    id: Long,
    imageUrl: String,
    performanceId: Long?,
    redirectUrl: String,
    isExternal: Boolean,
    carouselNumber: CarouselNumber,
): Promotion = Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber)
