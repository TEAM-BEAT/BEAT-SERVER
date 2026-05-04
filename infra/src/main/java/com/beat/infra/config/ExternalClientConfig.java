package com.beat.infra.config;

import com.beat.infra.InfraBaseConfig;
import com.beat.infra.external.auth.social.kakao.KakaoSocialLoginAdapter;
import com.beat.infra.external.auth.social.kakao.client.KakaoApiClient;
import com.beat.infra.external.auth.social.kakao.client.KakaoAuthApiClient;
import com.beat.infra.external.notification.slack.SlackBookingNotificationAdapter;
import com.beat.infra.external.notification.slack.SlackMemberNotificationAdapter;
import com.beat.infra.external.notification.slack.client.BookingSlackClient;
import com.beat.infra.external.notification.slack.client.MemberSlackClient;
import com.beat.infra.external.sms.CoolSmsAdapter;
import com.beat.infra.external.storage.s3.S3FileStorageAdapter;
import com.beat.infra.external.storage.s3.S3InfraConfig;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(S3InfraConfig.class)
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
		CoolSmsAdapter.class
	},
	excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = S3InfraConfig.class)
)
public class ExternalClientConfig implements InfraBaseConfig {
}
