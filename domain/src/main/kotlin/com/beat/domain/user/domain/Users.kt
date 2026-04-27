package com.beat.domain.user.domain

@ConsistentCopyVisibility
data class Users private constructor(
    private val userId: Id?,
    val role: Role,
) {
    fun getId(): Long? = userId?.value

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
        fun create(): Users = Users(
            userId = null,
            role = Role.USER
        )

        @JvmStatic
        fun createWithRole(role: Role): Users = Users(
            userId = null,
            role = role
        )

        @JvmStatic
        fun rehydrate(
            id: Long?,
            role: Role,
        ): Users = Users(
            userId = Id.fromNullable(id),
            role = role
        )
    }
}
