package com.beat.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.util.StringUtils;

import com.beat.infra.InfraBaseConfig;

// TODO: transition-only — auth.jwt.dao ownership이 gateway/auth 모듈로 이동하는 시점에 @EnableRedisRepositories도 함께 이동
@Configuration(proxyBeanMethods = false)
@EnableRedisRepositories(basePackages = "com.beat.global.auth.jwt.dao")
public class RedisConfig implements InfraBaseConfig {

	@Value("${spring.data.redis.host:localhost}")
	private String host;

	@Value("${spring.data.redis.port:6379}")
	private int port;

	@Value("${spring.data.redis.password:#{null}}")
	private String password;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory(createStandaloneConfiguration());
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		return redisTemplate;
	}

	private RedisStandaloneConfiguration createStandaloneConfiguration() {
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);

		if (StringUtils.hasText(password)) {
			configuration.setPassword(password);
		}

		return configuration;
	}
}
