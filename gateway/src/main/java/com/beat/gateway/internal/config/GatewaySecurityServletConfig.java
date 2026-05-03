package com.beat.gateway.internal.config;

import com.beat.gateway.security.internal.servlet.CustomAccessDeniedHandler;
import com.beat.gateway.security.internal.servlet.CustomJwtAuthenticationEntryPoint;
import com.beat.gateway.security.internal.servlet.JwtAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
	CustomAccessDeniedHandler.class,
	CustomJwtAuthenticationEntryPoint.class,
	JwtAuthenticationFilter.class
})
public class GatewaySecurityServletConfig {
}
