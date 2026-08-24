package com.beat.application.frontoffice.fixture

import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity

fun frontofficeMemberFixture(
    id: Long = 1L,
    nickname: String = "member",
    email: String? = "member@example.com",
    userId: Long = 7L,
    socialId: Long = 123L,
): Member = Member.rehydrate(
    id = id,
    nickname = nickname,
    email = email,
    deletedAt = null,
    userId = userId,
    socialIdentity = SocialIdentity.of(SocialType.KAKAO, socialId),
)
