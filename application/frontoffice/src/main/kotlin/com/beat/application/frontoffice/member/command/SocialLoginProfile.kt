package com.beat.application.frontoffice.member.command

data class SocialLoginProfile(
    val socialId: Long,
    val nickname: String,
    val email: String,
)
