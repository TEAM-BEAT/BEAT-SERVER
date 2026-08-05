package com.beat.infra.external.cdn

import com.beat.global.support.jackson.CdnImageUrlSerializer
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class CdnImageUrlConfig {
    @field:Value("\${cloud.cdn.domain:}")
    private lateinit var cdnDomain: String

    @PostConstruct
    fun initSerializer() {
        CdnImageUrlSerializer.initialize(cdnDomain)
    }
}
