# gateway module

> 이 문서는 `gateway` 모듈의 현재 보안/JWT/bootstrap surface, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `gateway`는 인증/인가/JWT와 공통 보안 부품을 소유하되, 실행 모듈의 비즈니스 정책까지 가져가면 안 된다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Gateway module provides the current security/JWT/bootstrap surface through `GatewayModuleConfig`; executable modules still rely on a mix of public marker config and legacy implementation packages. | Tighter public/internal surface where executable modules depend only on explicit gateway contracts/enable boundaries. | `GatewayModuleConfig` scan narrowing and public/internal contract closeout -> #379. |

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
- 앱 모듈이 `gateway.security.*`, `gateway.filter.*`, `gateway.config.*`를 직접 import하게 만들지 말 것

## As-Is 패키지 구조

```text
gateway/
  src/main/kotlin/com/beat/gateway/
    GatewayModuleConfig.kt

legacy root:
  src/main/java/com/beat/global/auth/**
  src/main/java/com/beat/global/common/config/Security*
```

설명:
- 현재 `gateway` 모듈 자체는 marker config 수준이다.
- 실제 인증/인가/JWT/보안 코드는 아직 legacy root 패키지에 더 많이 남아 있다.

## To-Be 패키지 구조

```text
com.beat.gateway.jwt.contract
com.beat.gateway.jwt.internal
com.beat.gateway.security.servlet
```

설명:
- `jwt.contract`: 실행 모듈에 공개되는 인터페이스/enable 경계
- `jwt.internal`: provider, config, impl
- `security.servlet`: `SecurityFilterChain`, entrypoint, access denied handler, servlet filter

## 최종 목표

- `apis`는 `jwt.contract`만 참조한다.
- `admin`은 필요 시 `GatewayModuleConfig` 또는 `jwt.contract` 같은 공개 경계만 참조한다.
- `batch`는 기본적으로 `gateway`에 의존하지 않는다.
- route whitelist, 역할 기반 라우팅 정책 같은 entrypoint 정책은 각 실행 모듈이 소유한다.
- `gateway`는 reusable security primitive와 shared adapter까지만 남는다.
