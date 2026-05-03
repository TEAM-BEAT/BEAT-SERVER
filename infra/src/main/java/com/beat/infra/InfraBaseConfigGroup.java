package com.beat.infra;

import com.beat.infra.config.AsyncConfig;
import com.beat.infra.config.ExternalClientConfig;
import com.beat.infra.config.JpaConfig;
import com.beat.infra.config.RedisCacheConfig;

/**
 * Selectable top-level infra slices for {@link EnableInfraBaseConfig}.
 *
 * <p>Every config class registered here must implement {@link InfraBaseConfig}.
 * Support configurations that are imported or scanned internally by these slices
 * must not be added to this enum and must not implement {@link InfraBaseConfig}.
 */
public enum InfraBaseConfigGroup {

	ASYNC(AsyncConfig.class),
	EXTERNAL_CLIENTS(ExternalClientConfig.class),
	JPA(JpaConfig.class),
	REDIS_CACHE(RedisCacheConfig.class);

	private final Class<? extends InfraBaseConfig> configClass;

	InfraBaseConfigGroup(Class<? extends InfraBaseConfig> configClass) {
		this.configClass = configClass;
	}

	public Class<? extends InfraBaseConfig> getConfigClass() {
		return configClass;
	}
}
