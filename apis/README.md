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
| Domain/persistence boundary | API DTO는 JPA Entity/QueryDSL Q type/Redis document를 직접 노출하지 않는 guard를 유지하고, user-facing promotion reads use the module-local `PromotionService`. | domain persistence 전략 정리 후에도 API boundary는 transfer DTO 중심으로 유지한다. | #380 |
| Infra/query boundary | `InfraConfig`가 JPA, QueryDSL, async, external-client group을 명시적으로 import하고, `InfraPersistenceConfig`를 IDE static-analysis breadcrumb로 직접 import한다. Runtime persistence import는 여전히 `JpaConfig`가 보장한다. | QueryDSL/JDSL 전환과 scan 결정은 infra-owned boundary에서 정한다. | #381 |
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

- `Facade`는 API 시나리오 조합과 최종 응답 반환을 맡는다. 단, raw Domain model을 받거나 반환하지 않는다.
- CQRS는 `application/service`에서 먼저 적용하고 `service/command`, `service/query`로 나눈다.
- DTO는 command/query로 나누지 않고 `dto/request`, `dto/response` 중심으로 유지한다. Facade 조합용 내부 결과가 필요할 때만 `dto/result`를 추가한다.
- command service는 상태 변경 유스케이스와 transaction 경계다. domain repository contract로 Domain model을 조회/변경/저장하고, 필요한 순수 정책은 DomainService/Entity/VO에 위임한다.
- query service는 user-facing 조회/응답 조립을 맡는다. 단순 domain 조회는 domain repository contract를 사용할 수 있지만, 화면/검색/정렬/통계/read-model 조회가 되면 domain repository를 키우지 않고 module-contracts read port + infra query adapter 또는 실행 모듈 내부 read-model 경계로 분리한다. infra adapter가 필요하면 실행 모듈 타입을 infra가 import하지 않고 module-contracts read contract를 먼저 둔다. 이때 infra persistence mapper를 직접 사용하지 않는다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- executable-lane owner file은 계속 `com.beat.apis.*` 아래에 둔다.


### Layer boundary standard

BEAT의 사용자 API lane은 확장성과 리팩터링 안정성을 위해 아래 호출 방향을 표준으로 둔다.

```text
Controller -> Facade -> ApplicationService(command/query) -> DomainService/Entity/RepositoryPort/ReadPort
```

- Controller는 HTTP adapter이며 `Facade`만 호출한다. Repository, DomainService, command/query service를 직접 호출하지 않는다.
- `Facade`는 API 시나리오의 공식 진입점이다. 여러 command/query service output을 조합하고 최종 response를 반환하지만, transaction을 열거나 repository/domain service를 직접 호출하지 않는다.
- `Facade`는 raw Domain model을 절대 받거나 반환하지 않는다. Facade 입력/출력은 request primitive, ResponseDTO, CommandResult/QueryResult 같은 실행 모듈 내부 전달 모델로 제한한다.
- `ApplicationService`는 command service와 query service를 의미한다. 이 계층만 유스케이스 내부에서 Domain model을 조회/변경/정책 판단에 사용할 수 있고, Domain model은 이 계층 밖으로 반환하지 않는다.
- command service는 상태 변경 흐름과 transaction 경계를 맡는다. repository 조회/저장, lock, event, external/module-contract command port 호출, DomainService/Entity 호출 순서를 책임진다.
- query service는 조회 흐름과 user-facing 응답 조립을 맡는다. 단일 조회 응답은 query service가 ResponseDTO로 닫고, 여러 query/command 결과를 묶는 경우에만 Facade가 최종 ResponseDTO를 합성한다.
- ApplicationService는 도메인 판단을 직접 if/계산으로 반복 구현하지 않는다. 주요 도메인 판단은 `domain.<context>.service`의 DomainService 또는 Entity/VO method에 위임한다.
- DomainService는 `apis`가 아니라 `domain` 모듈에 둔다. `apis`에는 `application/port/in` 같은 use-case port 패키지를 기본으로 만들지 않는다.
- 복잡한 화면 조회/read-model은 domain repository를 키우지 않고 `module-contracts` read port + infra query adapter 또는 infra adapter가 필요 없는 실행 모듈 query service 내부 read-model 경계로 분리한다.
- application/use-case 실패 사유는 `application/exception/<Context>ApplicationErrorCode`가 소유한다. repository lookup 실패, request/use-case input validation, actor/owner/permission 검증, external adapter 실패 번역은 domain ErrorCode로 표현하지 않는다.
- Controller response 성공 문구는 `api/response/<Context>SuccessCode`가 소유한다. `SuccessCode`를 domain에 새로 추가하지 않는다.
- `infra` adapter가 던진 adapter-local failure는 application service에서 API-facing ErrorCode로 번역한다. `infra`가 `apis` ErrorCode를 import하게 만들지 않는다.


