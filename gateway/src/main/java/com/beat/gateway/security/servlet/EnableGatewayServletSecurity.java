package com.beat.gateway.security.servlet;

import com.beat.gateway.internal.config.GatewayServletSecurityConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(GatewayServletSecurityConfig.class)
public @interface EnableGatewayServletSecurity {
}
