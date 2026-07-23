package com.beat.gateway;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.beat.gateway.shared.internal.GatewayConfigImportSelector;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(GatewayConfigImportSelector.class)
public @interface EnableGatewayConfig {

	GatewayConfigGroup[] value();
}
