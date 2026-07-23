package com.beat.admin.config

import com.beat.domain.promotion.service.PromotionCarouselDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class DomainServiceConfig {
    @Bean
    fun promotionCarouselDomainService(): PromotionCarouselDomainService = PromotionCarouselDomainService()
}
