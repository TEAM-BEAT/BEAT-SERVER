package com.beat.gateway;

import com.beat.gateway.internal.config.GatewayRefreshTokenConfig;
import com.beat.gateway.internal.config.GatewayServletSecurityConfig;

public enum GatewayConfigGroup {

	SERVLET_SECURITY(GatewayServletSecurityConfig.class),
	REFRESH_TOKEN_STORE(GatewayRefreshTokenConfig.class);

	private final Class<?> configClass;

	GatewayConfigGroup(Class<?> configClass) {
		this.configClass = configClass;
	}

	public Class<?> getConfigClass() {
		return configClass;
	}
}
