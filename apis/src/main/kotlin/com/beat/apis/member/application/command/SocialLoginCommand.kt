package com.beat.apis.member.application.command

data class SocialLoginCommand(
    val socialType: SocialLoginProvider,
)

enum class SocialLoginProvider {
    KAKAO,
}
