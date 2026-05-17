package com.beat.domain.user.domain

enum class Role(
    val roleName: String,
) {
    USER("ROLE_USER"),
    MEMBER("ROLE_MEMBER"),
    ADMIN("ROLE_ADMIN"),
    ;
}
