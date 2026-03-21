package com.beat.infra;

import org.springframework.context.annotation.Import;

@Import(InfraBaseConfigImportSelector.class)
public @interface EnableInfraBaseConfig {

	InfraBaseConfigGroup[] value();
}
