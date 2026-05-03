package com.beat.apis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;


@Configuration(proxyBeanMethods = false)
public class ApisSecurityConfig {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String[] AUTH_ADMIN_ONLY = {
		"/api/admin/**"
	};

	private final OncePerRequestFilter jwtAuthenticationFilter;
	private final AuthenticationEntryPoint authenticationEntryPoint;
	private final AccessDeniedHandler accessDeniedHandler;

	@Value("${management.endpoints.web.base-path}")
	private String actuatorEndPoint;

	public ApisSecurityConfig(
		@Qualifier("gatewayJwtAuthenticationFilter") OncePerRequestFilter jwtAuthenticationFilter,
		AuthenticationEntryPoint authenticationEntryPoint,
		AccessDeniedHandler accessDeniedHandler
	) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler));

		http.authorizeHttpRequests(auth ->
				auth.requestMatchers(getAuthWhitelist()).permitAll()
					.requestMatchers(AUTH_ADMIN_ONLY).hasAuthority(ROLE_ADMIN)
					.anyRequest().authenticated())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private String[] getAuthWhitelist() {
		return new String[] {
			"/api/users/sign-up",
			"/api/users/refresh-token",
			"/api/bookings/guest/**",
			"/api/main",
			"/api/performances/booking/**",
			"/api/schedules/**",
			"/api/notifications/**",
			"/api/performances/detail/**",
			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/swagger-resources/**",
			"/api/files/**",
			"/error",
			"/api/bookings/refund",
			"/api/bookings/cancel",
			actuatorEndPoint + "/health",
			actuatorEndPoint + "/prometheus"
		};
	}
}
