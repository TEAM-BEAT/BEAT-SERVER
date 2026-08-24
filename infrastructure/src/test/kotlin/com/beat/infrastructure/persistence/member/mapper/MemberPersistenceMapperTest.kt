package com.beat.infrastructure.persistence.member.mapper

import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.infrastructure.persistence.exception.PersistenceMappingException
import com.beat.infrastructure.persistence.member.entity.MemberJpaEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class MemberPersistenceMapperTest : FunSpec({
    val mapper = MemberPersistenceMapper()

    test("toDomain은 social identity와 scalar userId를 보존한다") {
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

    test("왕복 시 신규 member를 보존하고 생성 id는 null로 유지된다") {
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

    test("저장된 social identity가 유효하지 않으면 persistence failure로 변환된다") {
        val corrupted = mockk<MemberJpaEntity>(relaxed = true)
        every { corrupted.socialType } returns nullValue()

        shouldThrow<PersistenceMappingException> { mapper.toDomain(corrupted) }
    }
})

@Suppress("UNCHECKED_CAST")
private fun <T> nullValue(): T = null as T
