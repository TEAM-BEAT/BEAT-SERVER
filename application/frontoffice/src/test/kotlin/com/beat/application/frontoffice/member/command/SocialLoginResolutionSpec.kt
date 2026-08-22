package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.member.exception.DuplicateSocialIdentityException
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito

class SocialLoginResolutionSpec : FunSpec({

    test("existing member login returns its authentication result without registering another user") {
        val memberRepository = Mockito.mock(MemberRepository::class.java)
        val memberRegistrar = Mockito.mock(MemberRegistrar::class.java)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val existingMember = member(id = 11L, userId = 22L)
        Mockito.`when`(memberRepository.findBySocialIdentity(identity)).thenReturn(existingMember)

        val result = resolver(memberRepository, memberRegistrar).findOrRegister(PROFILE, identity)

        result shouldBe MemberAuthenticationResult(11L, 22L)
        Mockito.verifyNoInteractions(memberRegistrar)
    }

    test("new member registration reloads the persisted member by registrar result") {
        val memberRepository = Mockito.mock(MemberRepository::class.java)
        val memberRegistrar = Mockito.mock(MemberRegistrar::class.java)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val registeredMember = member(id = 11L, userId = 22L)
        Mockito.`when`(memberRepository.findBySocialIdentity(identity)).thenReturn(null)
        Mockito.`when`(memberRegistrar.registerMemberWithUserInfo(PROFILE, identity)).thenReturn(11L)
        Mockito.`when`(memberRepository.findById(11L)).thenReturn(registeredMember)

        val result = resolver(memberRepository, memberRegistrar).findOrRegister(PROFILE, identity)

        result shouldBe MemberAuthenticationResult(11L, 22L)
        Mockito.verify(memberRegistrar).registerMemberWithUserInfo(PROFILE, identity)
        Mockito.verify(memberRepository).findById(11L)
    }

    test("duplicate identity registration race reloads the winning member") {
        val memberRepository = Mockito.mock(MemberRepository::class.java)
        val memberRegistrar = Mockito.mock(MemberRegistrar::class.java)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val winner = member(id = 11L, userId = 22L)
        val duplicate = DuplicateSocialIdentityException(IllegalStateException("unique constraint"))
        Mockito.`when`(memberRepository.findBySocialIdentity(identity))
            .thenReturn(null, winner)
        Mockito.`when`(memberRegistrar.registerMemberWithUserInfo(PROFILE, identity)).thenThrow(duplicate)

        val result = resolver(memberRepository, memberRegistrar).findOrRegister(PROFILE, identity)

        result shouldBe MemberAuthenticationResult(11L, 22L)
        Mockito.verify(memberRepository, Mockito.times(2)).findBySocialIdentity(identity)
    }

    test("missing post-registration member maps to member not found") {
        val memberRepository = Mockito.mock(MemberRepository::class.java)
        val memberRegistrar = Mockito.mock(MemberRegistrar::class.java)
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        Mockito.`when`(memberRepository.findBySocialIdentity(identity)).thenReturn(null)
        Mockito.`when`(memberRegistrar.registerMemberWithUserInfo(PROFILE, identity)).thenReturn(11L)
        Mockito.`when`(memberRepository.findById(11L)).thenReturn(null)

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

private fun member(id: Long, userId: Long): Member = Member.rehydrate(
    id = id,
    nickname = PROFILE.nickname,
    email = PROFILE.email,
    deletedAt = null,
    userId = userId,
    socialIdentity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId),
)

private val PROFILE = SocialLoginProfile(123L, "nickname", "email@test.com")
