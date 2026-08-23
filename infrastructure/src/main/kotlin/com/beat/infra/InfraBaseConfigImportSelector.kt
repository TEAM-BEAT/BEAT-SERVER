package com.beat.infra

import org.springframework.context.annotation.DeferredImportSelector
import org.springframework.core.type.AnnotationMetadata

internal class InfraBaseConfigImportSelector : DeferredImportSelector {
    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> =
        getValues(importingClassMetadata)
            .map { it.configClass.name }
            .toTypedArray()

    @Suppress("UNCHECKED_CAST")
    private fun getValues(metadata: AnnotationMetadata): List<InfraBaseConfigGroup> =
        metadata.getAnnotationAttributes(EnableInfraBaseConfig::class.java.name)
            ?.let { it["value"] as Array<InfraBaseConfigGroup>? }
            ?.toList()
            .orEmpty()
}
