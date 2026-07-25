package com.beat.domain.user.model

enum class Role(
    val roleName: String,
) {
    USER("ROLE_USER"),
    MEMBER("ROLE_MEMBER"),
    ADMIN("ROLE_ADMIN"),
    ;
}
