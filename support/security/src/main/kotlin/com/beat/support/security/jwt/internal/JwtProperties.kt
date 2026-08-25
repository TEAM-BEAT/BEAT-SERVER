package com.beat.support.security.jwt.internal

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    @field:NotBlank val secret: String,
    @field:Positive val accessTokenExpireTime: Long,
    @field:Positive val refreshTokenExpireTime: Long,
    @field:NotBlank val keyId: String,
)
