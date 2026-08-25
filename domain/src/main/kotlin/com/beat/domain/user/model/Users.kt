package com.beat.domain.user.model

import com.beat.domain.sharedkernel.model.AggregateRoot

class Users
private constructor(
    private val userId: Id?,
    val role: Role,
) : AggregateRoot {
    val id: Long?
        get() = userId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Users) return false
        return userId != null && userId == other.userId
    }

    override fun hashCode(): Int = userId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Users(id=$id)"

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            fun from(value: Long): Id = Id(value)

            fun fromNullable(value: Long?): Id? = value?.let(::from)
        }
    }

    companion object {
        fun create(): Users =
            Users(
                userId = null,
                role = Role.USER,
            )

        fun createWithRole(role: Role): Users =
            Users(
                userId = null,
                role = role,
            )

        fun rehydrate(
            id: Long?,
            role: Role,
        ): Users =
            Users(
                userId = Id.fromNullable(id),
                role = role,
            )
    }
}
