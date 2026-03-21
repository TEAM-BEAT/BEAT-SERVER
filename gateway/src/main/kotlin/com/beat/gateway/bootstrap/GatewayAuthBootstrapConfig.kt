package com.beat.gateway.bootstrap

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Transition bootstrap — gateway가 소유하는 auth/security bean surface.
 *
 * 현재 auth 코드는 아직 legacy root에 있지만, runtime bean 등록 ownership은
 * 이 config를 통해 gateway 경계로 옮긴다. apis/admin의 broad scan에서
 * auth 패키지를 exclude하고 이 config이 대신 가져온다.
 *
 * auth 코드가 gateway 모듈로 물리 이동하면 이 config은 제거된다.
 */
@Configuration(proxyBeanMethods = false)
@Import(GatewayAuthImportSelector::class)
class GatewayAuthBootstrapConfig
