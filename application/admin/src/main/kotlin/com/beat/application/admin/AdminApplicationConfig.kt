package com.beat.application.admin

import com.beat.domain.promotion.service.PromotionCarouselDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = [AdminApplicationConfig::class])
class AdminApplicationConfig {
    @Bean
    fun promotionCarouselDomainService(): PromotionCarouselDomainService = PromotionCarouselDomainService()
}
