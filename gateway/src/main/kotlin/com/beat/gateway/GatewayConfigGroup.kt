package com.beat.gateway

import com.beat.gateway.guest.internal.config.GuestAccessConfig
import com.beat.gateway.refreshtoken.internal.config.RefreshTokenConfig

enum class GatewayConfigGroup(
    val configClass: Class<*>,
) {
    REFRESH_TOKEN_STORE(RefreshTokenConfig::class.java),
    GUEST_ACCESS(GuestAccessConfig::class.java),
}