### CQRS query/read-model rule

BEAT의 CQRS는 저장소/DB를 처음부터 물리적으로 둘로 나누는 것이 아니라, 실행 모듈의 ApplicationService를 command와 query로 분리하는 것부터 시작한다.

```text
Controller -> Facade -> command service  -> Domain Repository -> Domain model
Controller -> Facade -> query service    -> ReadPort/ReadModel or simple Domain Repository -> ResponseDTO
```

- command service는 Domain model 중심이다. Domain repository는 command와 aggregate lifecycle에 필요한 저장/수정/단순 조회 언어만 유지한다.
- query service는 ResponseDTO를 조립하지만 Domain model을 Facade로 반환하지 않는다.
- 단순 조회는 domain repository contract를 임시로 사용할 수 있다. 예: `findById`, `findAllByPerformanceId`, `exists...`처럼 Domain model이 실제로 필요한 조회.
- 화면/검색/목록/정렬/통계/N+1 회피/fetch 전용/projection 조회는 read-model로 분리한다.
- read-model은 save 대상이 아니며 Domain model도 API ResponseDTO도 아니다. query 결과를 담는 내부 조회 shape다.
- infra query adapter가 구현하고 실행 모듈 query service가 주입받아야 하는 조회 계약은 `module-contracts`의 `*ReadPort`, `*ReadModel`로 둔다. 검색 조건이 단순하면 port 메서드 파라미터로 직접 전달하고, 조건이 많아져 의미가 분명해질 때만 `*SearchCondition`을 추가한다.
- 특정 API query service 내부에서만 쓰는 조립 결과는 `apis.<context>.application.dto.result` 또는 query service private row/result로 둔다.
- query service는 JPA Entity, QueryDSL Q type, EntityManager, infra persistence mapper를 직접 사용하지 않는다.

### Response and domain exposure rule

- 단일 command/query 유스케이스의 ResponseDTO는 command/query service가 만든다.
- 여러 command/query service output을 조합하는 API scenario에서만 Facade가 최종 ResponseDTO를 만든다.
- Controller와 Facade에는 raw Domain model을 절대 올리지 않는다.
- ResponseDTO, RequestDTO, CommandResult, QueryResult는 Domain model을 필드로 담지 않는다.
- 실행 모듈 간 DTO/ApplicationService/Facade를 공유하지 않는다. 공유가 필요하면 `module-contracts`에 최소 계약을 새로 정의한다.


### ResponseDTO vs Result selection rule

BEAT의 기본값은 command/query service가 실행 모듈 전용 ResponseDTO를 반환하는 것이다. Result는 기본 계층이 아니라 Facade 조합이 필요한 순간에만 추가한다.

- service 하나가 endpoint 응답을 완성할 수 있으면 command/query service가 ResponseDTO를 반환한다.
- Facade가 받은 값을 그대로 반환하면 ResponseDTO가 맞다.
- Facade가 여러 command/query service output을 다시 섞고 재가공해 하나의 API response를 만들어야 하면 각 service는 CommandResult/QueryResult를 반환하고 Facade가 최종 ResponseDTO를 만든다.
- Result는 최종 client contract가 아니라 Facade 조합용 application output이다.
- Result도 raw Domain model, JPA Entity, infra projection row를 필드로 담지 않는다. primitive/JDK type, contract-local value, 실행 모듈 내부 value만 사용한다.
- 같은 service output을 여러 response shape로 재사용해야 하거나 API response 변경으로부터 application output을 보호하고 싶을 때 Result를 둔다.
- 단일 API response와 1:1인 단순 유스케이스에 Result를 만들지 않는다.

```text
단일 유스케이스:
Controller -> Facade -> QueryService -> ResponseDTO

복합 API scenario:
Controller -> Facade -> QueryService A -> QueryResult A
                     -> QueryService B -> QueryResult B
                     -> Final ResponseDTO
```

## Follow-up after this issue

1. `com.beat.apis.<context>` 내부 하위 계층(`controller`, `application/service`, `dto`)을 문맥별로 더 일관되게 정리
2. `ApisScheduleJobPortConfig` 같은 intentional bridge를 closeout 문서 기준으로 계속 최소화
3. package normalization 이후 문맥별 하위 계층 정리를 별도 리팩터링 lane으로 진행
