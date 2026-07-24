package com.beat.batch.config

import com.beat.domain.promotion.service.PromotionCarouselDomainService
import com.beat.domain.promotion.service.PromotionEligibilityDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class DomainServiceConfig {
    @Bean
    fun promotionCarouselDomainService(): PromotionCarouselDomainService = PromotionCarouselDomainService()

    @Bean
    fun promotionEligibilityDomainService(): PromotionEligibilityDomainService = PromotionEligibilityDomainService()
}
