package com.beat.application.system

import com.beat.domain.promotion.service.PromotionCarouselDomainService
import com.beat.domain.promotion.service.PromotionEligibilityDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = [SystemApplicationConfig::class])
class SystemApplicationConfig {
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()

    @Bean
    fun promotionCarouselDomainService(): PromotionCarouselDomainService = PromotionCarouselDomainService()

    @Bean
    fun promotionEligibilityDomainService(): PromotionEligibilityDomainService = PromotionEligibilityDomainService()
}
