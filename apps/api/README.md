# apis module

> 이 문서는 `apis` 모듈의 현재 bootstrap 계약, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `apis`는 사용자 API 실행 모듈이며, root
> project 의존 없이 자체 classpath로 build/boot/test 되어야 한다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| 사용자 API의 기존 V1 Controller/API interface만 Java에 남고, DTO·Facade·ApplicationService·설정·예외 처리는 Kotlin으로 이전됐다. 상태 변경과 조회가 함께 있는 context는 `application/command`, `application/query`로 분리한다. | 새 운영 코드는 Kotlin으로 작성하고 동일한 context 경계와 HTTP 계약을 유지한다. | 명세가 확정된 V2 endpoint의 Kotlin Controller 추가 |

## 역할

- 사용자 대상 HTTP API의 유일한 실행 진입점이다.
- 사용자용 Request/Response DTO, Controller, user-facing Swagger/OpenAPI 노출을 소유한다.
- 팀 컨벤션상 `Controller -> Facade -> Application Service -> Domain` 흐름을 따른다.
- 비즈니스 규칙은 `domain` 계약에 위임하고, 구현 기술과 외부 연동은 `infra` 및 명시적 module bootstrap 경계를 통해 사용한다.
- 인증/인가는 `gateway`의 공개 계약과 bootstrap 경계를 통해 연결된다.

## 허용 의존성

- `:application:frontoffice`
- `:infrastructure`의 명시적 composition configuration만
- `:support:security`의 공개 technical/bootstrap API
- `:support:observability`

Production source의 `:domain` 직접 의존은 금지합니다. Domain failure와 model은 Application boundary를 통해 번역합니다.

## 금지 규칙

- `project(":")` 직접 의존 금지
- `admin`, `batch` 직접 의존 금지
- `infra.external.*`, `infra.*.entity`, `infra.*.repository.impl` 직접 import 금지
- `:support:security` 내부 구현 직접 import 금지
    - `com.beat.support.security.jwt.internal.*`
    - `com.beat.support.security.guest.internal.*`
    - `com.beat.support.security.authentication.internal.*`
    - `com.beat.support.security.shared.internal.*`
- 허용되는 `:support:security` 공개 표면은 `com.beat.support.security` root bootstrap/`CurrentMember`, `password/PasswordHasher`, `token/*` technical API와 `GUEST_ACCESS`로 제한한다.
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
    exception/
      ApiGlobalExceptionHandler.kt
    config/
      GatewayConfig.kt              # servlet security + gateway auth capability bootstrap
      InfraConfig.kt                # infra group 선택 + AuthRedisConfig 명시적 import
      WebConverterConfig.kt         # MVC converter registration only
    web/converter/
      CaseInsensitiveStringToEnumConverterFactory.kt

  src/main/kotlin/com/beat/apis/
    booking/
    home/
    member/
    performance/
    promotion/
    schedule/
    ticket/
    user/
    external/
    swagger/
    config/
      ApisSecurityConfig.java       # apis-owned HTTP security policy
