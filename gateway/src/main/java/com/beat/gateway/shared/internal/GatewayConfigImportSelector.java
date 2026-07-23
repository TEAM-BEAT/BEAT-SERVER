package com.beat.gateway.shared.internal;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import com.beat.gateway.EnableGatewayConfig;
import com.beat.gateway.GatewayConfigGroup;

public class GatewayConfigImportSelector implements DeferredImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return getValues(importingClassMetadata).stream()
			.map(group -> group.getConfigClass().getName())
			.toArray(String[]::new);
	}

	private List<GatewayConfigGroup> getValues(AnnotationMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(EnableGatewayConfig.class.getName());
		if (attributes == null) {
			return List.of();
		}

		GatewayConfigGroup[] values = (GatewayConfigGroup[])attributes.get("value");
		return values == null ? List.of() : List.of(values);
	}
}
