package com.beat.application.admin.promotion.command

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.fixture.adminMemberFixture
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class PromotionImageCommandApplicationSpec :
    FunSpec({
        test("관리자 회원이 존재하면 carousel과 banner upload 요청을 storage로 위임한다") {
            val storage = RecordingImageStorage()
            val service =
                PromotionImageCommandService(storage, ImageCommandMemberRepository(member()))

            val carousel =
                service.issueAllPresignedUrlsForCarousel(MEMBER_ID, listOf("carousel.png"))
            val banner = service.issuePresignedUrlForBanner(MEMBER_ID, "banner.png")

            carousel.carouselPresignedUploads shouldBe
                mapOf(
                    "carousel.png" to
                        PromotionImageUpload("carousel-upload-url", "dev/carousel/carousel.png")
                )
            banner.bannerPresignedUrl shouldBe "banner-upload-url"
            banner.bannerImageKey shouldBe "dev/banner/banner.png"
            storage.carouselRequests shouldContainExactly listOf(listOf("carousel.png"))
            storage.bannerRequests shouldContainExactly listOf("banner.png")
        }

        test("관리자 회원이 존재하지 않으면 storage 호출 전에 실패한다") {
            val storage = RecordingImageStorage()
            val service = PromotionImageCommandService(storage, ImageCommandMemberRepository(null))

            val exception =
                shouldThrow<AdminApplicationException> {
                    service.issueAllPresignedUrlsForCarousel(MEMBER_ID, listOf("carousel.png"))
                }

            exception.errorCode shouldBe PromotionApplicationErrorCode.MEMBER_NOT_FOUND
            storage.carouselRequests shouldBe emptyList()
            storage.bannerRequests shouldBe emptyList()
        }
    })

private class ImageCommandMemberRepository(private val member: Member?) : MemberRepository {
    override fun findById(id: Long): Member? = member?.takeIf { it.id == id }

    override fun save(member: Member): Member = member

    override fun findBySocialIdentity(socialIdentity: SocialIdentity): Member? = null

    override fun count(): Long = if (member == null) 0 else 1
}

private class RecordingImageStorage : PromotionImageStorage {
    val carouselRequests = mutableListOf<List<String>>()
    val bannerRequests = mutableListOf<String>()

    override fun issueCarouselUploads(imageNames: List<String>): Map<String, PromotionImageUpload> {
        carouselRequests += imageNames
        return imageNames.associateWith {
            PromotionImageUpload("carousel-upload-url", "dev/carousel/$it")
        }
    }

    override fun issueBannerUpload(imageName: String): PromotionImageUpload {
        bannerRequests += imageName
        return PromotionImageUpload("banner-upload-url", "dev/banner/$imageName")
    }

    override fun exists(imageKey: String): Boolean = false
}

private const val MEMBER_ID = 7L

private fun member(): Member = adminMemberFixture(id = MEMBER_ID)
