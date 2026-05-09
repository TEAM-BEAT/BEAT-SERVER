package com.beat.gateway;

import com.beat.gateway.internal.config.GatewayRefreshTokenConfig;

public enum GatewayConfigGroup {

	REFRESH_TOKEN_STORE(GatewayRefreshTokenConfig.class);

	private final Class<?> configClass;

	GatewayConfigGroup(Class<?> configClass) {
		this.configClass = configClass;
	}

	public Class<?> getConfigClass() {
		return configClass;
	}
}
