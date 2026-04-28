package com.beat.infra.persistence.member.entity

import com.beat.domain.BaseTimeEntity
import com.beat.domain.member.domain.SocialType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity(name = "Member")
@Table(name = "member")
class MemberJpaEntity private constructor(
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

    @Column(nullable = false)
    var socialId: Long = socialId
        protected set

    @Enumerated(EnumType.STRING)
    var socialType: SocialType = socialType
        protected set

    companion object {
        @JvmStatic
        fun rehydrate(
            id: Long?,
            nickname: String,
            email: String?,
            deletedAt: LocalDateTime?,
            userId: Long,
            socialId: Long,
            socialType: SocialType,
        ): MemberJpaEntity = MemberJpaEntity(
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
