package com.beat.domain.performance.model

class Cast private constructor(
    private val castId: Id?,
    val castName: String,
    val castRole: String,
    val castPhoto: String,
) {
    fun getId(): Long? = castId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cast) return false
        return castId != null && castId == other.castId
    }

    override fun hashCode(): Int = castId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Cast(id=${getId()})"

    fun update(castName: String, castRole: String, castPhoto: String): Cast = Cast(
        castId = castId,
        castName = castName,
        castRole = castRole,
        castPhoto = castPhoto,
    )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            @JvmStatic
            fun fromNullable(value: Long?): Id? = value?.let(::Id)
        }
    }

    companion object {
        @JvmStatic
        fun create(castName: String, castRole: String, castPhoto: String): Cast = Cast(
            castId = null,
            castName = castName,
            castRole = castRole,
            castPhoto = castPhoto,
        )

        @JvmStatic
        fun rehydrate(id: Long?, castName: String, castRole: String, castPhoto: String): Cast = Cast(
            castId = Id.fromNullable(id),
            castName = castName,
            castRole = castRole,
            castPhoto = castPhoto,
        )
    }
}
