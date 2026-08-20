package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.member.exception.MemberApplicationErrorCode
import com.beat.domain.member.exception.DuplicateSocialIdentityException
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import org.springframework.stereotype.Component

@Component
internal class SocialLoginMemberResolver(
    private val memberRepository: MemberRepository,
    private val memberRegistrar: MemberRegistrar,
) {
    fun findOrRegister(
        socialLoginProfile: SocialLoginProfile,
        socialIdentity: SocialIdentity,
    ): MemberAuthenticationResult {
        val existingMember = memberRepository.findBySocialIdentity(socialIdentity)
        if (existingMember.isPresent) return existingMember.get().toAuthenticationResult()
        return try {
            val memberId = memberRegistrar.registerMemberWithUserInfo(socialLoginProfile, socialIdentity)
            findById(memberId)
        } catch (duplicate: DuplicateSocialIdentityException) {
            memberRepository.findBySocialIdentity(socialIdentity)
                .map { member -> member.toAuthenticationResult() }
                .orElseThrow { duplicate }
        }
    }

    private fun findById(memberId: Long): MemberAuthenticationResult = memberRepository.findById(memberId)
        .map { member -> member.toAuthenticationResult() }
        .orElseThrow { FrontofficeApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }

    private fun Member.toAuthenticationResult(): MemberAuthenticationResult = MemberAuthenticationResult(
        memberId = requireNotNull(getId()),
        userId = getUserId(),
    )
}
