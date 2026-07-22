package com.beat.apis.web.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory

class CaseInsensitiveStringToEnumConverterFactory : ConverterFactory<String, Enum<*>> {
    override fun <T : Enum<*>> getConverter(targetType: Class<T>): Converter<String, T> =
        Converter { source ->
            targetType.enumConstants.firstOrNull { it.name.equals(source.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid value")
        }
}
