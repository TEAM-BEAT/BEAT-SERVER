package com.beat.application.frontoffice.member.command

import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.model.Role
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MemberRegistrar(
    private val eventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun registerMemberWithUserInfo(
        socialLoginProfile: SocialLoginProfile,
        socialIdentity: SocialIdentity,
    ): Long {
        val user = userRepository.save(Users.createWithRole(Role.MEMBER))
        val member = memberRepository.save(
            Member.create(
                socialLoginProfile.nickname,
                socialLoginProfile.email,
                requireNotNull(user.id),
                socialIdentity,
            ),
        )
        log.info { "Member registered with memberId: ${member.id}, role: ${user.role}" }
        eventPublisher.publishEvent(MemberRegisteredEvent(member.nickname))
        return requireNotNull(member.id)
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
