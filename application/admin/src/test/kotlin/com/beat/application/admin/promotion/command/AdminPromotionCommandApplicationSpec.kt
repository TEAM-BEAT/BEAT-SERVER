package com.beat.application.admin.promotion.command

import com.beat.application.admin.exception.AdminApplicationException
import com.beat.application.admin.fixture.adminMemberFixture
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionGenerateCommand
import com.beat.application.admin.promotion.command.CarouselHandleCommand.PromotionModifyCommand
import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.promotion.service.PromotionCarouselDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AdminPromotionCommandApplicationSpec :
    FunSpec({
        context("캐러셀 요청이 유효하지 않으면") {
            test("중복 carousel 번호를 저장소 mutation 전에 거부한다") {
                val fixture = commandFixture()
                val command =
                    CarouselHandleCommand(
                        listOf(
                            PromotionGenerateCommand(
                                "ONE",
                                "prod/carousel/one",
                                true,
                                "url-1",
                                null,
                            ),
                            PromotionGenerateCommand(
                                "ONE",
                                "prod/carousel/two",
                                true,
                                "url-2",
                                null,
                            ),
                        )
                    )

                val exception =
                    shouldThrow<AdminApplicationException> {
                        fixture.service.processAllPromotionsSortedByCarouselNumber(
                            MEMBER_ID,
                            command,
                        )
                    }

                exception.errorCode shouldBe PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT
                fixture.promotions.lockAllCalls shouldBe 0
                fixture.promotions.saved shouldBe emptyList()
            }

            test("중복 수정 id를 저장소 mutation 전에 거부한다") {
                val fixture = commandFixture()
                val command =
                    CarouselHandleCommand(
                        listOf(
                            PromotionModifyCommand(
                                1L,
                                "ONE",
                                "prod/carousel/one",
                                true,
                                "url-1",
                                null,
                            ),
                            PromotionModifyCommand(
                                1L,
                                "TWO",
                                "prod/carousel/two",
                                true,
                                "url-2",
                                null,
                            ),
                        )
                    )

                val exception =
                    shouldThrow<AdminApplicationException> {
                        fixture.service.processAllPromotionsSortedByCarouselNumber(
                            MEMBER_ID,
                            command,
                        )
                    }

                exception.errorCode shouldBe PromotionApplicationErrorCode.INVALID_REQUEST_FORMAT
                fixture.promotions.lockAllCalls shouldBe 0
            }
        }

        context("업로드된 캐러셀 이미지가 존재하지 않으면") {
            test("Performance와 Promotion을 lock하기 전에 실패한다") {
                val fixture = commandFixture(existingImages = emptySet())
                val command =
                    CarouselHandleCommand(
                        listOf(
                            PromotionGenerateCommand(
                                "ONE",
                                "prod/carousel/missing",
                                false,
                                "url",
                                PERFORMANCE_ID,
                            )
                        )
                    )

                val exception =
                    shouldThrow<AdminApplicationException> {
                        fixture.service.processAllPromotionsSortedByCarouselNumber(
                            MEMBER_ID,
                            command,
                        )
                    }

                exception.errorCode shouldBe PromotionApplicationErrorCode.INVALID_IMAGE_UPLOAD
                fixture.performances.lockedIds shouldBe emptyList()
                fixture.promotions.lockAllCalls shouldBe 0
            }
        }

        context("연결할 Performance가 존재하지 않으면") {
            test("authoritative Performance lock에서 실패하고 Promotion을 변경하지 않는다") {
                val fixture = commandFixture(performances = emptyMap())
                val command =
                    CarouselHandleCommand(
                        listOf(
                            PromotionGenerateCommand(
                                "ONE",
                                "prod/carousel/new",
                                false,
                                "url",
                                PERFORMANCE_ID,
                            )
                        )
                    )

                val exception =
                    shouldThrow<AdminApplicationException> {
                        fixture.service.processAllPromotionsSortedByCarouselNumber(
                            MEMBER_ID,
                            command,
                        )
                    }

                exception.errorCode shouldBe PromotionApplicationErrorCode.PERFORMANCE_NOT_FOUND
                fixture.performances.lockedIds shouldContainExactly listOf(PERFORMANCE_ID)
                fixture.promotions.lockAllCalls shouldBe 0
            }
        }

        context("유효한 캐러셀 일괄 변경이면") {
            test("Performance를 정렬된 순서로 lock하고 삭제·수정·생성·응답 정렬을 수행한다") {
                val existing =
                    promotion(1L, "old", PERFORMANCE_ID, "old-url", false, CarouselNumber.TWO)
                val omitted = promotion(2L, "delete", null, "delete-url", true, CarouselNumber.FIVE)
                val fixture =
                    commandFixture(
                        promotions = listOf(existing, omitted),
                        performances = mapOf(PERFORMANCE_ID to performance(PERFORMANCE_ID)),
                        existingImages = setOf("prod/carousel/modified", "prod/carousel/created"),
                    )
                val command =
                    CarouselHandleCommand(
                        listOf(
                            PromotionModifyCommand(
                                1L,
                                "THREE",
                                "prod/carousel/modified",
                                true,
                                "modified-url",
                                PERFORMANCE_ID,
                            ),
                            PromotionGenerateCommand(
                                "ONE",
                                "prod/carousel/created",
                                false,
                                "created-url",
                                null,
                            ),
                        )
                    )

                val result =
                    fixture.service.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)

                fixture.performances.lockedIds shouldContainExactly listOf(PERFORMANCE_ID)
                fixture.promotions.deletedIds shouldContainExactly listOf(2L)
                fixture.cache.warmedKeys shouldContainExactly
                    listOf("prod/carousel/modified", "prod/carousel/created")
                result.promotionResults.map { it.carouselNumber } shouldContainExactly
                    listOf("ONE", "THREE")
                result.promotionResults.map { it.promotionId } shouldContainExactly listOf(3L, 1L)
            }

            test("여러 Performance reference를 중복 제거하고 id 오름차순으로 lock한다") {
                val fixture =
                    commandFixture(
                        performances =
                            mapOf(
                                20L to performance(20L),
                                10L to performance(10L),
                            ),
                        existingImages =
                            setOf("prod/carousel/one", "prod/carousel/two", "prod/carousel/three"),
                    )
                val command =
                    CarouselHandleCommand(
                        listOf(
                            PromotionGenerateCommand("ONE", "prod/carousel/one", false, "one", 20L),
                            PromotionGenerateCommand("TWO", "prod/carousel/two", false, "two", 10L),
                            PromotionGenerateCommand(
                                "THREE",
                                "prod/carousel/three",
                                false,
                                "three",
                                20L,
                            ),
                        )
                    )

                fixture.service.processAllPromotionsSortedByCarouselNumber(MEMBER_ID, command)

                fixture.performances.lockedIds shouldContainExactly listOf(10L, 20L)
            }
        }
    })

