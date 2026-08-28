package com.beat.application.admin.fixture

import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity

fun adminMemberFixture(
    id: Long = 7L,
    userId: Long = 1L,
): Member =
    Member.rehydrate(
        id = id,
        nickname = "admin",
        email = "admin@example.com",
        deletedAt = null,
        userId = userId,
        socialIdentity = SocialIdentity.of(SocialType.KAKAO, 10L),
    )
