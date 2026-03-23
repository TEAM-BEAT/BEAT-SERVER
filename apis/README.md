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
      ApisBootstrapConfig.kt  # targeted @ComponentScan + root scheduler exclusions
      InfraConfig.kt          # @EnableInfraBaseConfig(JPA, QUERY_DSL, REDIS, ASYNC, EXTERNAL_CLIENTS)

  src/main/java/com/beat/domain/*/
    api/
    application/
  src/main/java/com/beat/global/
    external/
    swagger/
```

설명:
- 현재 `apis` 모듈은 `com.beat.apis` 아래 앱 진입점과 bootstrap config를 소유한다.
- `InfraConfig.kt`가 `@EnableInfraBaseConfig`로 필요한 infra 설정 그룹을 선택적으로 import한다.
- `ApisApplication`의 raw broad `@ComponentScan`은 제거됐고, `ApisBootstrapConfig`가 필요한 legacy 패키지만 targeted scan한다.
- 실제 사용자 API 컨트롤러와 애플리케이션 서비스 다수는 아직 legacy 패키지(`com.beat.domain.*`, `com.beat.global.*`)를 유지한다.
- root의 `TicketCleanupScheduler`, `PromotionSchedulerService`는 아직 같은 패키지 네임스페이스에 남아 있으므로 `ApisBootstrapConfig`에서 명시적으로 exclude한다.
- 전환기 동안 `implementation(project(":"))` root 의존이 남아 있다. 최종적으로 제거 대상이다.
- 즉 현재는 **실행 lane과 bootstrap ownership은 `apis`로 분리됐지만, 패키지 구조와 일부 scheduler 관련 root coupling은 아직 남아 있는 상태**다.

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

---

## Root Coupling Surface

> 현재 `:apis`는 broad scan에 기대지 않지만, 전환기 동안 여전히 `project(":")`에 build-time으로 의존한다.
> 이 의존은 Step 8 이후 남은 root scheduler leftovers와 legacy root baseline 때문에 유지된다.

### Decision Memo — Why `ApisBootstrapConfig` still exists

- 지금 `ApisBootstrapConfig`는 단순한 임시 편의 레이어가 아니라, 현재 `apis` runtime을 성립시키는 **명시적 브리지**다.
- 장기적으로는 `com.beat.apis.*` 아래로 패키지를 정리하는 편이 더 낫지만, 현재는 패키지 이관만으로 `ApisBootstrapConfig`를 제거할 수 없다.

#### 지금 유지해야 하는 이유

- `apis` 내부 서비스 중 일부는 아직 root scheduler bridge에 의존한다.
- 예를 들어 `PerformanceManagementService`는 `ScheduleJobPort`를 주입받고, 현재 그 구현 bean은 root의 `JobSchedulerService`다.
- 그래서 `ApisBootstrapConfig`는 legacy package scan을 최소 범위로 통제하면서도, 필요한 root scheduler bridge만 명시적으로 연결한다.

#### 언제 제거 가능한가

- `ApisBootstrapConfig`는 아래 조건이 충족되면 제거 대상이다.
1. `ScheduleJobPort` 구현이 root가 아니라 `batch` 또는 다른 명시적 모듈 경계로 이동
2. `TicketCleanupScheduler`, `PromotionSchedulerService`가 root package overlap 없이 정리
3. `apis` 소유 bean이 `com.beat.apis.*` 아래로 충분히 이관되어 기본 module scan만으로 기동 가능

#### 제거하려면 필요한 선행조건

1. scheduler ownership migration 완료
2. root scheduler bridge 제거 또는 대체 경계 확정
3. `apis/build.gradle.kts`의 `implementation(project(":"))` 제거 가능 상태 검증

즉, 현재 결론은 다음과 같다.
- **최종 방향**: `com.beat.apis.*` 아래로 패키지 이관
- **현재 판단**: `ApisBootstrapConfig` 유지
- **이유**: 지금은 legacy package 문제만이 아니라 root scheduler bridge 문제도 함께 걸려 있기 때문

### 1. Active unresolved coupling

#### 1-1. Root scheduler leftovers

`ApisBootstrapConfig`는 targeted scan을 사용하지만, 아래 root scheduler 클래스가 아직 같은 패키지 네임스페이스를 공유한다.
이 중 `JobSchedulerService` 계열은 현재 `ScheduleJobPort`를 통해 `apis` runtime이 실제로 사용하고 있고,
`TicketCleanupScheduler`, `PromotionSchedulerService`는 명시적 exclusion으로 막고 있다.

| Bean | 현재 위치 | 현재 상태 | 후속 행선지 |
|------|----------|-----------|------------|
| `TicketCleanupScheduler` | `src/main/java/com/beat/domain/booking/application` | `ApisBootstrapConfig`에서 exclude | batch |
| `PromotionSchedulerService` | `src/main/java/com/beat/domain/promotion/application` | `ApisBootstrapConfig`에서 exclude | batch |
| `JobSchedulerService` | `src/main/java/com/beat/global/common/scheduler/application` | `ApisBootstrapConfig`를 통해 runtime 사용 중 | batch |
| `JobSchedulerTransactionalService` | `src/main/java/com/beat/global/common/scheduler/application` | `JobSchedulerService` 의존성으로 runtime 사용 중 | batch |

#### 1-2. Legacy root baseline

| 항목 | 현재 위치 | 현재 상태 |
|------|----------|-----------|
| root `BeatApplication` | `src/main/java/com/beat/BeatApplication.java` | legacy baseline 유지 |
| root `@EnableFeignClients` | `src/main/java/com/beat/BeatApplication.java` | root 전용 transitional 유지 |
| `LegacyRootSecurityConfig` | `src/main/java/com/beat/legacyroot/config` | root lane 유지용 |

#### 1-3. Build-time root dependency

| 항목 | 현재 상태 | 왜 아직 남아 있나 |
|------|----------|------------------|
| `implementation(project(":"))` | `apis/build.gradle.kts`에 유지 | `ApisBootstrapConfig`와 테스트가 root scheduler bridge (`JobSchedulerService`, `TicketCleanupScheduler`, `PromotionSchedulerService`)를 아직 참조하고 있고, batch/root retirement를 이 브랜치에서 하지 않기 때문 |

### 2. Resolved on this branch or earlier steps

#### 2-1. Auth/Security — gateway bootstrap으로 이전 완료

`GatewayModuleConfig` → `GatewayAuthBootstrapConfig` → `GatewayAuthImportSelector` 경로로
아래 빈의 runtime 등록 ownership을 gateway 경계로 옮겼다.

| Bean | 원래 위치 | 상태 |
|------|----------|------|
| `JwtAuthenticationFilter` | `global.auth.jwt.filter` | gateway bootstrap |
| `JwtTokenProvider` | `global.auth.jwt.provider` | gateway bootstrap |
| `CurrentMemberArgumentResolver` | `global.auth.resolver` | gateway bootstrap |
| `CustomAccessDeniedHandler` | `global.auth.security` | gateway bootstrap |
| `CustomJwtAuthenticationEntryPoint` | `global.auth.security` | gateway bootstrap |
| `SecurityConfig` | `global.common.config` | gateway bootstrap |
| `WebConfig` | `global.common.config` | gateway bootstrap |

#### 2-2. `apis` application bootstrap — Step 8 완료

| 항목 | 이전 상태 | 현재 상태 |
|------|----------|-----------|
| `ApisApplication` scan | raw broad `@ComponentScan(basePackages = ["com.beat.domain", "com.beat.global"])` | `ApisBootstrapConfig` explicit import |
| Feign bootstrap | app module가 직접 보유 | 제거 완료, external bootstrap은 `infra` 소관 |
| scheduler overlap | broad scan에 묻혀 있었음 | 명시적 exclusion으로 드러남 |

#### 2-3. Application/service ownership — `apis` 모듈로 이관 완료

아래 애플리케이션 서비스는 더 이상 root broad scan으로 발견되는 대상이 아니라, `apis` 소스 트리 안의 실제 구현이다.

| 영역 | 현재 owner |
|------|-----------|
| member application services | apis |
| booking application services | apis |
| performance application services | apis |
| promotion application services | apis |
| schedule application services | apis |
| user application services | apis |

#### 2-4. Global/common ownership — 이전 완료

| 항목 | 현재 owner |
|------|-----------|
| `GlobalExceptionHandler` | apis |
| `SwaggerConfig` | apis |
| Slack/S3/SMS adapters | infra |
| `BookingCreatedEventListener` / `MemberRegisteredEventListener` | apis |
| logging/tx/controller aspects | observability |

### 3. Infra bootstrap — explicit import 경계는 있지만 아직 detach 완료는 아님

| 설정 | 현재 범위 | 문제 |
|------|----------|------|
| `JpaConfig` `@EntityScan` | `"com.beat"` 전체 | root entity 사라지면 실패 |
| `JpaConfig` `@EnableJpaRepositories` | `"com.beat"` 전체 | root repo 사라지면 실패 |
| `RedisConfig` `@EnableRedisRepositories` | `"com.beat.global.auth.jwt.dao"` | auth 패키지 직접 참조 |

### #360 에서 바로 해야 할 일

1. root scheduler leftovers를 `batch`로 이동하거나 네임스페이스 overlap을 제거
2. `JpaConfig`의 `@EntityScan`, `@EnableJpaRepositories` 범위를 모듈별로 축소
3. `RedisConfig`의 `@EnableRedisRepositories` basePackages를 gateway/auth ownership으로 이동
4. `batch/root` retirement 이후 `apis/build.gradle.kts`에서 `project(":")` 제거
5. 남아 있는 legacy package names를 점진적으로 `com.beat.apis.<context>` 구조로 정리