```

### Runtime contract

- `ApisApplication`은 module-local `GatewayConfig`, `InfraConfig`와 공개 `ObservabilityModuleConfig`만 import한다.
- `GatewayConfig`가 `@EnableGatewayServletSecurity`와 `@EnableGatewayConfig(GUEST_ACCESS)`를 캡슐화하고, `InfraConfig`가 `AuthRedisConfig`를 명시적으로 import해 auth application output port인 `RefreshTokenStore`의 Redis adapter를 조립한다.
- executable bootstrap resource는 module-local 값과 `spring.profiles.group`만 소유하고, persistence/redis/external/jwt/observability 설정은 각 concern-owned `application-*.yml`로 분리한다.
- app-level broad `@ComponentScan`은 없다.
- `@SpringBootApplication(scanBasePackageClasses = [ApisApplication::class])`가 `com.beat.apis.*` owner namespace만 스캔한다.
- `ApisSecurityConfig`가 route whitelist와 인증 정책을 소유한다.
- `ApisSecurityConfig`는 `gatewaySecurityMdcLoggingFilter`를 JWT보다 먼저 배치해 모든 응답에 trace/request MDC와 `X-Request-ID`를 보장하고, 이후 `gatewayJwtAuthenticationFilter`가 인증 성공 시 MDC `userId`를 갱신한다.
- gateway 내부 `SecurityMdcLoggingFilter` 클래스는 직접 import하지 않고 qualifier + `OncePerRequestFilter` 타입으로만 주입한다.
- observability 내부 config는 직접 import하지 않고 `ObservabilityModuleConfig`만 사용한다.
- 예매 가능 여부는 `booking_close_at`, 재고, `CURRENT_TIMESTAMP(6)`으로 계산하며 `apis`는 예매 마감 스케줄러 계약에 의존하지 않는다.
- 조회 API는 `ScheduleAvailabilityReadPort`의 단일 native SQL이 반환한 `evaluated_at`을 `isBooking`과 `dueDate`에 함께 사용한다.

## What changed in issue #360

- `apis/build.gradle.kts`에서 `implementation(project(":"))`를 제거했다.
- `apis`는 root project classpath 없이 build/boot/test 되는 방향으로 고정됐다.
- Issue #428에서 no-op 마감 스케줄러 bridge와 실행 계약을 제거했다.
- 테스트 계약을 갱신해 root dependency 재도입, root bootstrap import, root scheduler owner 재연결을 막는다.

## Current / Target / Deferred-to-issue clarity for #384

Issue #384는 README/CI gate baseline만 문서화한다. 아래 표는 현재 실행 가능한 `apis` 계약과 목표 방향을 분리하고, 실제 구조 변경은 후속 이슈로 미룬다.

| Area | Current in `apis` | Target direction | Deferred-to-issue |
| --- | --- | --- | --- |
| Executable lane ownership | 사용자 API lane은 root bootstrap 없이 `ApisApplication`과 module-local config로 실행된다. | 계속 `apis`가 user-facing controller/DTO/security/OpenAPI를 소유한다. | #384 gate baseline only |
| Dependency ownership | Frontoffice Application과 좁은 Infrastructure/Security/Observability bootstrap만 사용한다. | Controller/Web에서 Infrastructure 구현 접근을 금지한다. | compiled guard |
| CQRS/package normalization | context별 `api/request`, `api/response`, `facade`, `application/command`, `application/query`, `application/result` 경계를 적용했다. 조회·변경 중 한쪽만 있는 context에는 불필요한 빈 package를 만들지 않는다. | 응집된 변경 이유와 transaction 경계를 기준으로 service를 나누며 endpoint마다 기계적으로 클래스를 만들지 않는다. | architecture guard로 지속 검증 |
| Gateway/Redis boundary | `@EnableGatewayServletSecurity`, `@EnableGatewayConfig(GUEST_ACCESS)`, `com.beat.support.security.CurrentMember`와 public password/token API를 사용한다. Redis refresh store는 auth application output port `RefreshTokenStore`를 infrastructure가 구현하고 composition root가 `AuthRedisConfig`로 조립한다. | `:support:security` 내부 구현 직접 참조 없이 public API와 명시적 infra config만 사용한다. | architecture guard로 고정 |
| Domain/persistence boundary | API DTO는 JPA Entity/QueryDSL Q type/Redis document를 직접 노출하지 않는 guard를 유지하고, 홈 화면의 공연·프로모션 조회는 단일 `HomeQueryService` read-only transaction에서 조합한다. | domain persistence 전략 정리 후에도 API boundary는 transfer DTO 중심으로 유지한다. | #380 |
| Infra/query boundary | `InfraConfig`가 JPA, QueryDSL, async, external-client group을 명시적으로 import하고, `InfraPersistenceConfig`를 IDE static-analysis breadcrumb로 직접 import한다. Runtime persistence import는 여전히 `JpaConfig`가 보장한다. | QueryDSL/JDSL 전환과 scan 결정은 infra-owned boundary에서 정한다. | #381 |
| Async/scheduler handoff | `apis`는 scheduler를 실행하지 않는다. 예매 마감은 DB 시각 기반 계산으로 완결된다. | async/coroutine 도입 범위가 결정될 때까지 HTTP lane의 비동기 경계를 넓히지 않는다. | #383, #428 |

## Current ownership notes

### In `apis`

- user-facing controller / application service / DTO
- module-local security policy
- user-facing Swagger/OpenAPI exposure as an executable-module owner concern
- module-local bootstrap config such as `springdoc.*`, `cors.allowed-origins`, `app.server.url`
- global exception handling for the HTTP lane
- notification/file API entrypoints도 `com.beat.apis.*` owner namespace로 정렬됐다.

### Outside `apis`

- `gateway`: public bootstrap/current-member/JWT contract와 internal JWT/security implementation boundary
- `application:frontoffice`: use cases, transaction, result/failure language, consumer-owned contracts
- `infrastructure`: composition root가 import하는 최소 bootstrap configuration
- API-local: `ErrorResponse`, `SuccessResponse`, `SuccessCode`와 HTTP/Jackson policy
- `observability`: MDC/access logging, metrics/actuator, Micrometer/OpenTelemetry tracing bootstrap
- `batch`: 프로모션 관리와 티켓 정리 등 정기 유지보수 작업

## Remaining transitional debt

- V1 Java source에는 기존 `*Controller.java`, `*Api.java`만 둔다. DTO·Facade·ApplicationService·설정·예외 처리 등 나머지 운영 코드는 Kotlin을 사용한다.
- 새 V2 Controller는 endpoint 명세가 확정된 뒤 Kotlin으로 작성한다. 단순 마이그레이션을 이유로 새 URL이나 HTTP 계약을 추측해 만들지 않는다.
- root executable lane은 retire되었고, `apis`는 root bootstrap 없이 detached module contract를 유지한다.

## Guard rails

- `ApisApplicationTest`
    - `ApisApplication` import 집합 고정
    - broad app scan 금지
    - owner source package가 `com.beat.apis.*`로 정렬됐는지 확인
    - test profile이 blanket bean override 없이 유지되는지 확인
- `ApisArchitectureGuardTest`
    - `apis/build.gradle.kts`의 root dependency 재추가 금지
    - root bootstrap lane import 금지
    - gateway 공개 import allowlist 및 infra implementation package 직접 import 금지
    - DTO/event boundary raw Domain model import 금지
    - API client boundary(`api/request`, `api/response`, `facade`, `application/result`)의 domain enum/value import 금지
- `ApisDtoJsonContractTest`
    - API-local enum 이름과 JSON string 값 호환성 고정
    - Home response field name과 enum-string JSON 호환성 고정
- `ApisModuleContextBootTest`
    - module context boot smoke test
    - 예매 마감 scheduler bean 없이 기동하는지 확인
    - shared async import가 `TaskScheduler`를 함께 올리지 않는지 확인

## To-Be direction

```text
com.beat.apis.<context>/
  api/
    request/
    response/
    type/
  facade/
  application/
    command/
    query/
    result/
    event/
  exception/
  config/
