package com.beat.gateway.authentication.internal.config;

import com.beat.gateway.authentication.internal.CustomAccessDeniedHandler;
import com.beat.gateway.authentication.internal.CustomJwtAuthenticationEntryPoint;
import com.beat.gateway.authentication.internal.JwtAuthenticationFilter;
import com.beat.gateway.authentication.internal.SecurityMdcLoggingFilter;
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
public class SecurityFilterConfig {

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
