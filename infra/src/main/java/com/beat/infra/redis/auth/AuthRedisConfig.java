package com.beat.infra.redis.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import com.beat.infra.InfraBaseConfig;
import com.beat.infra.redis.auth.guest.GuestSessionRedisRepository;
import com.beat.infra.redis.auth.guest.RedisGuestAccessThrottleAdapter;
import com.beat.infra.redis.auth.guest.RedisGuestSessionAdapter;
import com.beat.infra.redis.auth.refreshtoken.RedisRefreshTokenAdapter;
import com.beat.infra.redis.auth.refreshtoken.RefreshTokenRedisRepository;

@Configuration(proxyBeanMethods = false)
@EnableRedisRepositories(basePackageClasses = {
	RefreshTokenRedisRepository.class,
	GuestSessionRedisRepository.class
})
@Import({
	RedisRefreshTokenAdapter.class,
	RedisGuestSessionAdapter.class,
	RedisGuestAccessThrottleAdapter.class
})
public class AuthRedisConfig implements InfraBaseConfig {

	@Bean
	public RedisScript<Long> recordGuestAccessFailureScript() {
		return RedisScript.of(
			new ClassPathResource("redis/scripts/record-guest-access-failure.lua"),
			Long.class
		);
	}
}
