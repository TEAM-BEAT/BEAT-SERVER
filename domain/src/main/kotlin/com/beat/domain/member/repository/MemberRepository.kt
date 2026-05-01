package com.beat.domain.member.repository

import com.beat.domain.member.domain.Member
import com.beat.domain.member.domain.SocialType
import java.util.*

@JvmSuppressWildcards
interface MemberRepository {
    fun findById(id: Long?): Optional<Member>

    fun save(member: Member): Member

    fun findBySocialTypeAndSocialId(
        socialId: Long?,
        socialType: SocialType,
    ): Optional<Member>

    fun count(): Long
}