```

### Directional rules

- `Facade`는 API 시나리오 조합과 최종 응답 반환을 맡는다. 단, raw Domain model을 받거나 반환하지 않는다.
- CQRS는 상태 변경과 조회가 모두 있는 context에서 `application/command`, `application/query`로 나눈다. 단일 책임을 이유로 use-case마다 클래스를 기계적으로 만들지 않고, 응집된 변경 이유와 transaction 경계를 기준으로 분리한다.
- Java/Kotlin 및 API 버전과 무관하게 외부 HTTP DTO는 `<context>/api/request`, `<context>/api/response`가 소유한다. `create`, `modify`, `detail` 같은 유스케이스별 하위 DTO 패키지는 만들지 않는다.
- 모든 JSON request의 필수 필드는 누락과 명시적 `null`도 동일한 Bean Validation 계약으로 처리할 수 있도록 nullable immutable property와 `jakarta.validation` field constraint를 사용한다. Controller의 `@RequestBody`에는 `@Valid`를 적용한다.
- 검증된 request는 Facade에서 non-null application command 또는 query/search condition으로 변환한다. 상태 변경은 command, 복잡한 조회 조건은 query/condition을 사용하고 단순 path variable이나 query parameter 한두 개를 기계적으로 객체화하지 않는다.
- request/response JSON 필드명과 기존 오류 응답 계약은 패키지 이동 및 application 입력 분리와 무관하게 유지한다.
- request와 response가 함께 쓰는 wire enum은 `api/type`, 내부 후속 처리 이벤트는 `application/event`가 소유한다. enum과 event를 DTO 패키지에 섞지 않는다.
- command service는 상태 변경 유스케이스와 transaction 경계다. domain repository contract로 Domain model을 조회/변경/저장하고, 필요한 순수 정책은 DomainService/Entity/VO에 위임한다.
- query service는 조회 흐름과 consumer read model 조립을 맡습니다. 화면/검색/정렬/통계 조회 계약은 `application:frontoffice` consumer가 소유하고 Infrastructure가 구현하며, Apps는 persistence mapper를 직접 사용하지 않습니다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- executable-lane owner file은 계속 `com.beat.apis.*` 아래에 둔다.


### Layer boundary standard

BEAT의 사용자 API lane은 확장성과 리팩터링 안정성을 위해 아래 호출 방향을 표준으로 둔다.

```text
Controller -> Facade -> ApplicationService(command/query) -> DomainService/Entity/RepositoryPort/ReadPort
```

- Controller는 HTTP adapter이며 `Facade`만 호출한다. Repository, DomainService, command/query service를 직접 호출하지 않는다.
- `Facade`는 API 시나리오의 공식 진입점이다. 여러 command/query service output을 조합하고 최종 response를 반환하지만, transaction을 열거나 repository/domain service를 직접 호출하지 않는다.
- `Facade`는 raw Domain model을 절대 받거나 반환하지 않는다. HTTP request를 검증된 application command/query로 변환하고, 입력/출력은 request primitive, ResponseDTO, CommandResult/QueryResult 같은 실행 모듈 내부 전달 모델로 제한한다.
- `ApplicationService`는 command service와 query service를 의미한다. 이 계층만 유스케이스 method 내부에서 Domain model을 조회/변경/정책 판단에 사용할 수 있고, Domain model은 이 계층 밖으로 반환하지 않는다. 다른 ApplicationService에 raw Domain model을 반환하는 public helper method를 새로 만들지 않는다.
- command service는 상태 변경 흐름과 transaction 경계를 맡습니다. repository 조회/저장, lock, event, 증명된 output port, DomainService/Entity 호출 순서를 책임집니다.
- transactional command method 전체를 `runCatching`으로 감싸거나 `Result.failure`로 정상 반환하지 않는다. Spring rollback이 필요한 실패는 예외로 경계 밖까지 전파한다.
- query service는 조회 흐름과 application read model 조립을 맡는다. HTTP ResponseDTO 조립은 Facade가 담당해 application이 API 계약에 의존하지 않게 한다.
- ApplicationService는 도메인 판단을 직접 if/계산으로 반복 구현하지 않는다. 주요 도메인 판단은 `domain.<context>.service`의 DomainService 또는 Entity/VO method에 위임한다.
- Kotlin 생성자에 인자가 둘 이상이면 named argument를 사용해 필드 대응을 명시한다. Java 생성자와 외부 라이브러리 API에는 이 규칙을 강제하지 않는다.
- Kotlin-owned input/output은 Kotlin property와 nullability를 사용하며 Java-only `@Jvm*` compatibility를 관성적으로 추가하지 않습니다.
- DomainService는 `apis`가 아니라 `domain` 모듈에 둔다. `apis`에는 `application/port/in` 같은 use-case port 패키지를 기본으로 만들지 않는다.
- 복잡한 화면 조회/read-model은 Domain repository를 키우지 않고 해당 Application capability의 query reader/read model로 분리합니다.
- application/use-case 실패 사유는 `<context>/exception/<Context>ApplicationErrorCode`가 소유한다. repository lookup 실패, request/use-case input validation, actor/owner/permission 검증, external adapter 실패 번역은 domain ErrorCode로 표현하지 않는다.
- Application failure language는 `application:frontoffice`가 소유합니다. `ApiGlobalExceptionHandler`는 그 실패를 API-local response envelope과 HTTP status로 변환하며 Domain failure를 직접 매핑하지 않습니다.
- Controller response 성공 문구는 `api/response/<Context>SuccessCode`가 소유한다. `SuccessCode`를 domain에 새로 추가하지 않는다.
- `infra` adapter가 던진 adapter-local failure는 application service에서 API-facing ErrorCode로 번역한다. `infra`가 `apis` ErrorCode를 import하게 만들지 않는다.
- 같은 transaction에서 지켜야 하는 invariant는 event listener로 넘기지 않는다. Application event는 저장 성공 이후의 알림·관측 같은 부수 효과에만 사용한다.
- ApplicationService는 다른 ApplicationService를 직접 주입하지 않는다. 별도 transaction·외부 I/O 경계가 필요하면 의미가 드러나는 internal collaborator로 분리하고, 단순 재사용이면 repository/port 호출을 해당 유스케이스 안에 둔다.
- Application의 현재 시각은 composition root가 제공하는 `Clock`으로 읽는다. `LocalDate.now()`와 `LocalDateTime.now()`를 유스케이스 안에서 직접 호출하지 않는다.
- `@TransactionalEventListener(AFTER_COMMIT)`은 best-effort 후속 처리에만 사용한다. 전달 보장이 필요한 외부 작업은 outbox와 idempotent consumer를 별도 설계한다.


### CQRS query/read-model rule

BEAT의 CQRS는 저장소/DB를 처음부터 물리적으로 둘로 나누는 것이 아니라, 실행 모듈의 ApplicationService를 command와 query로 분리하는 것부터 시작한다.

```text
Controller -> Facade -> command service  -> Domain Repository -> Domain model -> CommandResult
Controller -> Facade -> query service    -> ReadPort/ReadModel or simple Domain Repository -> QueryResult
Controller <- Facade                     <- ResponseDTO conversion
```

- command service는 Domain model 중심이다. Domain repository는 command와 aggregate lifecycle에 필요한 저장/수정/단순 조회 언어만 유지한다.
- query service는 application read model/result를 조립하며 Domain model을 Facade나 다른 ApplicationService로 반환하지 않는다.
- 단순 조회는 domain repository contract를 임시로 사용할 수 있다. 예: `findById`, `findAllByPerformanceId`, `exists...`처럼 Domain model이 실제로 필요한 조회.
- 화면/검색/목록/정렬/통계/N+1 회피/fetch 전용/projection 조회는 read-model로 분리한다.
- read-model은 save 대상이 아니며 Domain model도 API ResponseDTO도 아니다. query 결과를 담는 내부 조회 shape다.
- Infrastructure query adapter가 구현할 조회 계약은 consumer인 Application query package가 소유합니다. 실제 projection volatility와 deletion test를 통과할 때만 reader/read model을 만듭니다.
- 특정 API query service 내부에서만 쓰는 조립 결과는 `apis.<context>.application.result` 또는 query service private row/result로 둔다.
- query service는 JPA Entity, QueryDSL Q type, EntityManager, infra persistence mapper를 직접 사용하지 않는다.
- mapper 타입은 실행 모듈에 만들지 않는다. API ResponseDTO 조립은 Facade가 담당하고 Controller는 HTTP envelope만 조립한다. persistence entity ↔ domain 변환만 `infra.persistence.<aggregate>.mapper`가 담당한다.
- 공연 조회는 목적별로 `PerformanceDetailQueryService`(공개 상세), `PerformanceEditFormQueryService`(메이커 편집 폼), `MakerPerformanceListQueryService`(메이커 목록)로 나눈다. `EditForm`은 수정 명령이 아니라 수정 화면을 만들기 위한 조회임을 이름으로 드러낸다.
- 공연 수정 transaction은 `PerformanceModifyCommandService`가 소유한다. Cast/Staff/Image는 `Performance` 내부 Entity로 교체한 뒤 `PerformanceRepository.save()` 한 번으로 저장한다. 별도 coordinator나 child RepositoryPort를 만들지 않는다. 독립 Aggregate인 Schedule 동기화만 같은 command package의 `internal ScheduleSynchronizer`에 위임한다.
- `PerformanceSummaryReadPort`는 예매·티켓·홈·관리자 검증처럼 공연 전체 Aggregate가 필요 없는 조회에 사용한다. `PerformanceRepository`는 공연 Aggregate lifecycle에만 사용한다.
- 순수 DomainService는 `domain`의 일반 class이며 `apis.config.DomainServiceConfig`에서 생성한다. ApplicationService가 직접 생성하거나 DomainService에 Spring annotation을 붙이지 않는다.

### Response and domain exposure rule

- command/query service는 HTTP ResponseDTO가 아니라 application result/read model을 반환한다.
- Facade가 application output을 최종 ResponseDTO로 변환한다. 단일 위임이어도 HTTP 계약 변환 경계는 Facade에 유지한다.
- Controller와 Facade에는 raw Domain model을 절대 올리지 않는다.
- ApplicationService 간 공유도 raw Domain model이 아니라 primitive/value/result/read model로 한다.
- ResponseDTO, RequestDTO, CommandResult, QueryResult는 Domain model을 필드로 담지 않는다.
- 실행 모듈 간 DTO/ApplicationService/Facade를 공유하지 않습니다. 중앙 contracts module 대신 Domain collaboration 또는 consumer-owned narrow seam을 사용합니다.


### ResponseDTO vs Result selection rule

BEAT의 기본값은 command/query service가 HTTP 비의존 application result/read model을 반환하고 Facade가 ResponseDTO를 만드는 것이다.

- service 하나가 endpoint 결과를 완성해도 HTTP ResponseDTO를 반환하지 않는다.
- Facade는 application output을 기존 JSON 계약의 ResponseDTO로 변환한다.
- Facade가 여러 command/query service output을 다시 섞고 재가공해 하나의 API response를 만들어야 하면 각 service는 CommandResult/QueryResult를 반환하고 Facade가 최종 ResponseDTO를 만든다.
- Result는 최종 client contract가 아니라 Facade 조합용 application output이다.
- Result도 raw Domain model, JPA Entity, infra projection row를 필드로 담지 않는다. primitive/JDK type, contract-local value, 실행 모듈 내부 value만 사용한다.
- 다른 ApplicationService가 재사용해야 하는 출력이면 raw Domain model을 반환하지 말고 목적이 드러나는 Result 또는 ReadModel을 먼저 정의한다.
- 같은 service output을 여러 response shape로 재사용해야 하거나 API response 변경으로부터 application output을 보호하고 싶을 때 Result를 둔다.
- 단일 API response와 1:1인 단순 유스케이스는 최소 result/read model 하나만 두고 불필요한 중간 계층을 추가하지 않는다.

```text
단일 유스케이스:
Controller -> Facade -> QueryService -> QueryResult -> ResponseDTO

복합 API scenario:
Controller -> Facade -> QueryService A -> QueryResult A
                     -> QueryService B -> QueryResult B
                     -> Final ResponseDTO
```

## Follow-up after this issue

1. `com.beat.apis.<context>` 내부 하위 계층(`api/request`, `api/response`, `facade`, `application/command`, `application/query`, `application/result`)을 문맥별로 일관되게 유지
2. `ScheduleAvailabilityReadPort` 조회 계약과 DB 시각 기준을 회귀 테스트로 유지
3. package normalization 이후 문맥별 하위 계층 정리를 별도 리팩터링 lane으로 진행
