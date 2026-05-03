package com.beat.gateway.internal.config;

import com.beat.gateway.jwt.internal.JwtTokenProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(JwtTokenProvider.class)
public class GatewayJwtConfig {
}
