package com.beat.domain.member.domain

import java.time.LocalDateTime

@ConsistentCopyVisibility
data class Member private constructor(
    private val memberId: Id?,
    val nickname: String,
    val email: String?,
    val deletedAt: LocalDateTime?,
    val userId: Long,
    val socialId: Long,
    val socialType: SocialType,
) {
    fun getId(): Long? = memberId?.value

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            @JvmStatic fun from(value: Long): Id = Id(value)
            @JvmStatic fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
        @JvmStatic
        fun create(
            nickname: String,
            email: String?,
            userId: Long,
            socialId: Long,
            socialType: SocialType,
        ): Member = Member(
            memberId = null,
            nickname = nickname,
            email = email,
            deletedAt = null,
            userId = userId,
            socialId = socialId,
            socialType = socialType,
        )

        @JvmStatic
        fun rehydrate(
            id: Long?,
            nickname: String,
            email: String?,
            deletedAt: LocalDateTime?,
            userId: Long,
            socialId: Long,
            socialType: SocialType,
        ): Member = Member(
            memberId = Id.fromNullable(id),
            nickname = nickname,
            email = email,
            deletedAt = deletedAt,
            userId = userId,
            socialId = socialId,
            socialType = socialType,
        )
    }
}
