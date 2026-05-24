package com.beat.infra.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import com.beat.infra.InfraBaseConfig;
import com.beat.infra.external.auth.social.kakao.KakaoSocialLoginAdapter;
import com.beat.infra.external.auth.social.kakao.client.KakaoApiClient;
import com.beat.infra.external.auth.social.kakao.client.KakaoAuthApiClient;
import com.beat.infra.external.cdn.CdnImageUrlConfig;
import com.beat.infra.external.cdn.ImageCacheAdapter;
import com.beat.infra.external.notification.slack.SlackBookingNotificationAdapter;
import com.beat.infra.external.notification.slack.SlackMemberNotificationAdapter;
import com.beat.infra.external.notification.slack.client.BookingSlackClient;
import com.beat.infra.external.notification.slack.client.MemberSlackClient;
import com.beat.infra.external.sms.CoolSmsAdapter;
import com.beat.infra.external.storage.s3.S3FileStorageAdapter;
import com.beat.infra.external.storage.s3.S3InfraConfig;

@Configuration(proxyBeanMethods = false)
@Import({S3InfraConfig.class, CdnImageUrlConfig.class})
@EnableFeignClients(basePackageClasses = {
	KakaoApiClient.class,
	KakaoAuthApiClient.class,
	BookingSlackClient.class,
	MemberSlackClient.class
})
@ComponentScan(
	basePackageClasses = {
		KakaoSocialLoginAdapter.class,
		SlackBookingNotificationAdapter.class,
		SlackMemberNotificationAdapter.class,
		S3FileStorageAdapter.class,
		CoolSmsAdapter.class,
		ImageCacheAdapter.class
	},
	excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = S3InfraConfig.class)
)
public class ExternalClientConfig implements InfraBaseConfig {
}
