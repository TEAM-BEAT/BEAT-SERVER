package com.beat.domain.performance.model

class Cast private constructor(
    private val castId: Id?,
    val castName: String,
    val castRole: String,
    val castPhoto: String,
) {
    val id: Long?
        get() = castId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cast) return false
        return castId != null && castId == other.castId
    }

    override fun hashCode(): Int = castId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Cast(id=$id)"

    fun update(castName: String, castRole: String, castPhoto: String): Cast = Cast(
        castId = castId,
        castName = castName,
        castRole = castRole,
        castPhoto = castPhoto,
    )

    @JvmInline
    value class Id private constructor(val value: Long) {
        companion object {
            fun fromNullable(value: Long?): Id? = value?.let(::Id)
        }
    }

    companion object {
        fun create(castName: String, castRole: String, castPhoto: String): Cast = Cast(
            castId = null,
            castName = castName,
            castRole = castRole,
            castPhoto = castPhoto,
        )

        fun rehydrate(id: Long?, castName: String, castRole: String, castPhoto: String): Cast = Cast(
            castId = Id.fromNullable(id),
            castName = castName,
            castRole = castRole,
            castPhoto = castPhoto,
        )
    }
}
