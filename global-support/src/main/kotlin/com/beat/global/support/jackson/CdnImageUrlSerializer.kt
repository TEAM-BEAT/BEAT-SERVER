package com.beat.global.support.jackson

import com.beat.global.support.jackson.CdnImageUrlSerializer.Companion.initialize
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

/**
 * Jackson 3 serializer that prepends the configured CDN base to a bare image key.
 * Jackson instantiates this class reflectively (no Spring DI), so the CDN base
 * is supplied at boot time via [initialize] from infra.
 */
class CdnImageUrlSerializer : ValueSerializer<String>() {

    override fun serialize(value: String?, gen: JsonGenerator, ctxt: SerializationContext) {
        if (value == null) {
            gen.writeNull()
            return
        }
        if (value.isBlank() || isAbsoluteUrl(value) || cdnBase.isEmpty()) {
            gen.writeString(value)
            return
        }
        val normalized = if (value.startsWith("/")) value.substring(1) else value
        gen.writeString("$cdnBase/$normalized")
    }

    companion object {
        @Volatile
        private var cdnBase: String = ""

        private fun isAbsoluteUrl(value: String): Boolean =
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true)

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
