package com.beat.apis.member.application.command

@ConsistentCopyVisibility
data class SocialLoginCommand private constructor(
    val socialType: SocialLoginProvider,
) {
    companion object {
        @JvmStatic
        fun from(socialType: SocialLoginProvider): SocialLoginCommand = SocialLoginCommand(socialType)
    }
}

enum class SocialLoginProvider {
    KAKAO,
}
