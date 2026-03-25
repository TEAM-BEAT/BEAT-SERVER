# batch module

> 이 문서는 batch root dependency removal 이후 `batch` 모듈의 현재 detached bootstrap 계약과 남아 있는 transitional debt를 설명한다. `batch`는 이제 root project 의존 없이 자체 classpath로 build/boot/test 되어야 한다.

## 역할

- 스케줄러, 배치 잡, 유지보수 작업의 유일한 실행 진입점이다.
- 대량 처리, 정기 작업, 비동기 유지보수 흐름을 소유한다.
- 팀 컨벤션상 `Job/Runner -> Facade -> Application Service -> Domain` 흐름을 따른다.
- 외부 발송/저장 구현은 `infra`를 통해 사용한다.

## 허용 의존성

- `module-contracts`
- `domain`
- `infra`
- `global-utils`
- `observability`

## 금지 규칙

- `project(":")` 직접 의존 금지
- `apis`, `admin` 직접 의존 금지
- `gateway` 직접 의존 금지
- 사용자용 Controller/API DTO 보유 금지
- 전역 스캔에 기대는 구조 금지
- 스케줄 소유권을 다른 실행 모듈과 중복 활성화 금지

## Current bootstrap shape

```text
batch/
  src/main/kotlin/com/beat/batch/
    BatchApplication.kt
    config/
      BatchSchedulerBootstrapConfig.kt
      InfraConfig.kt                 # @EnableInfraBaseConfig(JPA, QUERY_DSL, ASYNC)
  src/main/java/com/beat/
    global/common/scheduler/application/
    domain/**/application/          # batch-owned scheduled service 포함
  src/main/resources/
    application.yml                 # beat.scheduler.owner=true
  src/test/resources/
    application-test.yml            # beat.scheduler.owner=false
```

### Runtime contract

- `BatchApplication`은 정확히 아래 bootstrap surface만 import한다.
    - `BatchSchedulerBootstrapConfig`
    - `InfraConfig`
    - `ObservabilityModuleConfig`
- `@SpringBootApplication`이 batch package만 스캔한다.
- `batch`는 실행 모듈 중 유일하게 `@EnableScheduling`을 유지한다.
- `BatchSchedulerBootstrapConfig`가 아래 batch-owned runtime beans를 명시적으로 import한다.
    - `JobSchedulerService`
    - `JobSchedulerTransactionalService`
    - `TicketCleanupScheduler`
    - `PromotionSchedulerService`
- main resources는 `beat.scheduler.owner=true`를 기본값으로 유지한다.
- external-client / Feign runtime은 batch bootstrap이 아니라 `infra`의 `EXTERNAL_CLIENTS` 경계와 web-app lane에서만 소유한다.
- test profile은 `beat.scheduler.owner=false`로 내려서 detached smoke boot를 검증하고, owner-enabled contract는 별도 테스트로 검증한다.
- scheduler/service 코드는 `batch` 모듈로 이동했지만 패키지 네임스페이스는 아직 legacy 경로를 유지한다.

## What changed in this issue

- `batch/build.gradle.kts`에서 `implementation(project(":"))`를 제거했다.
- `batch`는 root project classpath 없이 build/boot/test 되는 방향으로 고정됐다.
- 테스트 계약을 갱신해 detached bootstrap, non-owner smoke profile, owner-enabled schedule contract를 고정했다.
- batch architecture guard를 추가해 root dependency 재도입과 forbidden runtime lane 참조를 막는다.
- `batch/README.md`를 detached bootstrap 상태 기준으로 갱신했다.

## Current ownership notes

### In `batch`

- scheduler runtime ownership
- `ScheduleJobPort` implementation for the active scheduler lane
- scheduled maintenance jobs and batch execution entrypoints
- batch-local scheduling bootstrap and owner flag defaults

### Outside `batch`

- `module-contracts`: `ScheduleJobPort` 같은 cross-module execution contract
- `domain`: schedule/booking/promotion domain models and repository contracts
- `infra`: JPA, QueryDSL, async/task-scheduler bootstrap
- `global-utils`: shared common types and exception base contracts
- `observability`: logging / metrics / tracing boundary

## Remaining transitional debt

- scheduler/service 소스 상당수가 아직 `com.beat.domain.*`, `com.beat.global.*` legacy package names를 유지한다.
- batch 실행 경계는 detached 되었지만 package normalization은 아직 시작 전이다.
- 일부 legacy domain entity가 여전히 Spring Security 타입(`GrantedAuthority`)을 참조해 `domain` 모듈에 transitional runtime support가 남아 있다.
- root executable lane은 retire되었고, scheduler runtime owner는 `batch`로 고정됐다.
- CQRS/service layer normalization과 batch 전용 package 정리는 다음 단계에서 다룬다.

## Guard rails

- `BatchApplicationTest`
    - `BatchApplication` import 집합 고정
    - narrow app bootstrap 유지
    - scheduler bootstrap import 집합 고정
    - main/test resource owner flag 계약 고정
- `BatchArchitectureGuardTest`
    - `batch/build.gradle.kts`의 root dependency 재추가 금지
    - `apis`, `admin`, `gateway` 직접 의존 금지
    - root bootstrap lane 참조 금지
- `BatchModuleContextBootTest`
    - module context boot smoke test
    - test profile에서 `beat.scheduler.owner=false`가 실제로 적용되는지 확인
    - batch-owned `ScheduleJobPort`가 detached classpath에서 그대로 올라오는지 확인
- `BatchSchedulerOwnerBootTest`
    - owner-enabled runtime에서 `ScheduleJobPort -> JobSchedulerService` 해석 고정
- `AbstractBatchIntegrationTest`
    - batch 통합 테스트용 MySQL service connection bootstrap 공유
- `JobSchedulerServiceTest`
    - reconcile / registerOrRefresh owner behavior 회귀 방지

## To-Be direction

```text
com.beat.batch.<context>/
  job/
  facade/
  application/
    service/
      command/
      query/   # 필요할 때만
    dto/
    exception/
  config/
```

## 서비스 / CQRS 규칙

- `Facade`는 하나로 유지하고 배치 실행 시나리오 조합을 맡는다.
- 실제 잡 실행은 대부분 command 성격이므로 `application/service/command`를 기본으로 둔다.
- 배치 조회/리포트/통계가 필요할 때만 `application/service/query`를 추가한다.
- DTO는 command/query로 나누지 않고 `application/dto` 아래에서 관리한다.
- 배치 애플리케이션 문맥의 에러 코드와 예외는 `application/exception`에 둔다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- Repository는 지금 즉시 분리하지 않고, 복잡한 조회 전용 구현이 필요할 때만 infra query 레이어를 추가한다.

## Follow-up after this issue

1. batch-owned legacy package를 `com.beat.batch.<context>` 구조로 점진 이관
2. scheduler-related transitional docs를 batch ownership 기준으로 더 축소
3. shared test bootstrap convergence가 필요해지면 실행 모듈 간 중복 test container setup 정리
