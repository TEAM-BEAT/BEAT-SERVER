package com.beat.support.security.jwt.internal.config

import com.beat.support.security.jwt.internal.JwtProperties
import com.beat.support.security.jwt.internal.JwtSigningKeyHolder
import com.beat.support.security.jwt.internal.JwtTokenIssuer
import com.beat.support.security.jwt.internal.JwtTokenParser
import com.beat.support.security.jwt.internal.JwtTokenProvider
import java.time.Clock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig {

    @Bean
    internal fun jwtSigningKeyHolder(jwtProperties: JwtProperties): JwtSigningKeyHolder =
        JwtSigningKeyHolder(jwtProperties)

    @Bean
    internal fun jwtTokenIssuer(jwtSigningKeyHolder: JwtSigningKeyHolder): JwtTokenIssuer =
        JwtTokenIssuer(jwtSigningKeyHolder, Clock.systemUTC())

    @Bean
    internal fun jwtTokenParser(jwtSigningKeyHolder: JwtSigningKeyHolder): JwtTokenParser =
        JwtTokenParser(jwtSigningKeyHolder)

    @Bean
    internal fun jwtTokenProvider(
        jwtProperties: JwtProperties,
        jwtTokenIssuer: JwtTokenIssuer,
        jwtTokenParser: JwtTokenParser,
    ): JwtTokenProvider = JwtTokenProvider(jwtProperties, jwtTokenIssuer, jwtTokenParser)
}