private data class CommandFixture(
    val service: AdminPromotionCommandService,
    val promotions: RecordingPromotionRepository,
    val performances: RecordingPerformanceRepository,
    val cache: RecordingPromotionImageCache,
)

private fun commandFixture(
    promotions: List<Promotion> = emptyList(),
    performances: Map<Long, Performance> = mapOf(PERFORMANCE_ID to performance(PERFORMANCE_ID)),
    existingImages: Set<String> =
        setOf("prod/carousel/new", "prod/carousel/one", "prod/carousel/two"),
): CommandFixture {
    val promotionRepository = RecordingPromotionRepository(promotions)
    val performanceRepository = RecordingPerformanceRepository(performances)
    val cache = RecordingPromotionImageCache()
    val service =
        AdminPromotionCommandService(
            memberRepository = StubPromotionMemberRepository(member()),
            promotionRepository = promotionRepository,
            performanceRepository = performanceRepository,
            promotionImageCache = cache,
            promotionImageStorage = StubPromotionImageStorage(existingImages),
            promotionCarouselDomainService = PromotionCarouselDomainService(),
        )
    return CommandFixture(service, promotionRepository, performanceRepository, cache)
}

private class StubPromotionMemberRepository(private val member: Member?) : MemberRepository {
    override fun findById(id: Long): Member? = member?.takeIf { it.id == id }

