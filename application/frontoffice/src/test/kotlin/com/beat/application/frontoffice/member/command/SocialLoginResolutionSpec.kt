package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.fixture.frontofficeMemberFixture
import com.beat.domain.member.exception.DuplicateSocialIdentityException
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.Called
import io.mockk.verify

class SocialLoginResolutionSpec : FunSpec({

    test("기존 member 로그인은 다른 user를 등록하지 않고 인증 결과를 반환한다") {
        val memberRepository = mockk<MemberRepository>(relaxed = true)
        val memberRegistrar = mockk<MemberRegistrar>(relaxed = true)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val existingMember = member(id = 11L, userId = 22L)
        every { memberRepository.findBySocialIdentity(identity) } returns existingMember

        val result = resolver(memberRepository, memberRegistrar).findOrRegister(PROFILE, identity)

        result shouldBe MemberAuthenticationResult(11L, 22L)
        verify { memberRegistrar wasNot Called }
    }

    test("신규 member 등록은 registrar 결과로 저장된 member를 다시 조회한다") {
        val memberRepository = mockk<MemberRepository>(relaxed = true)
        val memberRegistrar = mockk<MemberRegistrar>(relaxed = true)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val registeredMember = member(id = 11L, userId = 22L)
        every { memberRepository.findBySocialIdentity(identity) } returns null
        every { memberRegistrar.registerMemberWithUserInfo(PROFILE, identity) } returns 11L
        every { memberRepository.findById(11L) } returns registeredMember

        val result = resolver(memberRepository, memberRegistrar).findOrRegister(PROFILE, identity)

        result shouldBe MemberAuthenticationResult(11L, 22L)
        verify { memberRegistrar.registerMemberWithUserInfo(PROFILE, identity) }
        verify { memberRepository.findById(11L) }
    }

    test("동일 identity 등록 경쟁 시 중복 예외를 호출자에게 전달한다") {
        val memberRepository = mockk<MemberRepository>(relaxed = true)
        val memberRegistrar = mockk<MemberRegistrar>(relaxed = true)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val duplicate = DuplicateSocialIdentityException(IllegalStateException("unique constraint"))
        every { memberRepository.findBySocialIdentity(identity) } returns null
        every { memberRegistrar.registerMemberWithUserInfo(PROFILE, identity) } throws duplicate

        val exception = shouldThrow<DuplicateSocialIdentityException> {
            resolver(memberRepository, memberRegistrar).findOrRegister(PROFILE, identity)
        }

        exception shouldBe duplicate
        verify(exactly = 1) { memberRepository.findBySocialIdentity(identity) }
    }

    test("등록 후 member 조회 실패는 member not found로 매핑된다") {
        val memberRepository = mockk<MemberRepository>(relaxed = true)
        val memberRegistrar = mockk<MemberRegistrar>(relaxed = true)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        every { memberRepository.findBySocialIdentity(identity) } returns null
        every { memberRegistrar.registerMemberWithUserInfo(PROFILE, identity) } returns 11L
        every { memberRepository.findById(11L) } returns null

        val exception = shouldThrow<FrontofficeApplicationException> {
            resolver(memberRepository, memberRegistrar).findOrRegister(PROFILE, identity)
        }

        exception.errorCode.code shouldBe "MEMBER_NOT_FOUND"
        exception.errorCode.type shouldBe FrontofficeApplicationErrorType.NOT_FOUND
    }
})

private fun resolver(
    memberRepository: MemberRepository,
    memberRegistrar: MemberRegistrar,
): SocialLoginMemberResolver = SocialLoginMemberResolver(memberRepository, memberRegistrar)

private fun member(id: Long, userId: Long): Member = frontofficeMemberFixture(
    id = id,
    nickname = PROFILE.nickname,
    email = PROFILE.email,
    userId = userId,
    socialId = PROFILE.socialId,
)

private val PROFILE = SocialLoginProfile(123L, "nickname", "email@test.com")
