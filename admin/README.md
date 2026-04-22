# admin module

> 이 문서는 `admin` 모듈의 현재 bootstrap 계약, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `admin`은 root project 의존 없이 자체 classpath로 build/boot/test 되어야 한다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Detached admin executable lane with `AdminApplication`, admin-owned HTTP security/CORS/converter policy, admin-facing DTOs/Swagger, and explicit gateway/infra/observability bootstrap imports. | Admin-owned internal package and service organization that keeps backoffice policy separate from user API policy. | Adapter/facade/port/application cleanup and executable package/CQRS rules -> #382. |

## 역할

- 관리자/백오피스 HTTP API의 유일한 실행 진입점이다.
- 사용자 API와 다른 승인 정책, 응답 스펙, 운영 워크플로를 소유한다.
- 관리자용 Swagger/OpenAPI 노출도 executable-module owner concern으로 소유한다.
- 팀 컨벤션상 `Controller -> Facade -> Application Service -> Domain` 흐름을 따른다.
- `apis` 서비스를 재사용하지 않고, `domain`과 공유 모듈 계약만 사용한다.

## 허용 의존성

- `module-contracts`
- `domain`
- `infra`
- `global-utils`
- `observability`
- `gateway`의 공개 계약/enable 경계만

## 금지 규칙

- `project(":")` 직접 의존 금지
- `apis`, `batch` 직접 의존 금지
- `infra.external.*`, `infra.*.entity`, `infra.*.repository.impl` 직접 import 금지
- `gateway.security.*`, `gateway.filter.*`, `gateway.config.*` 직접 import 금지
- root legacy bootstrap lane import 금지
    - `com.beat.BeatApplication`
    - `com.beat.legacyroot.*`
    - root `SecurityConfig` / `WebConfig`
- `batch` runtime owner package 직접 import 금지
    - `com.beat.batch.*`
- 전역 스캔에 기대는 구조 금지
- JPA Entity, QueryDSL Q type, Redis document를 admin API DTO로 직접 노출 금지

## Current bootstrap shape

```text
admin/
  src/main/kotlin/com/beat/admin/
    AdminApplication.kt
    config/
      InfraConfig.kt                 # @EnableInfraBaseConfig(JPA, QUERY_DSL, EXTERNAL_CLIENTS)

  src/main/java/com/beat/admin/
    config/
      AdminSecurityConfig.java       # admin-owned HTTP security policy
      AdminCorsConfig.java
      AdminWebConverterConfig.java
    adapter/in/api/
    facade/
    application/
    handler/
    port/in/
```

### Runtime contract

- `AdminApplication`은 정확히 아래 bootstrap surface만 import한다.
    - `GatewayModuleConfig`
    - `InfraConfig`
    - `ObservabilityModuleConfig`
- executable bootstrap resource는 module-local 값과 `spring.profiles.group`만 소유하고, persistence/redis/external/jwt/observability 설정은 각 concern-owned `application-*.yml`로 분리한다.
- app-level broad `@ComponentScan`은 없다.
- `AdminSecurityConfig`가 관리자 route whitelist와 인증 정책을 소유한다.
- admin Swagger/OpenAPI는 기본적으로 non-prod 에서만 노출한다.
- `GatewayModuleConfig`가 gateway 내부 구현 빈을 제공하지만, `admin`은 공개 진입점인 `GatewayModuleConfig`와 `gateway.annotation.CurrentMember`만 직접 참조한다.
- `InfraConfig.kt`가 필요한 infra base config group만 명시적으로 import한다.
- `admin`은 async/scheduler shared runtime bean을 직접 import하지 않는다.
- `admin` resources는 `beat.scheduler.owner=false`를 유지하고, scheduler owner bean이나 non-owner bridge를 따로 소유하지 않는다.

## Previous detached-bootstrap changes

- `admin/build.gradle.kts`에서 `implementation(project(":"))`를 제거했다.
- `admin`은 root project classpath 없이 build/boot/test 되는 방향으로 고정됐다.
- 테스트 계약과 architecture guard를 추가해 root dependency 재도입, root bootstrap import, gateway 내부 패키지 직접 참조를 막는다.
- `admin/README.md`는 detached bootstrap 상태 기준으로 유지한다.

## Current / Target / Deferred-to-issue clarity for #384

Issue #384는 README/CI gate baseline만 문서화한다. 아래 표는 현재 실행 가능한 `admin` 계약과 목표 방향을 분리하고, 실제 구조 변경은 후속 이슈로 미룬다.

