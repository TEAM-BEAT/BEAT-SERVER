package com.beat.gateway.guest.internal.config

import com.beat.support.security.password.internal.BCryptPasswordHasher
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(BCryptPasswordHasher::class)
class GuestAccessConfig
