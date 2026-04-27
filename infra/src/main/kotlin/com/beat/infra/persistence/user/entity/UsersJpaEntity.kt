package com.beat.infra.persistence.user.entity

import com.beat.domain.user.domain.Role
import jakarta.persistence.*

@Entity(name = "Users")
@Table(name = "users")
class UsersJpaEntity private constructor(
    id: Long?,
    role: Role,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = id
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(10) default 'USER'")
    var role: Role = role
        protected set

    companion object {
        @JvmStatic
        fun rehydrate(
            id: Long?,
            role: Role,
        ): UsersJpaEntity = UsersJpaEntity(
            id = id,
            role = role,
        )
    }
}
