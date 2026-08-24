package com.beat.apps.api.config

import com.beat.apps.api.web.converter.CaseInsensitiveStringToEnumConverterFactory
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
class WebConverterConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverterFactory(CaseInsensitiveStringToEnumConverterFactory())
    }
}
