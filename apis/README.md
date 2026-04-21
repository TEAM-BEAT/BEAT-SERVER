# apis module

> 이 문서는 `apis` 모듈의 현재 bootstrap 계약, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `apis`는 사용자 API 실행 모듈이며, root
> project 의존 없이 자체 classpath로 build/boot/test 되어야 한다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Detached user API executable lane with `ApisApplication`, module-local HTTP security policy, user-facing controllers/DTOs/Swagger, and a non-owner `ApisScheduleJobPortConfig` bridge while `batch` owns scheduler runtime. | Stable user API lane with cleaner internal package and CQRS boundaries under `com.beat.apis.<context>`. | Internal CQRS/package rules and `ApisScheduleJobPortConfig` closeout -> #382. |

## 역할

- 사용자 대상 HTTP API의 유일한 실행 진입점이다.
- 사용자용 Request/Response DTO, Controller, user-facing Swagger/OpenAPI 노출을 소유한다.
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
    - root `SecurityConfig` / `WebConfig`
- `batch` runtime owner package 직접 import 금지
    - `com.beat.batch.*`
- broad component scan에 기대는 구조 금지
- JPA Entity, QueryDSL Q type, Redis document를 API DTO로 직접 노출 금지

## Current bootstrap shape

```text
apis/
  src/main/kotlin/com/beat/apis/
    ApisApplication.kt
    config/
      ApisScheduleJobPortConfig.kt  # module-local non-owner ScheduleJobPort bridge
      InfraConfig.kt                # @EnableInfraBaseConfig(JPA, QUERY_DSL, ASYNC, EXTERNAL_CLIENTS)

  src/main/java/com/beat/apis/
    booking/
    member/
    performance/
    promotion/
    schedule/
    user/
    external/
    swagger/
    config/
      ApisSecurityConfig.java       # apis-owned HTTP security policy
    common/handler/
      GlobalExceptionHandler.java
```

### Runtime contract

- `ApisApplication`은 정확히 아래 bootstrap surface만 import한다.
    - `GatewayModuleConfig`
    - `InfraConfig`
    - `ObservabilityModuleConfig`
- executable bootstrap resource는 module-local 값과 `spring.profiles.group`만 소유하고, persistence/redis/external/jwt/observability 설정은 각 concern-owned `application-*.yml`로 분리한다.
- app-level broad `@ComponentScan`은 없다.
- `@SpringBootApplication(scanBasePackageClasses = [ApisApplication::class])`가 `com.beat.apis.*` owner namespace만 스캔한다.
- `ApisSecurityConfig`가 route whitelist와 인증 정책을 소유한다.
- `ApisScheduleJobPortConfig`는 `beat.scheduler.owner=false`일 때만 non-owner `ScheduleJobPort`를 제공한다.
- active scheduler runtime owner는 `batch`이며, `apis`는 owner bean을 import/scan하지 않는다.

## What changed in issue #360

- `apis/build.gradle.kts`에서 `implementation(project(":"))`를 제거했다.
- `apis`는 root project classpath 없이 build/boot/test 되는 방향으로 고정됐다.
- scheduler handoff 이후에도 `ApisScheduleJobPortConfig`는 module-local non-owner bridge로 유지된다.
- 테스트 계약을 갱신해 root dependency 재도입, root bootstrap import, root scheduler owner 재연결을 막는다.

## Current / Target / Deferred-to-issue clarity for #384

Issue #384는 README/CI gate baseline만 문서화한다. 아래 표는 현재 실행 가능한 `apis` 계약과 목표 방향을 분리하고, 실제 구조 변경은 후속 이슈로 미룬다.

