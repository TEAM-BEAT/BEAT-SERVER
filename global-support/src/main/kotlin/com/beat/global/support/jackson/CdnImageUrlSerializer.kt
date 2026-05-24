package com.beat.global.support.jackson

import com.beat.global.support.jackson.CdnImageUrlSerializer.Companion.initialize
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * Jackson serializer that prepends the configured CDN base to a bare image key.
 * Jackson instantiates this class reflectively (no Spring DI), so the CDN base
 * is supplied at boot time via [initialize] from infra.
 */
class CdnImageUrlSerializer : JsonSerializer<String>() {

    override fun serialize(value: String?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }
        if (value.isBlank() || value.startsWith("http") || cdnBase.isEmpty()) {
            gen.writeString(value)
            return
        }
        val normalized = if (value.startsWith("/")) value.substring(1) else value
        gen.writeString("$cdnBase/$normalized")
    }

    companion object {
        @Volatile
        private var cdnBase: String = ""

        @JvmStatic
        fun initialize(domain: String?) {
            cdnBase = when {
                domain.isNullOrBlank() -> ""
                domain.endsWith("/") -> domain.dropLast(1)
                else -> domain
            }
        }
    }
}
