package com.beat.support.security.shared.internal

import com.beat.support.security.EnableGatewayConfig
import com.beat.support.security.GatewayConfigGroup
import org.springframework.context.annotation.DeferredImportSelector
import org.springframework.core.type.AnnotationMetadata

internal class GatewayConfigImportSelector : DeferredImportSelector {

    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> =
        getValues(importingClassMetadata).map { group -> group.configClass.name }.toTypedArray()

    private fun getValues(metadata: AnnotationMetadata): List<GatewayConfigGroup> {
        val attributes =
            metadata.getAnnotationAttributes(EnableGatewayConfig::class.java.name)
                ?: return emptyList()

        val values = attributes["value"] as? Array<*> ?: return emptyList()
        return values.filterIsInstance<GatewayConfigGroup>()
    }
}
