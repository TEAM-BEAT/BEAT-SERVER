package com.beat.gateway.guest.internal.config

import com.beat.gateway.guest.internal.GuestPasswordHashService
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(GuestPasswordHashService::class)
class GuestAccessConfig
