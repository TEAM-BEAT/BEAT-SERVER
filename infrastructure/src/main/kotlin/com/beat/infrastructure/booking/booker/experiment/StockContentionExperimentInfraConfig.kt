package com.beat.infrastructure.booking.booker.experiment

import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentEnabled
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration(proxyBeanMethods = false)
@Profile("dev & !prod")
@StockContentionExperimentEnabled
@ComponentScan(basePackageClasses = [StockContentionExperimentInfraConfig::class])
class StockContentionExperimentInfraConfig
