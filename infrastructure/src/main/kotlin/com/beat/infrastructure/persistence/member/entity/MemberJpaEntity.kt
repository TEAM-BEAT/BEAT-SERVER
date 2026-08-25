package com.beat.infrastructure.persistence.member.entity

import com.beat.domain.member.model.SocialType
import com.beat.infrastructure.persistence.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity(name = "Member")
@Table(
    name = "member",
    uniqueConstraints =
        [
            UniqueConstraint(name = "uk_member_user_id", columnNames = ["user_id"]),
            UniqueConstraint(
                name = "uk_member_social_identity",
                columnNames = ["social_type", "social_id"],
            ),
        ],
)
internal class MemberJpaEntity
private constructor(
    id: Long?,
    nickname: String,
    email: String?,
    deletedAt: LocalDateTime?,
    userId: Long,
    socialId: Long,
    socialType: SocialType,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Column(nullable = false)
    var nickname: String = nickname
        protected set

    @Column(nullable = true)
    var email: String? = email
        protected set

    @Column(nullable = true)
    var deletedAt: LocalDateTime? = deletedAt
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "social_id", nullable = false)
    var socialId: Long = socialId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false)
    var socialType: SocialType = socialType
        protected set

    companion object {
        fun rehydrate(
            id: Long?,
            nickname: String,
            email: String?,
            deletedAt: LocalDateTime?,
            userId: Long,
            socialId: Long,
            socialType: SocialType,
        ): MemberJpaEntity =
            MemberJpaEntity(
                id = id,
                nickname = nickname,
                email = email,
                deletedAt = deletedAt,
                userId = userId,
                socialId = socialId,
                socialType = socialType,
            )
    }
}
