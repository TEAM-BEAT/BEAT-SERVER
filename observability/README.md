# observability module

> 이 문서는 `observability` 모듈의 현재 logging/metrics/tracing surface, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. 관측성은 모든 실행 모듈이 쓰지만, 어느 비즈니스 모듈의 일부가 되어서는 안 된다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Observability module owns marker config and shared logging resource config, while some AOP/common observability concerns may still reflect legacy package debt. | Logging, metrics, tracing, actuator configuration, and shared runtime observability conventions are owned by the observability module. | Legacy observability package ownership closeout -> #378. |

## 역할

- 공통 로깅, MDC, 메트릭, 트레이싱, actuator 설정을 소유한다.
- 인증 유무와 관계없이 일관된 관측 규칙을 제공한다.
- 실행 모듈이 필요한 관측성 설정을 명시적으로 import할 수 있도록 한다.
- shared logging resource config (`log4j2-spring.xml`) ownership is here, while the executable-module build logic selects the runtime logging backend.

## 허용 의존성

- `global-utils`
- Spring Actuator / Micrometer / logging 라이브러리

## 금지 규칙

- `apis`, `admin`, `batch`, `domain`, `infra`, `gateway`의 비즈니스 규칙 의존 금지
- Controller, UseCase, Repository 구현 보유 금지
- 보안/도메인 정책을 관측성 규칙에 섞지 말 것

## As-Is 패키지 구조

```text
observability/
  src/main/kotlin/com/beat/observability/
    ObservabilityModuleConfig.kt

legacy root:
  src/main/java/com/beat/global/common/aop/**
```

설명:
- 현재 `observability` 모듈은 marker config + shared resource ownership 단계이다.
- AOP와 일부 공통 관측성 관심사는 아직 legacy root 패키지에 남아 있다.
- executable-lane notification event listener는 package normalization closeout 이후 `apis` owner namespace(`com.beat.apis.external.notification.slack.event`)로 정렬됐다.

## To-Be 패키지 구조

```text
com.beat.observability.logging
com.beat.observability.metrics
com.beat.observability.tracing
com.beat.observability.config
```

## 최종 목표

- `ObservabilityModuleConfig`가 logging/metrics/tracing import를 모으는 진입점이 된다.
- 실행 모듈은 공통 build logic에서 동일한 logging backend를 사용하고, shared log pattern/config resource는 `observability`가 소유한다.
- 실행 모듈이 같은 trace/log/metric 규칙을 재사용한다.
- 관측성 코드가 비즈니스 계층을 침범하지 않는다.
