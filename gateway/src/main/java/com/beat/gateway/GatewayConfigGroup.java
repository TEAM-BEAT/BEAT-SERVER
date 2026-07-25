package com.beat.gateway;

import com.beat.gateway.guest.internal.config.GuestAccessConfig;
import com.beat.gateway.refreshtoken.internal.config.RefreshTokenConfig;

public enum GatewayConfigGroup {

	REFRESH_TOKEN_STORE(RefreshTokenConfig.class),
	GUEST_ACCESS(GuestAccessConfig.class);

	private final Class<?> configClass;

	GatewayConfigGroup(Class<?> configClass) {
		this.configClass = configClass;
	}

	public Class<?> getConfigClass() {
		return configClass;
	}
}
