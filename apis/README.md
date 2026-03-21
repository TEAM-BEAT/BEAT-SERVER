# apis module

> 이 문서는 `apis` 모듈의 최종 To-Be 계약을 정의한다. 현재 BEAT는 전환기 상태이지만, `apis`는 최종적으로 사용자 API 전용 실행 모듈이 되어야 한다.

## 역할

- 사용자 대상 HTTP API의 유일한 진입점이다.
- 사용자용 Request/Response DTO, Controller, Swagger 명세를 소유한다.
- 팀 컨벤션상 `Controller -> Facade -> Application Service -> Domain` 흐름을 따른다.
- 비즈니스 규칙은 `domain`에 위임하고, 구현 기술과 외부 연동은 `infra`와 모듈 import 경계를 통해 사용한다.
- 인증/인가가 필요하면 `gateway`의 공개 계약 또는 전용 enable 경계만 사용한다.

## 허용 의존성

- `domain`
- `infra`
- `gateway`의 공개 계약/enable 경계만
- `global-utils`
- `observability`

## 금지 규칙

- `project(":")` 직접 의존 금지
- `admin`, `batch` 직접 의존 금지
- `infra.external.*`, `infra.*.entity`, `infra.*.repository.impl` 직접 import 금지
- `gateway.security.*`, `gateway.filter.*`, `gateway.config.*` 직접 import 금지
- 전역 스캔에 기대는 구조 금지
- JPA Entity, QueryDSL Q type, Redis document를 API DTO로 노출 금지

## As-Is 패키지 구조

```text
apis/
  src/main/kotlin/com/beat/apis/
    ApisApplication.kt
    config/
      InfraConfig.kt          # @EnableInfraBaseConfig(JPA, QUERY_DSL, REDIS, ASYNC)

  src/main/java/com/beat/domain/*/api/
  src/main/java/com/beat/global/external/s3/api/
```

설명:
- 현재 `apis` 모듈은 `com.beat.apis` 아래 앱 진입점만 새로 생겼다.
- `InfraConfig.kt`가 `@EnableInfraBaseConfig`로 필요한 infra 설정 그룹을 선택적으로 import한다.
- 실제 사용자 API 컨트롤러 다수는 아직 legacy 패키지(`com.beat.domain.*.api`, `com.beat.global.external.s3.api`)로 남아 있다.
- 전환기 동안 `implementation(project(":"))` root 의존이 남아 있다. 최종적으로 제거 대상이다.
- 즉 현재는 **실행 lane만 `apis`로 분리되고, 패키지 구조는 아직 legacy를 많이 유지한 상태**다.

## To-Be 패키지 구조

```text
com.beat.apis.<context>/
  controller/
  facade/
  application/
    service/
      command/
      query/
    dto/
      request/
      response/
    exception/
  config/
```

## 서비스 / CQRS 규칙

- `Facade`는 하나로 유지하고 API 시나리오 조합과 최종 응답 반환을 맡는다.
- CQRS는 `application/service`에서 먼저 적용하고 `service/command`, `service/query`로 나눈다.
- DTO는 command/query로 나누지 않고 `dto/request`, `dto/response`만 유지한다.
- 애플리케이션 문맥의 에러 코드와 예외는 `application/exception`에 둔다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- Repository는 지금 즉시 command/query로 나누지 않는다. 조회 복잡도 증가나 jOOQ 도입 필요가 생기면 그때 infra query 구현을 추가한다.

## 최종 목표

- `apis`가 사용자 API 유스케이스를 독립적으로 소유한다.
- 신규 v2 기능은 root legacy lane이 아니라 `apis` 아래에서 확장된다.
- `apis -> infra.external.*` 직접 참조가 0건이 된다.
- `apis -> gateway`는 공개 계약 또는 명시적 import 경계만 남는다.
