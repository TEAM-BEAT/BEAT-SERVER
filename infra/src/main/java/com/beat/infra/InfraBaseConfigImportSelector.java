package com.beat.infra;

import java.util.Map;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class InfraBaseConfigImportSelector implements DeferredImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return getValues(importingClassMetadata).stream()
			.map(group -> group.getConfigClass().getName())
			.toArray(String[]::new);
	}

	private java.util.List<InfraBaseConfigGroup> getValues(AnnotationMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(EnableInfraBaseConfig.class.getName());
		if (attributes == null) {
			return java.util.List.of();
		}

		InfraBaseConfigGroup[] values = (InfraBaseConfigGroup[]) attributes.get("value");
		return values == null ? java.util.List.of() : java.util.List.of(values);
	}
}
