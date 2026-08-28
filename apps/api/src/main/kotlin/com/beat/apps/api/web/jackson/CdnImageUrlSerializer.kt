package com.beat.apps.api.web.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

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
        @Volatile private var cdnBase: String = ""

        fun initialize(domain: String?) {
            cdnBase =
                when {
                    domain.isNullOrBlank() -> ""
                    domain.endsWith("/") -> domain.dropLast(1)
                    else -> domain
                }
        }

        private fun isAbsoluteUrl(value: String): Boolean =
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true)
    }
}
