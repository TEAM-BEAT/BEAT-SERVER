# apis module

> 이 문서는 issue #360 반영 후 `apis` 모듈의 현재 bootstrap 계약과 남아 있는 transitional debt를 설명한다. `apis`는 사용자 API 실행 모듈이며, 이제 root
> project 의존 없이 자체 classpath로 build/boot/test 되어야 한다.

## 역할

- 사용자 대상 HTTP API의 유일한 실행 진입점이다.
- 사용자용 Request/Response DTO, Controller, Swagger 노출을 소유한다.
- 팀 컨벤션상 `Controller -> Facade -> Application Service -> Domain` 흐름을 따른다.
- 비즈니스 규칙은 `domain` 계약에 위임하고, 구현 기술과 외부 연동은 `infra` 및 명시적 module bootstrap 경계를 통해 사용한다.
- 인증/인가는 `gateway`의 공개 계약과 bootstrap 경계를 통해 연결된다.

## 허용 의존성

- `module-contracts`
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
- root legacy bootstrap lane import 금지
    - `com.beat.BeatApplication`
    - `com.beat.legacyroot.*`
    - `com.beat.global.common.scheduler.application.*`
    - root `SecurityConfig` / `WebConfig`
- broad component scan에 기대는 구조 금지
- JPA Entity, QueryDSL Q type, Redis document를 API DTO로 직접 노출 금지

## Current bootstrap shape

```text
apis/
  src/main/kotlin/com/beat/apis/
    ApisApplication.kt
    config/
      ApisBootstrapConfig.kt        # targeted component scan for apis-owned legacy packages
      ApisScheduleJobPortConfig.kt  # module-local non-owner ScheduleJobPort bridge
      InfraConfig.kt                # @EnableInfraBaseConfig(JPA, QUERY_DSL, REDIS, ASYNC, EXTERNAL_CLIENTS)

  src/main/java/com/beat/apis/
    config/
      ApisSecurityConfig.java       # apis-owned HTTP security policy
    common/handler/
      GlobalExceptionHandler.java

  src/main/java/com/beat/domain/**  # apis-owned legacy package names
  src/main/java/com/beat/global/**  # apis-owned legacy package names
```

### Runtime contract

- `ApisApplication`은 정확히 아래 bootstrap surface만 import한다.
    - `ApisBootstrapConfig`
    - `GatewayModuleConfig`
    - `InfraConfig`
    - `ObservabilityModuleConfig`
- app-level broad `@ComponentScan`은 없다.
- `ApisBootstrapConfig`는 필요한 apis-owned legacy package만 targeted scan한다.
- `ApisSecurityConfig`가 route whitelist와 인증 정책을 소유한다.
- `ApisScheduleJobPortConfig`는 `beat.scheduler.owner=false`일 때만 non-owner `ScheduleJobPort`를 제공한다.
- active scheduler runtime owner는 `batch`이며, `apis`는 owner bean을 import/scan하지 않는다.

## What changed in issue #360

- `apis/build.gradle.kts`에서 `implementation(project(":"))`를 제거했다.
- `apis`는 root project classpath 없이 build/boot/test 되는 방향으로 고정됐다.
- scheduler handoff 이후에도 `ApisScheduleJobPortConfig`는 module-local non-owner bridge로 유지된다.
- 테스트 계약을 갱신해 root dependency 재도입, root bootstrap import, root scheduler owner 재연결을 막는다.

## Current ownership notes

### In `apis`

- user-facing controller / application service / DTO
- module-local security policy
- swagger exposure
- global exception handling for the HTTP lane
- notification/file API entrypoints that still carry legacy package names but live in the `apis` module source tree

### Outside `apis`

- `gateway`: JWT, auth filter, current-member resolver, refresh-token redis repository, auth/security shared primitives
- `infra`: JPA, QueryDSL, Redis connection/template, async/external-client bootstrap
- `domain`: repository/domain/exception/port contracts used by `apis`
- `global-utils`: shared response DTO and common exception hierarchy
- `observability`: logging/metrics/tracing aspects
- `batch`: active scheduler runtime ownership

## Remaining transitional debt

- 소스 파일 상당수가 아직 `com.beat.domain.*`, `com.beat.global.*` legacy package names를 유지한다.
- `ApisBootstrapConfig`는 여전히 필요하다.
    - 이유: package normalization이 아직 끝나지 않아 기본 module scan만으로는 기동 계약이 닫히지 않는다.
- root legacy lane은 repository에 남아 있지만, `apis`는 더 이상 build-time dependency로 기대지 않는다.
- `admin`의 root dependency 제거는 별도 이슈 범위다.
- `com.beat.apis.<context>` 구조로의 전면 정리는 후속 migration lane에서 처리한다.

## Guard rails

- `ApisApplicationTest`
    - `ApisApplication` import 집합 고정
    - broad app scan 금지
    - targeted bootstrap scan 계약 고정
    - non-owner schedule bridge 계약 고정
- `ApisArchitectureGuardTest`
    - `apis/build.gradle.kts`의 root dependency 재추가 금지
    - root bootstrap lane import 금지
- `ApisModuleContextBootTest`
    - module context boot smoke test
    - `ScheduleJobPort`가 module-local non-owner bean으로 올라오는지 확인

## To-Be direction

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

### Directional rules

- `Facade`는 API 시나리오 조합과 최종 응답 반환을 맡는다.
- CQRS는 `application/service`에서 먼저 적용하고 `service/command`, `service/query`로 나눈다.
- DTO는 command/query로 나누지 않고 `dto/request`, `dto/response` 중심으로 유지한다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- package normalization은 이 issue 범위 밖이지만, 신규 추가 코드는 가능하면 `com.beat.apis.*` 아래로 모은다.

## Follow-up after this issue

1. `ApisBootstrapConfig`를 없앨 수 있을 정도로 package normalization이 충분히 진행됐는지 검토
2. `admin` root dependency 제거 lane 진행
3. root legacy lane retirement 시 bootstrap/documentation 최종 축소
