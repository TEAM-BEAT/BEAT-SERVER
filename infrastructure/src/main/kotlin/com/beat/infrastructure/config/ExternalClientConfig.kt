package com.beat.infrastructure.config

import com.beat.infrastructure.InfraBaseConfig
import com.beat.infrastructure.external.cdn.ImageCacheAdapter
import com.beat.infrastructure.external.notification.slack.SlackBookingNotificationAdapter
import com.beat.infrastructure.external.notification.slack.SlackMemberNotificationAdapter
import com.beat.infrastructure.external.notification.slack.client.BookingSlackClient
import com.beat.infrastructure.external.notification.slack.client.MemberSlackClient
import com.beat.infrastructure.external.notification.sms.CoolSmsAdapter
import com.beat.infrastructure.external.social.kakao.KakaoSocialLoginAdapter
import com.beat.infrastructure.external.social.kakao.client.KakaoApiClient
import com.beat.infrastructure.external.social.kakao.client.KakaoAuthApiClient
import com.beat.infrastructure.external.storage.s3.S3FileStorageAdapter
import com.beat.infrastructure.external.storage.s3.S3InfraConfig
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(S3InfraConfig::class)
@EnableFeignClients(
    basePackageClasses =
        [
            KakaoApiClient::class,
            KakaoAuthApiClient::class,
            BookingSlackClient::class,
            MemberSlackClient::class,
        ]
)
@ComponentScan(
    basePackageClasses =
        [
            KakaoSocialLoginAdapter::class,
            SlackBookingNotificationAdapter::class,
            SlackMemberNotificationAdapter::class,
            S3FileStorageAdapter::class,
            CoolSmsAdapter::class,
            ImageCacheAdapter::class,
        ],
    excludeFilters =
        [
            ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = [S3InfraConfig::class],
            )
        ],
)
internal class ExternalClientConfig : InfraBaseConfig
