package com.beat.gateway.internal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
	GatewayJwtConfig.class,
	GatewaySecurityServletConfig.class,
	GatewayWebMvcConfig.class
})
public class GatewayServletSecurityConfig {
}
