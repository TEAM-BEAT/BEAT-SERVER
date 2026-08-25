package com.beat.infrastructure

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(InfraBaseConfigImportSelector::class)
annotation class EnableInfraBaseConfig(val value: Array<InfraBaseConfigGroup>)
