package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.member.exception.DuplicateSocialIdentityException
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SocialLoginMemberResolverTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var memberRegistrar: MemberRegistrar

    @Test
    fun `returns the existing member without registering another user`() {
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val existingMember = member(id = 11L, userId = 22L)
        Mockito.`when`(memberRepository.findBySocialIdentity(identity)).thenReturn(Optional.of(existingMember))

        val result = resolver().findOrRegister(PROFILE, identity)

        assertEquals(MemberAuthenticationResult(11L, 22L), result)
        Mockito.verifyNoInteractions(memberRegistrar)
    }

    @Test
    fun `registers a new member and reloads it by the registrar result`() {
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val registeredMember = member(id = 11L, userId = 22L)
        Mockito.`when`(memberRepository.findBySocialIdentity(identity)).thenReturn(Optional.empty())
        Mockito.`when`(memberRegistrar.registerMemberWithUserInfo(PROFILE, identity)).thenReturn(11L)
        Mockito.`when`(memberRepository.findById(11L)).thenReturn(Optional.of(registeredMember))

        val result = resolver().findOrRegister(PROFILE, identity)

        assertEquals(MemberAuthenticationResult(11L, 22L), result)
        Mockito.verify(memberRegistrar).registerMemberWithUserInfo(PROFILE, identity)
        Mockito.verify(memberRepository).findById(11L)
    }

    @Test
    fun `reloads the winner after a duplicate social identity registration race`() {
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        val winner = member(id = 11L, userId = 22L)
        val duplicate = DuplicateSocialIdentityException(IllegalStateException("unique constraint"))
        Mockito.`when`(memberRepository.findBySocialIdentity(identity))
            .thenReturn(Optional.empty(), Optional.of(winner))
        Mockito.`when`(memberRegistrar.registerMemberWithUserInfo(PROFILE, identity)).thenThrow(duplicate)

        val result = resolver().findOrRegister(PROFILE, identity)

        assertEquals(MemberAuthenticationResult(11L, 22L), result)
        Mockito.verify(memberRepository, Mockito.times(2)).findBySocialIdentity(identity)
    }

    @Test
    fun `fails with member not found when registration reload cannot find the new member`() {
        val identity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId)
        Mockito.`when`(memberRepository.findBySocialIdentity(identity)).thenReturn(Optional.empty())
        Mockito.`when`(memberRegistrar.registerMemberWithUserInfo(PROFILE, identity)).thenReturn(11L)
        Mockito.`when`(memberRepository.findById(11L)).thenReturn(Optional.empty())

        val exception = assertThrows<FrontofficeApplicationException> {
            resolver().findOrRegister(PROFILE, identity)
        }

        assertEquals("MEMBER_NOT_FOUND", exception.errorCode.code)
        assertEquals(FrontofficeApplicationErrorType.NOT_FOUND, exception.errorCode.type)
    }

    private fun resolver() = SocialLoginMemberResolver(memberRepository, memberRegistrar)

    private fun member(id: Long, userId: Long): Member = Member.rehydrate(
        id = id,
        nickname = PROFILE.nickname,
        email = PROFILE.email,
        deletedAt = null,
        userId = userId,
        socialIdentity = SocialIdentity.of(SocialType.KAKAO, PROFILE.socialId),
    )

    private companion object {
        val PROFILE = SocialLoginProfile(123L, "nickname", "email@test.com")
    }
}
