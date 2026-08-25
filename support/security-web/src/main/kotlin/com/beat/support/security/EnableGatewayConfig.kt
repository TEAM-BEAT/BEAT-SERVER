package com.beat.support.security

import com.beat.support.security.shared.internal.GatewayConfigImportSelector
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(GatewayConfigImportSelector::class)
annotation class EnableGatewayConfig(val value: Array<GatewayConfigGroup>)
