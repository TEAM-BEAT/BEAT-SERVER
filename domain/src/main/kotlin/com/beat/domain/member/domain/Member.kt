package com.beat.domain.member.domain

import com.beat.domain.user.domain.Users
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class Member private constructor(
    private val memberId: Id?,
    val nickname: String,
    val email: String?,
    val deletedAt: LocalDateTime?,
    private val linkedUserId: Users.Id,
    val socialId: Long,
    val socialType: SocialType,
) {
    fun getId(): Long? = memberId?.value

    fun getUserId(): Long = linkedUserId.value

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            @JvmStatic
            fun from(value: Long): Id = Id(value)

            @JvmStatic
            fun fromNullable(value: Long?): Id? = value?.let(::from)
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
            linkedUserId = Users.Id.from(userId),
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
            linkedUserId = Users.Id.from(userId),
            socialId = socialId,
            socialType = socialType,
        )
    }
}
