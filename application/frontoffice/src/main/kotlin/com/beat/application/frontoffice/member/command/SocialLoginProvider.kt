package com.beat.application.frontoffice.member.command

fun interface SocialLoginProvider {
    fun login(request: SocialLoginRequest): SocialLoginProfile
}
