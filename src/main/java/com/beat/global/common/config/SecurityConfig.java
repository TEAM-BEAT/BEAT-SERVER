package com.beat.global.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.beat.domain.user.domain.Role;
import com.beat.global.auth.jwt.filter.JwtAuthenticationFilter;
import com.beat.global.auth.security.CustomAccessDeniedHandler;
import com.beat.global.auth.security.CustomJwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomJwtAuthenticationEntryPoint customJwtAuthenticationEntryPoint;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;

	@Value("${management.endpoints.web.base-path}")
	private String actuatorEndPoint;

	public String[] getAuthWhitelist() {
		return new String[] {
			"/api/users/sign-up",
			"/api/users/refresh-token",
			"/api/bookings/guest/**",
			"/api/main",
			"/api/performances/booking/**",
			"/api/schedules/**",
			"/api/notifications/**",
			"/api/performances/detail/**",
			"/health-check",
			"/actuator/health",
			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/swagger-resources/**",
			"/api/files/**",
			"/error",
			"/api/bookings/refund",
			"/api/bookings/cancel",
			actuatorEndPoint + "/health",
			actuatorEndPoint + "/prometheus",
			"/"
		};
	}

	private static final String[] AUTH_ADMIN_ONLY = {
		"/api/admin/**"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception ->
				exception.authenticationEntryPoint(customJwtAuthenticationEntryPoint)
					.accessDeniedHandler(customAccessDeniedHandler));

		http.authorizeHttpRequests(auth ->
				auth.requestMatchers(getAuthWhitelist()).permitAll()
					.requestMatchers(AUTH_ADMIN_ONLY).hasAuthority(Role.ADMIN.getRoleName())
					.anyRequest().authenticated())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
