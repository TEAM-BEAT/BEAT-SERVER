package com.beat.domain.member.exception

class DuplicateSocialIdentityException(
    cause: Throwable,
) : RuntimeException("Social identity already exists", cause)
