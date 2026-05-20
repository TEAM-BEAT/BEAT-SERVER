package com.beat.gateway.internal.config;

import com.beat.gateway.security.internal.servlet.CustomAccessDeniedHandler;
import com.beat.gateway.security.internal.servlet.CustomJwtAuthenticationEntryPoint;
import com.beat.gateway.security.internal.servlet.JwtAuthenticationFilter;
import com.beat.gateway.security.internal.servlet.SecurityMdcLoggingFilter;
import com.beat.observability.tracing.TraceContextResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
	CustomAccessDeniedHandler.class,
	CustomJwtAuthenticationEntryPoint.class,
	JwtAuthenticationFilter.class
})
public class GatewaySecurityServletConfig {

	@Bean(name = "gatewaySecurityMdcLoggingFilter")
	public SecurityMdcLoggingFilter gatewaySecurityMdcLoggingFilter(
		TraceContextResolver traceContextResolver,
		@Value("${management.server.port}") int managementPort
	) {
		return new SecurityMdcLoggingFilter(traceContextResolver, managementPort);
	}

	@Bean
	public FilterRegistrationBean<SecurityMdcLoggingFilter> gatewaySecurityMdcLoggingFilterRegistration(
		@Qualifier("gatewaySecurityMdcLoggingFilter") SecurityMdcLoggingFilter filter
	) {
		FilterRegistrationBean<SecurityMdcLoggingFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}
}
