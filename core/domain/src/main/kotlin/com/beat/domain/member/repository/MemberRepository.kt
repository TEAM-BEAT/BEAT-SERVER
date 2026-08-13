package com.beat.domain.member.repository

import com.beat.domain.member.model.Member
import com.beat.domain.member.vo.SocialIdentity
import java.util.*

@JvmSuppressWildcards
interface MemberRepository {
    fun findById(id: Long?): Optional<Member>

    fun save(member: Member): Member

    fun findBySocialIdentity(socialIdentity: SocialIdentity): Optional<Member>

    fun count(): Long
}