    override fun save(member: Member): Member = member

    override fun findBySocialIdentity(socialIdentity: SocialIdentity): Member? = null

    override fun count(): Long = if (member == null) 0 else 1
}

private class RecordingPromotionRepository(initialPromotions: List<Promotion>) :
    PromotionRepository {
    private val promotions = initialPromotions.toMutableList()
    var lockAllCalls = 0
        private set

    val deletedIds = mutableListOf<Long>()
    val saved = mutableListOf<Promotion>()
    private var nextId = 3L

    override fun findAll(): List<Promotion> = promotions.toList()

    override fun lockAll(): List<Promotion> {
        lockAllCalls += 1
        return promotions.toList()
    }

    override fun findById(promotionId: Long): Promotion? = promotions.firstOrNull {
        it.id == promotionId
    }

    override fun save(promotion: Promotion): Promotion {
        val savedPromotion =
            if (promotion.id == null) {
                promotion(
                    id = nextId++,
                    imageUrl = promotion.promotionPhoto,
                    performanceId = promotion.performanceId,
                    redirectUrl = promotion.redirectUrl,
                    isExternal = promotion.isExternal,
                    carouselNumber = promotion.carouselNumber,
                )
            } else {
                promotion
            }
        promotions.removeIf { it.id == savedPromotion.id }
        promotions += savedPromotion
        saved += savedPromotion
        return savedPromotion
    }

    override fun saveAll(promotions: List<Promotion>): List<Promotion> = promotions.map(::save)

    override fun deleteByPromotionIds(promotionIds: List<Long>) {
        deletedIds += promotionIds
        promotions.removeIf { it.id in promotionIds }
    }

    override fun deleteByPerformanceId(performanceId: Long) {
        promotions.removeIf { it.performanceId == performanceId }
    }

    override fun findByCarouselNumber(carouselNumber: CarouselNumber): Promotion? =
        promotions.firstOrNull {
            it.carouselNumber == carouselNumber
        }
}

private class RecordingPerformanceRepository(private val performances: Map<Long, Performance>) :
    PerformanceRepository {
    val lockedIds = mutableListOf<Long>()

    override fun findById(id: Long): Performance? = performances[id]

    override fun lockById(id: Long): Performance? {
        lockedIds += id
        return performances[id]
    }

    override fun save(performance: Performance): Performance = performance

    override fun deleteById(id: Long) = Unit
}

private class StubPromotionImageStorage(private val existingKeys: Set<String>) :
    PromotionImageStorage {
    override fun issueCarouselUploads(imageNames: List<String>): Map<String, PromotionImageUpload> =
        emptyMap()

    override fun issueBannerUpload(imageName: String): PromotionImageUpload =
        PromotionImageUpload("url", imageName)

    override fun exists(imageKey: String): Boolean = imageKey in existingKeys
}

private class RecordingPromotionImageCache : PromotionImageCache {
    val warmedKeys = mutableListOf<String>()

    override fun preWarm(imageKey: String) {
        warmedKeys += imageKey
    }
}

private const val MEMBER_ID = 7L
private const val PERFORMANCE_ID = 11L

private fun member() = adminMemberFixture(id = MEMBER_ID)

private fun promotion(
    id: Long,
    imageUrl: String,
    performanceId: Long?,
    redirectUrl: String,
    isExternal: Boolean,
    carouselNumber: CarouselNumber,
): Promotion =
    Promotion.rehydrate(id, imageUrl, performanceId, redirectUrl, isExternal, carouselNumber)

private fun performance(id: Long): Performance =
    Performance.rehydrate(
        id,
        "title",
        Genre.PLAY,
        RunningTime.of(120),
        "description",
        "attention",
        null,
        "poster",
        "team",
        "venue",
        "road",
        "detail",
        "37.0",
        "127.0",
        "contact",
        PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)),
        TicketPrice.of(10_000),
        1,
        1L,
    )
