package com.beat.gateway.guest.internal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.beat.gateway.guest.internal.GuestAccessThrottleService;
import com.beat.gateway.guest.internal.GuestPasswordHashService;
import com.beat.gateway.guest.internal.GuestSessionService;
import com.beat.gateway.shared.internal.config.RedisConfig;

@Configuration(proxyBeanMethods = false)
@Import({
	RedisConfig.class,
	GuestSessionService.class,
	GuestPasswordHashService.class,
	GuestAccessThrottleService.class
})
public class GuestAccessConfig {
}
