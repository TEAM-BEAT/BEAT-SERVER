package com.beat.apis.member.api.request

import com.beat.apis.member.api.type.SocialTypeRequest

data class MemberLoginRequest(
    val socialType: SocialTypeRequest,
)
