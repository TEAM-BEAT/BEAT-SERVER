package com.beat.global.auth.client.service;

import com.beat.global.auth.client.dto.MemberInfoResponse;
import com.beat.global.auth.client.dto.MemberLoginRequest;

public interface SocialService {
    MemberInfoResponse login(final String authorizationToken, final MemberLoginRequest loginRequest);
}
