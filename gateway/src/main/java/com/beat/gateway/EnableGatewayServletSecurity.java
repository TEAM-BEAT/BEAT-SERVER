package com.beat.gateway;

import com.beat.gateway.authentication.internal.config.ServletSecurityConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ServletSecurityConfig.class)
public @interface EnableGatewayServletSecurity {
}
