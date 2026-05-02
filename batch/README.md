# batch module

> 이 문서는 `batch` 모듈의 현재 detached bootstrap 계약, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `batch`는 root project 의존 없이 자체 classpath로 build/boot/test 되어야 한다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Batch owns scheduler runtime after detach/root dependency removal; `BatchApplication` imports required infra support and observability while `beat.scheduler.owner=true` in runtime resources. Scheduled entrypoints now call a batch Facade before ApplicationService. | Cleaner application service command/query package and DTO organization for scheduler, maintenance, and batch flows. | Internal package/CQRS cleanup for executable modules -> #382. |

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
      InfraConfig.kt                 # @EnableInfraBaseConfig(JPA, QUERY_DSL, ASYNC)
  src/main/java/com/beat/batch/
    scheduler/job/
    scheduler/application/
    booking/job/
    booking/application/
    promotion/job/
    promotion/application/
  src/main/kotlin/com/beat/batch/
    scheduler/facade/
    booking/facade/
    promotion/facade/
  src/main/resources/
    application.yml                 # beat.scheduler.owner=true
  src/test/resources/
    application-test.yml            # beat.scheduler.owner=false
```

### Runtime contract

- `BatchApplication`은 정확히 아래 bootstrap surface만 import한다.
    - `InfraConfig`
    - `ObservabilityModuleConfig`
- `@SpringBootApplication`이 batch package만 스캔한다.
- `BatchApplication`은 Spring Boot `TaskSchedulingAutoConfiguration`을 사용하고, batch가 유일하게 `@EnableScheduling`을 켠다.
- `batch`는 실행 모듈 중 유일하게 `@EnableScheduling`을 유지한다.
- `batch`는 user-facing API는 소유하지 않지만, 운영용 health check를 위해 최소 HTTP/Actuator surface를 제공할 수 있다.
- batch-owned scheduler/runtime bean은 `com.beat.batch.<context>.job.*`, `com.beat.batch.<context>.facade.*`, `com.beat.batch.<context>.application.*` 아래에서 component scan으로 올라온다.
- executable bootstrap resource는 module-local 값과 `spring.profiles.group`만 소유하고, batch는 `persistence`, `observability`, `thread-pool` concern만 활성화한다.
- main resources는 `beat.scheduler.owner=true`를 기본값으로 유지한다.
- external-client / Feign runtime은 batch bootstrap이 아니라 `infra`의 `EXTERNAL_CLIENTS` 경계와 web-app lane에서만 소유한다.
- scheduler bean은 Spring Boot auto-configuration이 만들고, pool/thread 설정은 `spring.task.scheduling.*` profile property로 조정한다.
- test profile은 `beat.scheduler.owner=false`로 내려서 detached smoke boot를 검증하고, owner-enabled contract는 별도 테스트로 검증한다.
- scheduler/service 코드는 `batch` owner namespace 기준으로 정렬됐다.

## Previous detached-bootstrap changes

- `batch/build.gradle.kts`에서 `implementation(project(":"))`를 제거했다.
- `batch`는 root project classpath 없이 build/boot/test 되는 방향으로 고정됐다.
- 테스트 계약을 갱신해 detached bootstrap, non-owner smoke profile, owner-enabled schedule contract를 고정했다.
- batch architecture guard를 추가해 root dependency 재도입과 forbidden runtime lane 참조를 막는다.
- `batch/README.md`는 detached bootstrap 상태 기준으로 유지한다.

## Current / Target / Deferred-to-issue clarity for #384

Issue #384는 README/CI gate baseline만 문서화한다. 아래 표는 현재 실행 가능한 `batch` 계약과 목표 방향을 분리하고, 실제 구조 변경은 후속 이슈로 미룬다.

| Area | Current in `batch` | Target direction | Deferred-to-issue |
| --- | --- | --- | --- |
| Executable lane ownership | scheduler/batch lane은 root bootstrap 없이 `BatchApplication`과 module-local config로 실행된다. | 계속 `batch`가 scheduler runtime, scheduled jobs, maintenance flows를 소유한다. | #384 gate baseline only |
| Shared module ownership | `domain`, `module-contracts`, `global-utils`, `observability`, `infra`의 현재 공개 계약을 사용하고 `gateway`는 직접 의존하지 않는다. | shared module ownership/package closeout 이후 batch가 필요한 공개 계약만 더 좁게 사용한다. | #378 |
| CQRS/package normalization | `scheduler`, `booking`, `promotion`은 `Job -> Facade -> ApplicationService` 흐름으로 정렬됐고, application service는 아직 `application/` 바로 아래에 있다. | `com.beat.batch.<context>` 아래 application service command/query package와 dto 기준을 필요 시 더 세분화한다. | #382 |
| Gateway boundary | `batch`는 gateway에 직접 의존하지 않고 사용자/관리자 HTTP lane과 분리되어 있다. | scheduler lane은 gateway 인증 구현과 계속 분리한다. | #379 |
| Domain/persistence boundary | batch job은 domain model contract와 infra bootstrap을 통해 persistence를 사용하고, promotion scheduler의 promotion repository access는 domain `PromotionRepository` contract를 주입받아 infra 구현으로 격리한다. 인접 `ScheduleRepository` 등은 아직 transitional domain persistence concern이다. | domain entity/persistence 전략 정리 후에도 batch는 domain contract 중심으로 접근한다. | #380 |
| Infra/query boundary | `InfraConfig`가 JPA, QueryDSL, async group을 명시적으로 import하고, `InfraPersistenceConfig`를 IDE static-analysis breadcrumb로 직접 import한다. Runtime persistence import는 여전히 `JpaConfig`가 보장하며 scheduler bean은 Spring Boot auto-configuration이 소유한다. | QueryDSL/JDSL 전환과 `JpaConfig` scan 결정은 infra-owned boundary에서 정한다. | #381 |
| Async/scheduler handoff | 실행 모듈 중 유일하게 `@EnableScheduling`을 유지하고 `ScheduleBookingCloseJobPort` owner를 제공한다. | coroutine/async 확장 여부가 결정될 때까지 scheduler owner 계약을 넓히지 않는다. | #383 |

## Current ownership notes

### In `batch`

- scheduler runtime ownership
- `ScheduleBookingCloseJobPort` implementation for the active scheduler lane
- scheduled maintenance jobs and batch execution entrypoints
- batch-local scheduling bootstrap and owner flag defaults
- module-local bootstrap config such as `beat.scheduler.owner`

### Outside `batch`

- `module-contracts`: `ScheduleBookingCloseJobPort` 같은 cross-module execution contract
- `domain`: schedule/booking/promotion domain models and repository contracts
- `infra`: JPA, QueryDSL, async/task-scheduler bootstrap
- `global-utils`: shared common types and exception base contracts
- `observability`: logging / metrics / tracing boundary

## Remaining transitional debt

- owner namespace normalization과 `Job -> Facade -> ApplicationService` 흐름은 정렬됐지만, `application/service/command|query` 내부 세분화는 아직 최소 수준으로만 남겨 둔다.
- `domain` 모듈은 아직 JPA entity/repository 같은 persistence concern을 포함하는 transitional state이며, `Role`은 현재 `ROLE_*` 문자열만 소유하고 Spring Security `GrantedAuthority` bridge는 갖지 않는다. domain persistence 전략은 #380에서 다룬다.
- root executable lane은 retire되었고, scheduler runtime owner는 `batch`로 고정됐다.
- CQRS/service layer normalization과 batch 전용 package 정리는 다음 단계에서 다룬다.

## Guard rails

- `BatchApplicationTest`
    - `BatchApplication` import 집합 고정
    - narrow app bootstrap 유지
    - batch owner source package가 `com.beat.batch.*`로 정렬됐는지 확인
    - main/test resource owner flag 계약 고정
    - test profile이 blanket bean override 없이 유지되는지 확인
- `BatchArchitectureGuardTest`
    - `batch/build.gradle.kts`의 root dependency 재추가 금지
    - `apis`, `admin`, `gateway` 직접 의존 금지
    - root bootstrap lane 참조 금지
    - legacy owner package 선언 재도입 금지
    - `@Scheduled` / `ApplicationReadyEvent` runtime entrypoint는 `job/` package에서만 소유
    - `job/` entrypoint가 application/domain/infra/contract를 직접 호출하지 않고 facade boundary만 호출하는지 확인
- `BatchModuleContextBootTest`
    - module context boot smoke test
    - test profile에서 `beat.scheduler.owner=false`가 실제로 적용되는지 확인
    - batch-owned `ScheduleBookingCloseJobPort`가 detached classpath에서 그대로 올라오는지 확인
    - batch lane만 명시적으로 소유한 `taskScheduler`를 유지하는지 확인
- `BatchSchedulerOwnerBootTest`
    - owner-enabled runtime에서 `ScheduleBookingCloseJobPort -> JobSchedulerService` 해석 고정
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

- `Facade`는 batch context/job scenario의 공식 진입점이며 실행 시나리오 조합을 맡는다. Job/Runner는 facade만 호출하고, facade는 필요한 ApplicationService를 호출한다. 단, raw Domain model을 받거나 반환하지 않는다.
- 실제 잡 실행은 대부분 command 성격이므로 `application/service/command`를 기본으로 둔다.
- 배치 조회/리포트/통계가 필요할 때만 `application/service/query`를 추가한다.
- DTO는 command/query로 나누지 않고 `application/dto` 아래에서 관리한다. Facade 조합용 내부 결과가 필요할 때만 `application/dto/result`를 추가한다.
- command job/service는 domain repository contract와 infra 구현을 통해 저장/수정 흐름과 transaction을 수행한다. 단순 조회는 domain repository contract를 사용할 수 있지만, 배치 리포트/통계 조회가 필요해질 때는 infra persistence mapper를 직접 재사용하지 않고 query 전용 read model/projection을 둔다. infra adapter가 필요하면 실행 모듈 타입을 infra가 import하지 않고 module-contracts read contract를 먼저 둔다.
- 배치 애플리케이션 문맥의 에러 코드는 `application/exception`에 둔다. repository lookup 실패, batch flow 실패, 외부 adapter 실패 번역을 domain ErrorCode로 표현하지 않는다.
- batch는 HTTP response success code가 기본적으로 필요하지 않다. 배치 결과 메시지가 필요해질 때는 batch-local result/response boundary가 소유하고 domain에는 `SuccessCode`를 추가하지 않는다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- Repository는 지금 즉시 분리하지 않고, 복잡한 조회 전용 구현이 필요할 때만 infra `repository.query` 레이어를 추가한다. 이 레이어는 batch 리포트/통계용 read projection을 소유하고 JPA entity와 domain model을 번역하는 mapper를 재사용하지 않는다.

### Spring Batch adoption rule

현재 `batch` 모듈은 Spring Batch가 아니라 Spring Scheduling / Spring Boot auto-configured `TaskScheduler` 기반으로 동작한다.

- 단순 정기 유지보수 작업은 `@Scheduled` / auto-configured `TaskScheduler` + `Job -> Facade -> ApplicationService` 구조를 기본으로 둔다.
- 대량 chunk processing, restartability, execution metadata, step orchestration이 실제 요구사항으로 생길 때만 Spring Batch 도입을 검토한다.
- Spring Batch를 도입하더라도 batch module boundary는 유지한다. Spring Batch `Job/Step`은 실행 adapter이고, 비즈니스 유스케이스는 Facade/ApplicationService로 위임한다.

### Layer boundary standard

BEAT의 batch lane은 HTTP Controller 대신 Job/Runner가 entrypoint인 점만 다르고, 내부 계층 의미는 실행 모듈 표준을 따른다.

```text
Job/Runner -> Facade -> ApplicationService(command/query) -> DomainService/Entity/RepositoryPort/ReadPort
```

- Job/Runner는 scheduler/launcher adapter이며 `Facade`만 호출한다.
- `Facade`는 배치 실행 시나리오의 공식 진입점이다. 여러 command/query service output을 조합하지만, transaction/repository/domain service를 직접 소유하지 않는다.
- `Facade`는 raw Domain model을 절대 받거나 반환하지 않는다. Facade 입력/출력은 primitive, 실행 결과 DTO, CommandResult/QueryResult 같은 batch 내부 전달 모델로 제한한다.
- `ApplicationService`는 command service와 query service를 의미한다. 이 계층만 유스케이스 method 내부에서 Domain model을 조회/변경/정책 판단에 사용할 수 있고, Domain model은 이 계층 밖으로 반환하지 않는다. 다른 ApplicationService에 raw Domain model을 반환하는 public helper method를 새로 만들지 않는다.
- batch 흐름은 대부분 command service로 시작한다. 리포트/통계/read-model이 필요할 때만 query service를 추가한다.
- ApplicationService는 순수 도메인 정책을 직접 구현하지 않고 `domain.<context>.service`의 DomainService 또는 Entity/VO method에 위임한다.
- 배치 리포트/통계 query는 domain repository를 키우지 않고 `module-contracts` read port + infra query adapter 또는 infra adapter가 필요 없는 batch query service 내부 read-model 경계로 분리한다.


### CQRS query/read-model rule

- BEAT의 batch CQRS는 잡 실행 흐름을 command service로, 리포트/통계/조회 흐름을 query service로 분리하는 것부터 시작한다.
- command service는 Domain model 중심으로 cleanup, 상태 변경, 저장/삭제 흐름을 수행한다.
- query service는 배치 리포트/통계/read-model 조립을 맡지만 Domain model을 Facade, Job/Runner, 다른 ApplicationService로 반환하지 않는다.
- 단순 조회는 domain repository contract를 사용할 수 있다. 다만 리포트/통계/목록/projection 조회가 되면 domain repository를 키우지 않고 read-model로 분리한다.
- infra query adapter가 구현하고 batch query service가 주입받아야 하는 조회 계약은 `module-contracts`의 read port/result/condition으로 둔다.
- batch 내부에서만 쓰는 조립 결과는 `batch.<context>.application.dto.result` 또는 query service 내부 result로 둔다.
- query service는 JPA Entity, QueryDSL Q type, EntityManager, infra persistence mapper를 직접 사용하지 않는다.

### Response and domain exposure rule

- 배치 command/query service는 필요한 실행 결과 DTO 또는 result를 반환한다.
- 여러 command/query service output을 조합하는 배치 scenario에서만 Facade가 최종 실행 결과를 만든다.
- Job/Runner와 Facade에는 raw Domain model을 절대 올리지 않는다.
- ApplicationService 간 공유도 raw Domain model이 아니라 primitive/value/result/read model로 한다.
- DTO, CommandResult, QueryResult는 Domain model을 필드로 담지 않는다.
- `apis`, `admin`, `batch` 간 DTO/ApplicationService/Facade를 공유하지 않는다. 공유가 필요하면 `module-contracts`에 최소 계약을 새로 정의한다.


### ResponseDTO vs Result selection rule

Batch는 HTTP ResponseDTO보다 실행 결과 DTO/result가 중심이다. 그래도 선택 기준은 동일하다. 기본값은 command/query service가 필요한 실행 결과를 완성해 반환하는 것이고, Result는 Facade 조합이 필요한 순간에만 추가한다.

- service 하나가 job 실행 결과를 완성할 수 있으면 command/query service가 실행 결과 DTO 또는 void를 반환한다.
- Facade가 받은 값을 그대로 반환하거나 Job/Runner가 결과를 쓰지 않으면 별도 Result를 만들지 않는다.
- Facade가 여러 command/query service output을 다시 섞고 재가공해 하나의 batch execution result를 만들어야 하면 각 service는 CommandResult/QueryResult를 반환하고 Facade가 최종 실행 결과를 만든다.
- Result는 Facade 조합용 application output이다.
- Result도 raw Domain model, JPA Entity, infra projection row를 필드로 담지 않는다. primitive/JDK type, contract-local value, batch 내부 value만 사용한다.
- 다른 ApplicationService가 재사용해야 하는 출력이면 raw Domain model을 반환하지 말고 목적이 드러나는 Result 또는 ReadModel을 먼저 정의한다.
- 배치 리포트/통계 결과를 여러 job/facade에서 재사용해야 하거나 외부 알림/로그/파일 출력 shape와 내부 결과를 분리하고 싶을 때 Result를 둔다.
- 단일 job command와 1:1인 단순 cleanup 흐름에 Result를 만들지 않는다.

```text
단일 batch command:
Job/Runner -> Facade -> CommandService -> void or ExecutionDTO

복합 batch scenario:
Job/Runner -> Facade -> CommandService A -> CommandResult A
                    -> QueryService B   -> QueryResult B
                    -> Final ExecutionDTO
```

## Follow-up after this issue

1. `com.beat.batch.<context>` 내부 하위 계층(`job`, `application/service`, `dto`)을 문맥별로 정리
2. scheduler-related closeout docs를 batch ownership 기준으로 더 축소
3. shared test bootstrap convergence가 필요해지면 실행 모듈 간 중복 test container setup 정리