| Area | Current in `admin` | Target direction | Deferred-to-issue |
| --- | --- | --- | --- |
| Executable lane ownership | 관리자 HTTP API lane은 root bootstrap 없이 `AdminApplication`과 module-local config로 실행된다. | 계속 `admin`이 admin-facing controller/DTO/security/OpenAPI와 운영 워크플로 entrypoint를 소유한다. | #384 gate baseline only |
| Shared module ownership | `domain`, `module-contracts`, `global-utils`, `observability`, `gateway`, `infra`의 현재 공개 계약을 사용한다. | shared module ownership/package closeout 이후 공개 계약만 더 좁게 사용한다. | #378 |
| CQRS/package normalization | `adapter`, `application`, `facade`, `port` 중심 Java package가 남아 있고 context별 계층이 균일하지 않다. | `com.beat.admin.<context>` 아래 controller/facade/application/service/dto 기준으로 정리한다. | #382 |
| Gateway boundary | `GatewayModuleConfig`와 `gateway.annotation.CurrentMember` 같은 공개 표면만 직접 참조한다. | gateway 내부 security/filter/config 패키지 직접 참조 없이 admin 인증 경계를 유지한다. | #379 |
| Domain/persistence boundary | admin API DTO는 JPA Entity/QueryDSL Q type/Redis document를 직접 노출하지 않는 guard를 유지하고, promotion repository access는 domain `PromotionRepository` contract를 주입받아 infra 구현으로 격리한다. 인접 `PerformanceRepository` 등은 아직 transitional domain persistence concern이다. | domain persistence 전략 정리 후에도 admin boundary는 transfer DTO와 domain contract 중심으로 유지한다. | #380 |
| Infra/query boundary | `InfraConfig`가 필요한 infra base config group만 명시적으로 import하고, `InfraPersistenceConfig`를 IDE static-analysis breadcrumb로 직접 import한다. Runtime persistence import는 여전히 `JpaConfig`가 보장하며 async/scheduler bean은 직접 소유하지 않는다. | QueryDSL/JDSL 전환과 scan 결정은 infra-owned boundary에서 정한다. | #381 |
| Async/scheduler handoff | `admin`은 scheduler owner도 non-owner schedule bridge owner도 아니다. | async/coroutine 도입 범위가 결정될 때까지 admin lane의 runtime boundary를 넓히지 않는다. | #383 |

## Current ownership notes

### In `admin`

- admin-facing controller / facade / application service / DTO
- module-owned security route policy
- admin-facing Swagger/OpenAPI exposure as an executable-module owner concern
- module-local bootstrap config such as `springdoc.*`, `cors.allowed-origins`, `app.server.url`, `beat.scheduler.owner`
- admin exception handling
- admin CORS / converter configuration

### Outside `admin`

- `gateway`: JWT, auth filter, current-member resolver, refresh-token storage boundary, 인증/인가 shared primitives
- `infra`: JPA, QueryDSL, async, external-client bootstrap
- `domain`: admin이 사용하는 domain model / repository / port contracts
- `module-contracts`: storage, auth, schedule 같은 cross-module port contracts
- `global-utils`: shared response DTO와 공통 예외 계층
- `observability`: logging / metrics / tracing boundary

## Remaining transitional debt

- owner namespace는 이미 `com.beat.admin.*`로 정렬됐고, 남은 debt는 Java 기반 패키지 세분화 스타일 정리 쪽에 가깝다.
- `adapter`, `port` 패키지명은 그대로 남아 있고 최종 package normalization은 끝나지 않았다.
- `gateway`와 `observability`는 아직 marker config + legacy implementation 혼합 상태라 공개 표면을 더 명확히 줄일 여지가 있다.
- root executable lane은 retire되었고, `admin`은 detached module bootstrap만 유지한다.

## Guard rails

- `AdminApplicationTest`
    - `AdminApplication` import 집합 고정
    - broad app scan 금지
    - scheduler owner disabled 계약 고정
    - admin-owned security policy 존재 확인
    - test profile이 blanket bean override 없이 유지되고, boot smoke test는 `@MockitoBean`으로 필요한 collaborator만 대체하는지 확인
- `AdminArchitectureGuardTest`
    - `admin/build.gradle.kts`의 root dependency 재추가 금지
    - root bootstrap lane import 금지
    - owner source package가 `com.beat.admin.*`에 머무르는지 확인
    - gateway 내부 패키지 직접 import 금지
    - infra implementation package 직접 import 금지
- `AdminModuleContextBootTest`
    - module context boot smoke test
    - scheduler owner / schedule bridge가 admin context에 섞이지 않는지 확인

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
- command service는 domain repository contract를 통해 저장/수정 흐름을 수행하고, query service는 admin-facing 조회/응답 조립을 맡는다. 아직 단순 조회라면 domain repository contract를 사용할 수 있지만, 복잡한 목록/통계 조회는 별도 query/read-model로 분리한다. query service가 infra persistence mapper를 직접 사용하지 않는다.
- 관리자 애플리케이션 문맥의 에러 코드와 예외는 `application/exception`에 둔다.
- 관리자 전용 정책이라도 순수 비즈니스 규칙이면 `domain`으로 올린다.
- `admin -> gateway`는 최종적으로 허용하되, **공통 인증/보안 경계 진입점** 으로만 제한한다.
- 즉 `GatewayModuleConfig` 또는 `jwt.contract` 같은 공개 표면만 사용하고, `gateway.security.*`, `gateway.filter.*`, `gateway.config.*`를 직접 참조하지 않는다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- Repository는 지금 즉시 command/query로 나누지 않는다. 조회 복잡도 증가나 jOOQ/Kotlin JDSL 도입 필요가 생기면 그때 infra `repository.query`에 query 전용 구현과 read projection을 분리한다. 이 read projection은 JPA entity와 domain model을 번역하는 mapper를 재사용하지 않는다.

## Follow-up after this issue

1. `com.beat.admin.<context>` 내부 하위 계층(`adapter`, `application`, `dto`, `port`) 정리를 문맥별로 계속 진행
2. `gateway`/`observability` 공개 표면을 더 좁힐 수 있는지 검토
3. shared documentation을 현재 detached bootstrap 기준으로 지속 정리
