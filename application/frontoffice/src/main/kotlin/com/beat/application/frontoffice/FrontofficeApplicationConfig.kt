package com.beat.application.frontoffice

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = [FrontofficeApplicationConfig::class])
class FrontofficeApplicationConfig
