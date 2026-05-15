package com.beat.gateway.internal.config;

import com.beat.gateway.jwt.internal.JwtProperties;
import com.beat.gateway.jwt.internal.JwtTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class GatewayJwtConfig {

	@Bean
	public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
		return new JwtTokenProvider(jwtProperties);
	}
}
