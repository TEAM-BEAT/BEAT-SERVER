package com.beat.domain.member.repository

import com.beat.domain.member.model.Member
import com.beat.domain.member.vo.SocialIdentity

interface MemberRepository {
    fun findById(id: Long): Member?

    fun save(member: Member): Member

    fun findBySocialIdentity(socialIdentity: SocialIdentity): Member?

    fun count(): Long
}
