package com.beat.gateway

import com.beat.gateway.shared.internal.GatewayConfigImportSelector
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(GatewayConfigImportSelector::class)
annotation class EnableGatewayConfig(
    val value: Array<GatewayConfigGroup>,
)
