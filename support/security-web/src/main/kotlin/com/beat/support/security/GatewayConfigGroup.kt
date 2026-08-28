package com.beat.support.security

import com.beat.support.security.guest.internal.config.GuestAccessConfig

enum class GatewayConfigGroup(val configClass: Class<*>) {
    GUEST_ACCESS(GuestAccessConfig::class.java)
}
