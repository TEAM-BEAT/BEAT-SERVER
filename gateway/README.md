# gateway module

> 이 문서는 `gateway` 모듈의 현재 보안/JWT/bootstrap surface, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `gateway`는 인증/인가/JWT와 공통 보안 부품을 소유하되, 실행 모듈의 비즈니스 정책까지 가져가면 안 된다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Gateway module exposes `@EnableGatewayConfig`, `GatewayConfigGroup`, compatibility `GatewayModuleConfig`, and `gateway.security.servlet.CurrentMember` as the executable-facing public surface. JWT token ports live in `module-contracts`, while provider/store/filter/config implementations are under gateway internal packages and selected through the gateway import selector. | Stable public/internal surface where executable modules select only required gateway groups and depend only on gateway public config APIs, `CurrentMember`, and module-contracts auth ports. | Further JWT/security Kotlin conversion is separate from this package-boundary closeout. |

## 역할

- JWT 생성/검증, 인증 principal 변환 같은 인증 primitive를 소유한다.
- 공통 보안 부품, 인증 실패/인가 실패 처리, 보안 관련 MDC 확장을 소유한다.
- 실행 모듈이 보안 내부 구현을 모르도록 공개 계약과 enable 경계를 제공한다.

## 허용 의존성

- `global-utils`
- `observability` (허용하되 현재 미사용)

## 금지 규칙

- `domain`, `infra`, `apis`, `admin`, `batch` 의존 금지
- 비즈니스 규칙, Repository, 외부 API adapter 보유 금지
- 앱 모듈이 `com.beat.gateway.*.internal.*`, legacy `gateway.annotation.*`, legacy `gateway.config.*`, 또는 JWT store 구현체를 직접 import하게 만들지 말 것

## As-Is 패키지 구조

```text
gateway/
  src/main/kotlin/com/beat/gateway/
    GatewayModuleConfig.kt                  # compatibility public bootstrap facade; selector-backed

  src/main/java/com/beat/gateway/
    EnableGatewayConfig.java                # public selector annotation
    GatewayConfigGroup.java                 # public opt-in config groups
    GatewayConfigImportSelector.java        # DeferredImportSelector — group → internal config
    security/servlet/
      CurrentMember.java                    # public controller argument annotation
    internal/config/
      GatewayJwtConfig.java
      GatewayRedisConfig.java
      GatewayRefreshTokenConfig.java
      GatewaySecurityServletConfig.java
      GatewayServletSecurityConfig.java
      GatewayWebMvcConfig.java
    jwt/internal/
      JwtTokenProvider.java                 # implements module-contracts JwtTokenPort
      RefreshTokenService.java              # implements module-contracts RefreshTokenPort
      store/
        RefreshToken.java
        RefreshTokenRepository.java
    security/internal/servlet/
      AdminAuthentication.java
      CurrentMemberArgumentResolver.java
      CustomAccessDeniedHandler.java
      CustomJwtAuthenticationEntryPoint.java
      JwtAuthenticationFilter.java
      MemberAuthentication.java
```

설명:
- 실행 모듈의 gateway 직접 import 허용 표면은 `EnableGatewayConfig`, `GatewayConfigGroup`, `gateway.security.servlet.CurrentMember`로 제한한다. `GatewayModuleConfig`는 호환용 all-in-one facade로 유지한다.
- JWT token 생성/refresh token 저장 계약은 `module-contracts`의 `JwtTokenPort` / `RefreshTokenPort`가 공개 contract로 담당한다.
- `EnableGatewayConfig`는 broad `com.beat.gateway` component scan을 사용하지 않고 `GatewayConfigGroup`별 internal config를 selector로 import한다.
- provider, resolver, filter, security handler, Redis store/config는 gateway 내부 구현 패키지로 둔다.

## To-Be 패키지 구조

```text
com.beat.gateway.EnableGatewayConfig
com.beat.gateway.GatewayConfigGroup
com.beat.gateway.GatewayModuleConfig
com.beat.gateway.security.servlet.CurrentMember
com.beat.gateway.internal.config
com.beat.gateway.jwt.internal
com.beat.gateway.jwt.internal.store
com.beat.gateway.security.internal.servlet
```

설명:
- public: `EnableGatewayConfig`, `GatewayConfigGroup`, compatibility `GatewayModuleConfig`, `security.servlet.CurrentMember`
- contract: JWT/refresh token port는 `module-contracts`의 `com.beat.contracts.auth.*`를 사용한다.
- internal: config group, provider, impl, Redis store, servlet filter/resolver/handler

## 최종 목표

- `apis`는 `EnableGatewayConfig(SERVLET_SECURITY, REFRESH_TOKEN_STORE)`, `security.servlet.CurrentMember`, 그리고 `module-contracts` auth port만 참조한다.
- `admin`은 `EnableGatewayConfig(SERVLET_SECURITY)`와 `security.servlet.CurrentMember`만 참조하며 refresh token store를 가져가지 않는다.
- `batch`는 기본적으로 `gateway`에 의존하지 않는다.
- route whitelist, 역할 기반 라우팅 정책 같은 entrypoint 정책은 각 실행 모듈이 소유한다.
- `gateway`는 reusable security primitive와 shared adapter까지만 남는다.
