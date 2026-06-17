package com.beat.contracts.auth.social


fun interface SocialLoginPort {

    fun login(request: SocialLoginRequest): SocialMemberInfo
}
