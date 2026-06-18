package com.beat.contracts.auth.social

data class SocialMemberInfo(
    val socialId: Long,
    val nickname: String,
    val email: String,
)
