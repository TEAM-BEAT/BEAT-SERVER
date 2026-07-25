package com.beat.apis.member.application.command

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.application.result.MemberAuthenticationResult
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.contracts.auth.social.SocialMemberInfo
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
        socialMemberInfo: SocialMemberInfo,
        socialIdentity: SocialIdentity,
    ): MemberAuthenticationResult {
        val existingMember = memberRepository.findBySocialIdentity(socialIdentity)
        if (existingMember.isPresent) return existingMember.get().toAuthenticationResult()
        return try {
            val memberId = memberRegistrar.registerMemberWithUserInfo(socialMemberInfo, socialIdentity)
            findById(memberId)
        } catch (duplicate: DuplicateSocialIdentityException) {
            memberRepository.findBySocialIdentity(socialIdentity)
                .map { member -> member.toAuthenticationResult() }
                .orElseThrow { duplicate }
        }
    }

    private fun findById(memberId: Long): MemberAuthenticationResult = memberRepository.findById(memberId)
        .map { member -> member.toAuthenticationResult() }
        .orElseThrow { ApiApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }

    private fun Member.toAuthenticationResult(): MemberAuthenticationResult = MemberAuthenticationResult(
        memberId = getId(),
        userId = getUserId(),
    )
}