| Area | Current in `apis` | Target direction | Deferred-to-issue |
| --- | --- | --- | --- |
| Executable lane ownership | 사용자 API lane은 root bootstrap 없이 `ApisApplication`과 module-local config로 실행된다. | 계속 `apis`가 user-facing controller/DTO/security/OpenAPI를 소유한다. | #384 gate baseline only |
| Shared module ownership | `domain`, `module-contracts`, `global-utils`, `observability`, `gateway`, `infra`의 현재 공개 계약을 사용한다. | shared module ownership/package closeout 이후 공개 계약만 더 좁게 사용한다. | #378 |
| CQRS/package normalization | context별 `controller`, `api`, `application`, `dto` 세분화가 아직 균일하지 않다. | `com.beat.apis.<context>` 아래 controller/facade/application/service/dto 기준으로 정리한다. | #382 |
| Gateway boundary | `GatewayModuleConfig`와 공개 annotation/contract를 통해 인증 경계를 사용한다. | gateway 내부 패키지 직접 참조 없이 공개 표면만 유지한다. | #379 |
| Domain/persistence boundary | API DTO는 JPA Entity/QueryDSL Q type/Redis document를 직접 노출하지 않는 guard를 유지한다. | domain persistence 전략 정리 후에도 API boundary는 transfer DTO 중심으로 유지한다. | #380 |
| Infra/query boundary | `InfraConfig`가 JPA, QueryDSL, async, external-client group을 명시적으로 import한다. | QueryDSL/JDSL 전환과 scan 결정은 infra-owned boundary에서 정한다. | #381 |
| Async/scheduler handoff | `apis`는 scheduler owner가 아니며 non-owner `ScheduleJobPort` bridge만 가진다. | async/coroutine 도입 범위가 결정될 때까지 HTTP lane의 비동기 경계를 넓히지 않는다. | #383 |

## Current ownership notes

### In `apis`

- user-facing controller / application service / DTO
- module-local security policy
- user-facing Swagger/OpenAPI exposure as an executable-module owner concern
- module-local bootstrap config such as `springdoc.*`, `cors.allowed-origins`, `app.server.url`, `beat.scheduler.owner`
- global exception handling for the HTTP lane
- notification/file API entrypoints도 `com.beat.apis.*` owner namespace로 정렬됐다.

### Outside `apis`

- `gateway`: JWT, auth filter, current-member resolver, refresh-token storage boundary, auth/security shared primitives
- `infra`: JPA, QueryDSL, async/external-client bootstrap
- `domain`: repository/domain/exception/port contracts used by `apis`
- `global-utils`: shared response DTO and common exception hierarchy
- `observability`: logging/metrics/tracing aspects
- `batch`: active scheduler runtime ownership

## Remaining transitional debt

- owner namespace normalization은 끝났지만 `controller/application/dto` 내부 세분화는 문맥별로 완전히 통일되지 않았다.
- `ApisScheduleJobPortConfig`는 package normalization과 무관하게 non-owner schedule bridge로 남아 있다.
- root executable lane은 retire되었고, `apis`는 root bootstrap 없이 detached module contract를 유지한다.

## Guard rails

- `ApisApplicationTest`
    - `ApisApplication` import 집합 고정
    - broad app scan 금지
    - owner source package가 `com.beat.apis.*`로 정렬됐는지 확인
    - non-owner schedule bridge 계약 고정
    - test profile이 blanket bean override 없이 유지되는지 확인
- `ApisArchitectureGuardTest`
    - `apis/build.gradle.kts`의 root dependency 재추가 금지
    - root bootstrap lane import 금지
    - gateway internal / infra implementation package 직접 import 금지
- `ApisModuleContextBootTest`
    - module context boot smoke test
    - `ScheduleJobPort`가 module-local non-owner bean으로 올라오는지 확인
    - shared async import가 `TaskScheduler`를 함께 올리지 않는지 확인

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
- executable-lane owner file은 계속 `com.beat.apis.*` 아래에 둔다.

## Follow-up after this issue

1. `com.beat.apis.<context>` 내부 하위 계층(`controller`, `application/service`, `dto`)을 문맥별로 더 일관되게 정리
2. `ApisScheduleJobPortConfig` 같은 intentional bridge를 closeout 문서 기준으로 계속 최소화
3. package normalization 이후 문맥별 하위 계층 정리를 별도 리팩터링 lane으로 진행
