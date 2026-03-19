# admin module

> 이 문서는 `admin` 모듈의 최종 To-Be 계약을 정의한다. 현재 구현 밀도는 낮지만, 최종적으로 `admin`은 관리자/백오피스 전용 실행 모듈이어야 한다.

## 역할

- 관리자/백오피스 HTTP API의 유일한 진입점이다.
- 사용자 API와 다른 승인 정책, 응답 스펙, 운영 워크플로를 소유한다.
- 팀 컨벤션상 `Controller -> Facade -> Application Service -> Domain` 흐름을 따른다.
- `apis` 서비스를 재사용하지 않고, `domain`만 공유한다.

## 허용 의존성

- `domain`
- `infra`
- `global-utils`
- `observability`
- `gateway` 공개 계약 또는 전용 enable 경계만

## 금지 규칙

- `project(":")` 직접 의존 금지
- `apis`, `batch` 직접 의존 금지
- `infra.external.*`, `infra.*.entity`, `infra.*.repository.impl` 직접 import 금지
- `gateway` 내부 패키지 직접 import 금지
- 전역 스캔에 기대는 구조 금지

## As-Is 패키지 구조

```text
admin/
  src/main/kotlin/com/beat/admin/
    AdminApplication.kt
    config/

  src/main/java/com/beat/admin/
    adapter/in/api/
    facade/
    application/
      dto/
    exception/
    port/in/
```

설명:

- 현재 `admin`은 `adapter/in/api`, `facade`, `application`, `port/in`이 함께 존재하는 전환기 구조다.
- 이미 `apis`와 독립된 관리자 lane은 생겼지만, 패키지 스타일은 아직 최종 통일 전이다.

## To-Be 패키지 구조

```text
com.beat.admin.<context>/
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

- `Facade`는 하나로 유지하고 관리자 시나리오 조합을 맡는다.
- CQRS는 `application/service`에서 먼저 적용하고 `service/command`, `service/query`로 나눈다.
- DTO는 command/query로 나누지 않고 `dto/request`, `dto/response`만 유지한다.
- 관리자 애플리케이션 문맥의 에러 코드와 예외는 `application/exception`에 둔다.
- 관리자 전용 정책이라도 순수 비즈니스 규칙이면 `domain`으로 올린다.
- `admin -> gateway`는 최종적으로 허용하되, **공통 인증/보안 경계 진입점** 으로만 제한한다.
- 즉 `GatewayModuleConfig` 또는 `jwt.contract` 같은 공개 표면만 사용하고, `gateway.security.*`, `gateway.filter.*`, `gateway.config.*`
  를 직접 참조하지 않는다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- Repository는 지금 즉시 command/query로 나누지 않는다. 조회 복잡도 증가나 jOOQ 도입 필요가 생기면 그때 query 전용 구현을 분리한다.

## 최종 목표

- `admin`이 관리자 기능을 독립적으로 소유한다.
- `apis` 변경 없이 관리자 기능을 추가할 수 있다.
- 관리자 인증/권한 검사는 `gateway`의 공개 계약/enable 경계를 통해 연결된다.
