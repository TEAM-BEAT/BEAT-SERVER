package com.beat.domain.member.vo

import com.beat.domain.member.model.SocialType

@ConsistentCopyVisibility
data class SocialIdentity
private constructor(
    val socialType: SocialType,
    val socialId: Long,
) {
    override fun toString(): String = "SocialIdentity(REDACTED)"

    companion object {
        fun of(socialType: SocialType, socialId: Long): SocialIdentity =
            SocialIdentity(
                socialType = socialType,
                socialId = socialId,
            )
    }
}
