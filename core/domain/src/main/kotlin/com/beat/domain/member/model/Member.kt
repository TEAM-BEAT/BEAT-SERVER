package com.beat.domain.member.model

import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.sharedkernel.model.AggregateRoot
import com.beat.domain.user.model.Users
import java.time.LocalDateTime

class Member private constructor(
    private val memberId: Id?,
    val nickname: String,
    val email: String?,
    val deletedAt: LocalDateTime?,
    private val linkedUserId: Users.Id,
    val socialIdentity: SocialIdentity,
) : AggregateRoot {
    fun getId(): Long? = memberId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Member) return false
        return memberId != null && memberId == other.memberId
    }

    override fun hashCode(): Int = memberId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Member(id=${getId()})"

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
            socialIdentity: SocialIdentity,
        ): Member = Member(
            memberId = null,
            nickname = nickname,
            email = email,
            deletedAt = null,
            linkedUserId = Users.Id.from(userId),
            socialIdentity = socialIdentity,
        )

        @JvmStatic
        fun rehydrate(
            id: Long?,
            nickname: String,
            email: String?,
            deletedAt: LocalDateTime?,
            userId: Long,
            socialIdentity: SocialIdentity,
        ): Member = Member(
            memberId = Id.fromNullable(id),
            nickname = nickname,
            email = email,
            deletedAt = deletedAt,
            linkedUserId = Users.Id.from(userId),
            socialIdentity = socialIdentity,
        )
    }
}
