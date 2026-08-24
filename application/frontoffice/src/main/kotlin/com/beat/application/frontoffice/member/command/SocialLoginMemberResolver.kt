package com.beat.application.frontoffice.member.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.member.exception.MemberApplicationErrorCode
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
        existingMember?.let { return it.toAuthenticationResult() }
        val memberId = memberRegistrar.registerMemberWithUserInfo(socialLoginProfile, socialIdentity)
        return findById(memberId)
    }

    fun findExisting(socialIdentity: SocialIdentity): MemberAuthenticationResult? =
        memberRepository.findBySocialIdentity(socialIdentity)?.toAuthenticationResult()

    private fun findById(memberId: Long): MemberAuthenticationResult =
        memberRepository.findById(memberId)?.toAuthenticationResult()
            ?: throw FrontofficeApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND)

    private fun Member.toAuthenticationResult(): MemberAuthenticationResult = MemberAuthenticationResult(
        memberId = requireNotNull(id),
        userId = userId,
    )
}
