package com.beat.admin.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
public class AdminSecurityConfig {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String[] SWAGGER_WHITELIST = {
		"/v3/api-docs/**",
		"/swagger-ui/**",
		"/swagger-resources/**",
	};

	private final OncePerRequestFilter jwtAuthenticationFilter;
	private final AuthenticationEntryPoint authenticationEntryPoint;
	private final AccessDeniedHandler accessDeniedHandler;
	private final Environment environment;

	@Value("${management.endpoints.web.base-path}")
	private String actuatorEndPoint;

	public AdminSecurityConfig(
		@Qualifier("gatewayJwtAuthenticationFilter") OncePerRequestFilter jwtAuthenticationFilter,
		AuthenticationEntryPoint authenticationEntryPoint,
		AccessDeniedHandler accessDeniedHandler,
		Environment environment
	) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
		this.environment = environment;
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
					.anyRequest().hasAuthority(ROLE_ADMIN))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private String[] getAuthWhitelist() {
		List<String> whitelist = new ArrayList<>(List.of(
			"/error",
			actuatorEndPoint + "/health",
			actuatorEndPoint + "/prometheus"
		));
		if (!environment.acceptsProfiles(Profiles.of("prod"))) {
			Collections.addAll(whitelist, SWAGGER_WHITELIST);
		}
		return whitelist.toArray(String[]::new);
	}
}
