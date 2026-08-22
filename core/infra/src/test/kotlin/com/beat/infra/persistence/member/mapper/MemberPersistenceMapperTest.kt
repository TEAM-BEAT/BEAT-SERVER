package com.beat.infra.persistence.member.mapper

import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.infra.persistence.exception.PersistenceMappingException
import com.beat.infra.persistence.member.entity.MemberJpaEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito.mock
import java.time.LocalDateTime

class MemberPersistenceMapperTest : FunSpec({
    val mapper = MemberPersistenceMapper()

    test("toDomainPreservesSocialIdentityAndScalarUserId") {
        val deletedAt = LocalDateTime.of(2026, 7, 16, 12, 30)
        val entity = MemberJpaEntity.rehydrate(
            11L,
            "nickname",
            "member@example.com",
            deletedAt,
            22L,
            33L,
            SocialType.KAKAO,
        )

        val member = mapper.toDomain(entity)

        member.id shouldBe 11L
        member.nickname shouldBe "nickname"
        member.email shouldBe "member@example.com"
        member.deletedAt shouldBe deletedAt
        member.userId shouldBe 22L
        member.socialIdentity shouldBe SocialIdentity.of(SocialType.KAKAO, 33L)
    }

    test("roundTripPreservesNewMemberAndGeneratedIdRemainsNull") {
        val socialIdentity = SocialIdentity.of(SocialType.KAKAO, 44L)
        val member = Member.create("new-member", null, 55L, socialIdentity)

        val roundTrip = mapper.toDomain(mapper.toEntity(member))

        roundTrip.id shouldBe null
        roundTrip.nickname shouldBe "new-member"
        roundTrip.email shouldBe null
        roundTrip.deletedAt shouldBe null
        roundTrip.userId shouldBe 55L
        roundTrip.socialIdentity shouldBe socialIdentity
    }

    test("invalidStoredSocialIdentityIsTranslatedToPersistenceFailure") {
        val corrupted = mock(MemberJpaEntity::class.java)

        shouldThrow<PersistenceMappingException> { mapper.toDomain(corrupted) }
    }
})
