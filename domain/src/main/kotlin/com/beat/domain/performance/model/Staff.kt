package com.beat.domain.performance.model

class Staff private constructor(
    private val staffId: Id?,
    val staffName: String,
    val staffRole: String,
    val staffPhoto: String,
) {
    fun getId(): Long? = staffId?.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Staff) return false
        return staffId != null && staffId == other.staffId
    }

    override fun hashCode(): Int = staffId?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Staff(id=${getId()})"

    fun update(staffName: String, staffRole: String, staffPhoto: String): Staff = Staff(
        staffId = staffId,
        staffName = staffName,
        staffRole = staffRole,
        staffPhoto = staffPhoto,
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
        fun create(staffName: String, staffRole: String, staffPhoto: String): Staff = Staff(
            staffId = null,
            staffName = staffName,
            staffRole = staffRole,
            staffPhoto = staffPhoto,
        )

        @JvmStatic
        fun rehydrate(id: Long?, staffName: String, staffRole: String, staffPhoto: String): Staff = Staff(
            staffId = Id.fromNullable(id),
            staffName = staffName,
            staffRole = staffRole,
            staffPhoto = staffPhoto,
        )
    }
}
