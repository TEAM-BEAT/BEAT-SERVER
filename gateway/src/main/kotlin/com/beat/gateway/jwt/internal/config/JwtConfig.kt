package com.beat.gateway.jwt.internal.config

import com.beat.gateway.jwt.internal.JwtProperties
import com.beat.gateway.jwt.internal.JwtSigningKeyHolder
import com.beat.gateway.jwt.internal.JwtTokenIssuer
import com.beat.gateway.jwt.internal.JwtTokenParser
import com.beat.gateway.jwt.internal.JwtTokenProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig {

    @Bean
    fun jwtSigningKeyHolder(jwtProperties: JwtProperties): JwtSigningKeyHolder =
        JwtSigningKeyHolder(jwtProperties)

    @Bean
    fun jwtTokenIssuer(jwtSigningKeyHolder: JwtSigningKeyHolder): JwtTokenIssuer =
        JwtTokenIssuer(jwtSigningKeyHolder)

    @Bean
    fun jwtTokenParser(jwtSigningKeyHolder: JwtSigningKeyHolder): JwtTokenParser =
        JwtTokenParser(jwtSigningKeyHolder)

    @Bean
    fun jwtTokenProvider(
        jwtProperties: JwtProperties,
        jwtTokenIssuer: JwtTokenIssuer,
        jwtTokenParser: JwtTokenParser,
    ): JwtTokenProvider = JwtTokenProvider(jwtProperties, jwtTokenIssuer, jwtTokenParser)
}
