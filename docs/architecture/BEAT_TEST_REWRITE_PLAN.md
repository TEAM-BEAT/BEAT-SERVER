# BEAT-SERVER Test Rewrite Plan

> Status: implemented; baseline inventory and execution decisions retained as the audit trail
>
> Test Constitution: `docs/architecture/BEAT_TEST_ARCHITECTURE_FINAL.md`
>
> Source baseline: `develop` at `eb007147f6aa3824073b108407ea3ae47748aa40`
>
> Migration execution baseline: `refactor/cqrs-migration-pr9` at `af04273b91a193ce45717983bd6d1f6d03d93a20`
>
> Prepared: 2026-08-22

## 1. Executive Verdict

BEAT의 테스트를 전면 재작성하되 big-bang으로 교체하지 않는다. 각 production risk의 기존 보호 장치를 먼저 식별하고, 같은 risk를 더 작고 충실한 owner에게 옮긴 뒤에만 기존 테스트를 삭제한다.

최종 선택은 다음과 같다.

1. JUnit Platform을 실행 계약으로 유지하고 신규·재작성 Kotlin 테스트는 Kotest `FunSpec`으로 통일한다.
2. Domain과 Application 정책은 Spring 없는 fast test가 소유한다.
3. MySQL query, constraint, DB clock, pessimistic lock과 Redis TTL·Lua atomicity는 실제 MySQL/Redis Testcontainers가 소유한다. H2는 사용하지 않는다.
4. Web contract와 authorization은 `@WebMvcTest` 기반 slice가 소유하고, 핵심 사용자 여정만 `@SpringBootTest` acceptance가 소유한다.
5. 동시성 테스트는 production application transaction을 worker thread가 직접 호출하게 하며 test-level transaction을 금지한다.
6. fixture 기본값은 named Kotlin DSL이다. Fixture Monkey는 DTO/nested graph/bulk data에서만 선택적으로 도입하고, Kotest Property는 수학적 invariant와 경계값에만 사용한다.
7. Gradle이 막을 수 있는 module dependency는 Gradle로, source visibility는 Kotlin `internal`/compiler로, 남는 semantic package rule만 ArchUnit으로 막는다.
8. OpenAPI baseline diff와 실제 endpoint/role authorization matrix를 PR gate로 추가한다. Pact는 현재 consumer contract evidence가 없어 도입하지 않는다.
9. 현재 Flyway/Liquibase/versioned migration이 없으므로 migration suite는 만들지 않는다. 도입되는 PR에서 real-MySQL migration verification을 함께 추가하는 조건부 gate로 남긴다.
10. PR9 이후 계획은 기존 PR10~19를 그대로 실행하지 않는다. correctness characterization, test platform, risk-owner rewrite, capability migration을 compile/source-conflict 순서로 다시 배치한다.

가장 먼저 닫아야 할 gap은 `Performance` 가격 수정과 `Booking` 생성이 동시에 실행될 때 어느 가격 snapshot이 저장되고 어떤 lock order로 직렬화되는지에 대한 executable contract 부재였다. PR11 조사에서 두 command 모두 Performance를 먼저 잠근다는 사실을 확인했지만, 가격 수정 트랜잭션이 lock 대기 전에 만든 MySQL `REPEATABLE READ` snapshot으로 active Booking을 확인해 새 Booking을 놓칠 수 있는 correctness defect가 재현되었다. PR11은 active Booking 결정을 Schedule lock 이후의 current locking read로 바꾸고 두 직렬화 결과를 executable contract로 고정한다.

## 2. Repository Facts

### 2.1 Baseline distinction

| Baseline | Fact |
|---|---|
| `develop` | Source of actual released behavior. 99 tracked Java/Kotlin test sources: 98 test/base classes + `MockitoKotlinExtensions.kt` helper. |
| PR9 worktree | Migration execution state. 111 tracked Java/Kotlin test sources: 110 test/base classes + the same helper. |
| Workspace-only drafts | 8 untracked tests exist. They are not `develop` evidence and are not silently adopted or deleted. Their provisional disposition is recorded separately. |

The 77 test sources common to both baselines are inventoried against their PR9 path below. The 22 `develop`-only paths are mapped explicitly in §3.9, so no `develop` regression owner is omitted.

### 2.2 Actual module graph

`develop` uses the legacy physical projects:

```text
:admin
:apis
:batch
:core:domain
:core:infra
:gateway
:global-support
:module-contracts
:observability
```

PR9 exposes the target logical names while several projects still point at legacy directories:

```text
:apps:api            -> apis/
:apps:admin          -> admin/
:apps:batch          -> batch/
:application:frontoffice
:application:admin
:application:system
:domain              -> core/domain/
:infrastructure      -> core/infra/
:support:security    -> gateway/
:support:observability -> observability/
:module-contracts
:global-support
```

Current dependency facts relevant to tests:

- `apps:api` still depends on `application:frontoffice`, `module-contracts`, `support:security`, `domain`, `infrastructure`, `global-support`, and `support:observability`.
- `apps:admin` and `apps:batch` still compose legacy contracts/infrastructure directly.
- `application:admin` and `application:system` currently depend only on `domain` and do not yet own migrated workflows.
- `application:frontoffice` already owns migrated Booking, Performance, Schedule, Ticket, Member/Auth use cases and has an ArchUnit test dependency declared locally.
- `infrastructure` implements persistence, Redis, Kakao, S3, Slack, SMS, and event-listener details.

### 2.3 Build and test tooling

| Item | Actual state |
|---|---|
| Runtime line | PR9: Spring Boot 4.0.8, Kotlin 2.3.21, Testcontainers 2.0.5, JUnit 6.0.3 |
| Execution engine | root `build.gradle.kts` and `build-logic/src/main/kotlin/beat.test.gradle.kts` call `useJUnitPlatform()` |
| Authoring | JUnit Jupiter + Mockito; some Kotlin tests still use Java-style assertions and compatibility helpers |
| Absent | Kotest, MockK, Fixture Monkey, Kotest Property |
| Containers | MySQL and Redis, primarily static/manual startup or per-class JUnit container lifecycle |
| Spring contexts | 4 `@SpringBootTest`-based paths in PR9, no `@WebMvcTest`, `@DataJpaTest`, or `@DataRedisTest` |
| Tags/tasks | a small set of `integration` tags; no complete risk-based task topology |
| CI | one `./gradlew check verifyModuleBootJars --parallel --build-cache` verify job, then image scan; no fast/integration/correctness split |
| OpenAPI | springdoc groups exist (`/v3/api-docs/general`, `/api/admin/v3/api-docs/admin`), but no checked-in baseline or breaking-diff task |
| Schema migrations | no Flyway, Liquibase, or versioned migration files; test profile uses schema creation rather than migration replay |

Module-local test dependency facts:

| Project | Current explicit test dependencies beyond applied conventions |
|---|---|
| root | JUnit Jupiter + JUnit Platform launcher |
| `apps:api` | Spring Boot app convention's starter-test; MySQL/Redis/JUnit Testcontainers bundle; Redis starter |
| `apps:admin` | starter-test; MySQL/Redis/JUnit Testcontainers bundle |
| `apps:batch` | starter-test; MySQL/Redis/JUnit Testcontainers bundle |
| `application:frontoffice` | Spring Boot starter-test, ArchUnit 1.5.0, Platform launcher |
| `application:admin`, `application:system` | no explicit test library yet |
| `domain` | JUnit Jupiter + Platform launcher |
| `infrastructure` | starter-test, Redis starter, Testcontainers bundle, JPA test runtime, Spring Cloud/Feign test classpath |
| `support:security` | starter-test, Security, Web, ArchUnit 1.5.0, Platform launcher |
| `support:observability` | starter-test, Web, Platform launcher |
| `global-support` | JUnit Jupiter + Platform launcher |
| `module-contracts` | no explicit test dependency and no tracked test source |

`beat.spring-boot-app` adds starter-test and applies `beat.test`; `beat.test` currently configures only `useJUnitPlatform()`. This is the smallest central point for the authoring/task policy, while Property/Spring/MockK/Fixture Monkey remain opt-in at the consuming module.

The current full PR9 verification has one observed end-to-end sample of about 4m33s. There is no per-suite timing instrumentation or median baseline, so this document does not invent CI SLAs. Phase 0 records three warm/cold measurements before setting thresholds.

### 2.4 Production architecture and fidelity facts

- Booking creation reads authoritative Performance price under a pessimistic Performance lock in PR9, then locks Schedule and reserves inventory. Booking stores the calculated amount as its snapshot.
- Schedule owns booking close time and ticket allocation. `ScheduleJpaRepository` uses `PESSIMISTIC_WRITE` and database `CURRENT_TIMESTAMP(6) ... FOR UPDATE` for close-time correctness.
- Performance modification and Booking creation both acquire the authoritative Performance lock before the Schedule lock. PR11 adds the missing concurrent owner and changes active-Booking detection from a snapshot aggregate query to a current locking read after sorted Schedule locks.
- Ticket bulk changes acquire multiple database rows; the existing opposite-order integration test protects deterministic lock ordering.
- Guest sessions have Redis TTL 1,800 seconds; refresh tokens 1,209,600 seconds. Guest access throttling is a Redis Lua-scripted atomic counter with a bounded failure policy.
- `BookingCreated`, `MemberRegistered`, `PerformancePosterChanged`, and `TicketPaymentConfirmed` listeners use `AFTER_COMMIT`; existing tests do not collectively prove rollback suppression and exactly-once invocation after a successful local transaction.
- Kakao clients have hard-coded Feign base URLs. The existing adapter test mocks Feign clients and therefore does not protect HTTP path, form encoding, authorization header, or response deserialization.
- Slack webhook URLs are property-driven and can be tested with a mock HTTP server. S3 uses the AWS SDK and currently has an adapter unit test; LocalStack is not justified by current risk evidence.
- API security has explicit public paths, an admin-only `/api/admin/**` rule, and authenticated defaults. Admin app permits health/observability and non-prod Swagger, then requires `ROLE_ADMIN` for everything else.

## 3. Current Test Inventory

### 3.1 Classification legend

| Decision | Meaning |
|---|---|
| KEEP | Purpose and fidelity remain correct. Style conversion may be deferred; no behavioral redesign required. |
| REWRITE | Risk remains, but authoring style, isolation, fixture, transaction, or infrastructure fidelity changes. |
| MERGE | Risk remains, but moves into a single stronger owner; old file is removed only after the target passes. |
| DELETE | No independent production risk remains, or compiler/Gradle/a stronger semantic test replaces it. |

Every `DELETE` or `MERGE` row below names its replacement owner. “NEW” means the file does not exist yet.

Inventory field defaults, verified across all 111 tracked test sources:

- **Framework:** every executable test is JUnit Jupiter/JUnit Platform; there is no Kotest test. The one non-test source is `admin/src/test/kotlin/com/beat/admin/support/MockitoKotlinExtensions.kt`.
- **Spring:** default is no Spring context. Full-context exceptions are the subclasses of `AbstractIntegrationTest`, `AbstractAdminIntegrationTest`, `AbstractBatchIntegrationTest`, and `RedisRefreshTokenAdapterIntegrationTest`; each is named in the tables. There are no Web/JPA/Redis slice annotations.
- **External infrastructure:** default is none. Real MySQL/Redis use is stated in the relevant row. No tracked test calls a live Kakao, Slack, SMS, or S3 endpoint.
- **Test doubles:** default is direct real object construction. Rows marked Mockito/mock use framework doubles; 37 tracked sources import Mockito or a Mockito-based extension/configuration. The rewrite decision does not preserve mocks unless the boundary rule in §7 requires one.
- **Transactions:** only `BookerBookingQueriesIntegrationTest.kt` and `ReadPortJdslExecutionIntegrationTest.kt` are class-level transactional. `GuestBookingServiceConcurrencyTest.java` applies `@Transactional` to setup only, not concurrent workers. Other textual `@Transactional` matches occur inside source-scanning assertions, not test transactions.
- **Flakiness:** default is deterministic in-process execution. Known elevated risks are fixed sleep in Schedule lock waiting, random guest credentials, thread scheduling/deadlock timeouts, wall-clock JWT/TTL checks, and shared/cached Spring-container lifetime. These are called out in their row or target design.

### 3.2 Apps API tests

| Current path | Current mechanism / risk | Fidelity or duplicate issue | Decision and final owner |
|---|---|---|---|
| `apis/src/test/java/com/beat/apis/GuestAuthRedisIntegrationTest.java` | Full context + real Redis; guest session round trip, legacy alias, throttle limit/reset | Missing TTL and concurrent Lua atomicity; shares Redis setup with refresh-token tests | REWRITE into Redis slice specs; guest TTL/throttle risk remains here |
| `apis/src/test/java/com/beat/apis/booking/GuestBookingServiceConcurrencyTest.java` | Full context + real MySQL; 30 concurrent guests, capacity and final rows | Random credentials and broad context; valuable production transaction topology | REWRITE as deterministic concurrency spec; overselling owner |
| `apis/src/test/java/com/beat/apis/file/api/FileControllerTest.java` | Direct controller call with Mockito | Does not test HTTP binding, validation, serialization, or security | MERGE into File `@WebMvcTest`; controller contract owner |
| `apis/src/test/java/com/beat/apis/support/AbstractIntegrationTest.java` | Shared full context, static MySQL/Redis manual `.start()` | Context is too broad; container lifetime can outlive class incorrectly | DELETE after slice/acceptance Spring-managed configs replace all subclasses |
| `apis/src/test/java/com/beat/apis/support/DatabaseSessionTimeZoneTest.java` | Real MySQL session-time-zone contract | Valuable database/runtime contract, not a business rule | REWRITE as focused persistence configuration spec |
| `apis/src/test/kotlin/com/beat/apis/ApisApplicationTest.kt` | Source/resource/deployment string assertions | Mixes compile boundary, resources, Docker and naming | MERGE: Gradle/compiler guards + focused boot/deployment contract tests |
| `apis/src/test/kotlin/com/beat/apis/ApisArchitectureGuardTest.kt` | Source scanning for architectural shape | Brittle and weaker than Gradle/compiler/ArchUnit | DELETE after semantic guards pass |
| `apis/src/test/kotlin/com/beat/apis/ApisDtoJsonContractTest.kt` | ObjectMapper JSON compatibility checks | Valuable aliases/envelopes but duplicates some handler assertions | MERGE into API compatibility/Web slices plus OpenAPI diff |
| `apis/src/test/kotlin/com/beat/apis/ApisModuleContextBootTest.kt` | Full app boot, Swagger group, bean presence/exclusion | Valid composition smoke; contains assertions better owned by boundaries | REWRITE to one API boot smoke; move contract details to slices/guards |
| `apis/src/test/kotlin/com/beat/apis/boundary/DomainApplicationCodeBoundarySnapshotTest.kt` | Source snapshot of error/result ownership | String scanning and package-shape coupling | DELETE after application failure/API mapping ArchUnit + compiler gates |
| `apis/src/test/kotlin/com/beat/apis/exception/FrontofficeExceptionHttpContractTest.kt` | Exception-to-HTTP status/body contract | Correct risk, but not actual controller/security request pipeline | MERGE into parameterized Web slice error contract |
| `apis/src/test/kotlin/com/beat/apis/query/BookerBookingQueriesIntegrationTest.kt` | Real MySQL query mapping, stored amount and nullable account | Good fidelity; broad inherited context | REWRITE as focused MySQL persistence/query slice |
| `apis/src/test/kotlin/com/beat/apis/query/ReadPortJdslExecutionIntegrationTest.kt` | Real MySQL JDSL execution and mapping | Good risk owner; broad inherited context | REWRITE as focused persistence slice |
| `apis/src/test/kotlin/com/beat/apis/schedule/ScheduleBookingAvailabilityIntegrationTest.kt` | Real MySQL DB clock and close-time recheck after lock wait | Uses fixed `Thread.sleep`, producing flakiness and excess duration | REWRITE with deterministic lock/latch handshake; DB-clock owner |
| `apis/src/test/kotlin/com/beat/apis/ticket/TicketBulkLockOrderingIntegrationTest.kt` | Real MySQL, opposite ticket order, deadlock/timeout assertion | High-value concurrency risk; broad setup | REWRITE as dedicated concurrency spec; lock-order owner |
| `apis/src/test/kotlin/com/beat/apis/ticket/facade/TicketFacadeTest.kt` | Facade delegation with mocks | Implementation wiring only | DELETE after Ticket Web slice and application orchestration tests pass |

### 3.3 Application frontoffice tests

| Current path | Current mechanism / risk | Fidelity or duplicate issue | Decision and final owner |
|---|---|---|---|
| `application/frontoffice/src/test/java/com/beat/application/frontoffice/booking/booker/BookerBookingQueryServiceTest.java` | Mockito query orchestration/mapping | Important consumer output but Java/mock-heavy | REWRITE as no-Spring FunSpec with fake reader; SQL mapping stays persistence slice |
| `application/frontoffice/src/test/java/com/beat/application/frontoffice/booking/booker/BookingCancelServiceTest.java` | Mockito cancellation/refund authorization and state flow | Repeats aggregate transition edge cases | REWRITE; keep ownership/orchestration only, merge transition matrix into Booking domain spec |
| `application/frontoffice/src/test/java/com/beat/application/frontoffice/booking/booker/BookingCreationStatusServiceTest.java` | Mockito create/member/guest status and persistence flow | Repeats price/inventory behavior that needs authoritative integration | REWRITE; retain branching/event/snapshot orchestration, move lock outcome to concurrency suite |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/architecture/FrontofficeApplicationArchitectureTest.kt` | ArchUnit module/package constraints | Contains both valid dependency rules and brittle taxonomy/naming rules | REWRITE; retain semantic cross-capability/API and forbidden-framework rules only |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/auth/command/AuthenticationCommandServiceTest.kt` | No-Spring auth branching/failure mapping | Correct level; mock-heavy | REWRITE with real value objects/simple fakes, MockK only token/session boundaries |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/auth/command/LoginSessionIssuingServiceTest.kt` | Token/session issue and cleanup orchestration | Correct level | REWRITE as FunSpec; Redis semantics remain Redis slice |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/booking/booker/credential/GuestBookingCredentialAuthenticatorTest.kt` | BCrypt/legacy credential match and upgrade orchestration | Correct split between support-security primitive and Booking persistence action | REWRITE; security algorithm tests remain support-security |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/exception/ApplicationErrorCodeContractTest.kt` | Stable application failure code contract | Valuable public application language | REWRITE as FunSpec; Web mapping separately owns HTTP |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/member/command/SocialLoginCommandServiceTest.kt` | Social login orchestration and event/session outcome | Correct risk owner; uses doubles | REWRITE with simple fakes and boundary MockK |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/member/command/SocialLoginMemberResolverTest.kt` | Duplicate identity/race recovery decisions | Requires DB uniqueness proof in addition to unit branching | REWRITE unit policy + NEW real-MySQL uniqueness concurrency spec |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/performance/maker/command/FileCommandServiceTest.kt` | Storage use-case mapping/failure | Correct application boundary | REWRITE; protocol stays S3 adapter unit |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/performance/maker/command/PerformanceImageKeyTest.kt` | Deterministic image-key policy | Pure utility/policy, correct level | REWRITE as FunSpec; consider MERGE with File command spec only if same change reason remains |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/performance/maker/command/ScheduleSynchronizerTest.kt` | Sorted Schedule locking and active-booking guard | Critical ordering policy but not DB deadlock proof | REWRITE unit ordering; pair with real-MySQL Performance/Booking concurrency owner |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/ticket/maker/command/TicketCommandServiceTest.kt` | Ticket ownership, transitions, repository/event orchestration | Large mock surface and duplicates domain transitions | SPLIT/REWRITE: application orchestration owner + domain transition owner + MySQL lock owner |
| `application/frontoffice/src/test/kotlin/com/beat/application/frontoffice/ticket/maker/query/TicketQueryServiceTest.kt` | Maker query authorization/mapping/order | Correct consumer-use-case level | REWRITE with fake reader; actual SQL/order stays persistence slice |

### 3.4 Domain tests

| Current path | Current mechanism / risk | Fidelity or duplicate issue | Decision and final owner |
|---|---|---|---|
| `core/domain/src/test/java/com/beat/domain/booking/BookingDomainInvariantTest.java` | Broad Booking count/status/snapshot/privacy/rehydration transition matrix | Valuable but Java and mixes unrelated privacy check | REWRITE to Booking FunSpec; privacy may remain shared only if not duplicated |
| `core/domain/src/test/java/com/beat/domain/common/DomainEntityEqualityTest.java` | ID-based equality | Duplicated by Kotlin behavior test | MERGE into one Kotlin equality spec |
| `core/domain/src/test/java/com/beat/domain/performance/PerformanceChildOwnershipTest.java` | Child entity ownership/equality behavior | Narrow but valid aggregate ownership risk | MERGE into Performance aggregate spec |
| `core/domain/src/test/java/com/beat/domain/performance/PerformanceDomainInvariantTest.java` | price/change/delete/active-booking invariants | Correct owner | REWRITE as FunSpec with boundary property candidates |
| `core/domain/src/test/java/com/beat/domain/performance/PerformanceValueObjectTest.java` | running time, period, price/account validation | Correct owner | REWRITE; apply property testing only to stable numerical/date invariants |
| `core/domain/src/test/java/com/beat/domain/promotion/PromotionDomainServiceTest.java` | carousel eligibility/order/capacity | Correct owner | REWRITE as FunSpec; no Spring |
| `core/domain/src/test/java/com/beat/domain/schedule/ScheduleDomainInvariantTest.java` | capacity/allocation/window/close-date/rehydration invariants | Correct owner | REWRITE as FunSpec + selective property |
| `core/domain/src/test/java/com/beat/domain/schedule/ScheduleDomainServiceTest.java` | schedule sequencing/max/availability | Some overlap with aggregate capacity assertions | MERGE duplicate capacity assertions; REWRITE independent multi-Schedule policy |
| `core/domain/src/test/kotlin/com/beat/domain/common/DomainEntityEqualityBehaviorTest.kt` | ID-based equality behavior | Duplicate Java file | REWRITE as sole equality owner |
| `core/domain/src/test/kotlin/com/beat/domain/common/SensitiveDomainToStringTest.kt` | credential/payment privacy in `toString` | Cross-domain privacy guard is independently valuable | REWRITE; keep only sensitive fields not already asserted elsewhere |

### 3.5 Infrastructure tests

| Current path | Current mechanism / risk | Fidelity or duplicate issue | Decision and final owner |
|---|---|---|---|
| `core/infra/src/test/java/com/beat/infra/persistence/member/mapper/MemberPersistenceMapperTest.java` | entity/domain mapping | Pure deterministic mapping | REWRITE as FunSpec; mapping only |
| `core/infra/src/test/java/com/beat/infra/persistence/member/repository/MemberRepositoryImplTest.java` | Mockito repository delegation/error mapping | Does not prove MySQL unique/query semantics | MERGE into real-MySQL persistence slice; keep only adapter-specific error translation |
| `core/infra/src/test/java/com/beat/infra/persistence/performance/mapper/PerformancePersistenceMapperTest.java` | aggregate mapping | Correct pure mapping risk | REWRITE |
| `core/infra/src/test/java/com/beat/infra/persistence/schedule/mapper/SchedulePersistenceMapperTest.java` | Schedule mapping/rehydration | Correct mapping plus invalid persisted-state path | REWRITE; invalid rehydration also owned by domain spec |
| `core/infra/src/test/java/com/beat/infra/persistence/user/mapper/UsersPersistenceMapperTest.java` | User mapping | Correct pure mapping risk | REWRITE |
| `core/infra/src/test/kotlin/com/beat/infra/config/TaskExecutorConfigTest.kt` | async executor/MDC/error configuration | Runtime technical contract | REWRITE as focused config test |
| `core/infra/src/test/kotlin/com/beat/infra/external/notification/sms/TicketPaymentConfirmedEventListenerTest.kt` | AFTER_COMMIT annotation, message construction, failure swallowing | Annotation reflection does not prove commit semantics | MERGE: transaction-event integration owns commit/rollback; adapter unit owns payload/failure |
| `core/infra/src/test/kotlin/com/beat/infra/external/social/kakao/KakaoSocialLoginAdapterTest.kt` | Mocked Feign success/error mapping | Does not protect HTTP protocol | REWRITE adapter unit + NEW mock-server Feign contract after test-configurable base URL exists |
| `core/infra/src/test/kotlin/com/beat/infra/external/storage/s3/S3FileStorageAdapterTest.kt` | AWS SDK request/key/error mapping with mock | Appropriate boundary unit; LocalStack not justified | REWRITE as FunSpec, retain SDK seam |
| `core/infra/src/test/kotlin/com/beat/infra/persistence/booking/mapper/BookingPersistenceMapperTest.kt` | amount/payment/account/identity mapping | Correct snapshot mapping risk | REWRITE |
| `core/infra/src/test/kotlin/com/beat/infra/persistence/booking/repository/BookingRepositoryScalarLookupTest.kt` | Scalar identity/performance lookup | Mocked repository cannot prove query shape | MERGE into real-MySQL Booking repository slice |
| `core/infra/src/test/kotlin/com/beat/infra/persistence/cast/mapper/CastPersistenceMapperTest.kt` | Cast mapping | Correct pure mapping | REWRITE |
| `core/infra/src/test/kotlin/com/beat/infra/persistence/performance/repository/query/PerformancePeriodReadSupportTest.kt` | period query support | If mocked, insufficient for SQL/JDSL semantics | MERGE into real-MySQL Performance query slice |
| `core/infra/src/test/kotlin/com/beat/infra/persistence/promotion/entity/PromotionJpaEntityContractTest.kt` | `open`/accessor/JPA shape contract | Compiler/JPA slice protects actual need | DELETE after Promotion persistence slice boots and persists |
| `core/infra/src/test/kotlin/com/beat/infra/persistence/promotion/mapper/PromotionPersistenceMapperTest.kt` | Promotion mapping | Correct mapping risk | REWRITE |
| `core/infra/src/test/kotlin/com/beat/infra/persistence/staff/mapper/StaffPersistenceMapperTest.kt` | Staff mapping | Correct mapping risk | REWRITE |
| `core/infra/src/test/kotlin/com/beat/infra/redis/auth/AuthRedisTypeAliasCompatibilityTest.kt` | legacy Redis type aliases | Important rolling-data compatibility | MERGE into real Redis serialization compatibility suite |
| `core/infra/src/test/kotlin/com/beat/infra/redis/auth/refreshtoken/RedisRefreshTokenAdapterIntegrationTest.kt` | Full context + per-class container; TTL/serialization/delete/legacy | Correct risk, wrong context/container lifecycle | REWRITE as Redis slice with Spring-managed container when context-cached |
| `core/infra/src/test/kotlin/com/beat/infra/redis/auth/refreshtoken/RedisRefreshTokenAdapterTest.kt` | Mocked Redis repository delegation | Duplicates real adapter integration | DELETE after Redis slice covers save/find/delete/error mapping |

### 3.6 Support, observability, and shared utility tests

| Current path | Current mechanism / risk | Fidelity or duplicate issue | Decision and final owner |
|---|---|---|---|
| `gateway/src/test/kotlin/com/beat/support/security/GatewayConfigGroupTest.kt` | Source/config grouping assertions | Mixes architecture and config details | MERGE into support boot/config tests + Gradle/ArchUnit |
| `gateway/src/test/kotlin/com/beat/support/security/architecture/SupportSecurityArchitectureTest.kt` | ArchUnit support boundaries | Some valid framework ownership rules | REWRITE semantic rules only; no package-depth/naming checks |
| `gateway/src/test/kotlin/com/beat/support/security/authentication/internal/CurrentMemberArgumentResolverTest.kt` | principal/null/role resolution | Correct technical unit | REWRITE as FunSpec |
| `gateway/src/test/kotlin/com/beat/support/security/authentication/internal/JwtAuthenticationFilterTest.kt` | bearer parsing, principal and failure behavior | Correct technical unit, but endpoint authorization is elsewhere | REWRITE; Web security matrix owns route decisions |
| `gateway/src/test/kotlin/com/beat/support/security/authentication/internal/SecurityMdcLoggingFilterTest.kt` | MDC population/cleanup | Correct technical unit | REWRITE |
| `gateway/src/test/kotlin/com/beat/support/security/jwt/internal/JwtStartupValidationTest.kt` | invalid secret/config fail-fast | Correct startup risk | REWRITE |
| `gateway/src/test/kotlin/com/beat/support/security/jwt/internal/JwtTokenProviderTest.kt` | token issue/parse/expiry/role | Correct security primitive | REWRITE; time-controlled fixture required |
| `gateway/src/test/kotlin/com/beat/support/security/password/internal/BCryptPasswordHasherTest.kt` | BCrypt match, upgrade, legacy compatibility | Correct support-security API risk | REWRITE; no application output-port abstraction |
| `global-support/src/test/kotlin/com/beat/global/support/utils/ImageKeyExtractorTest.kt` | deterministic key parsing | Correct pure utility; ownership may move later | REWRITE; move with production owner, not as test-only relocation |
| `observability/src/test/kotlin/com/beat/observability/logging/Log4j2PatternContractTest.kt` | log pattern/resource contract | Correct runtime compatibility risk | REWRITE as focused resource contract |
| `observability/src/test/kotlin/com/beat/observability/logging/MdcTaskDecoratorTest.kt` | async MDC copy/cleanup | Correct technical unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/logging/access/AccessLogAsyncListenerTest.kt` | async request lifecycle logging | Correct servlet integration unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/logging/access/AccessLogEmitterTest.kt` | access log payload/emission | Correct technical unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/logging/coroutines/MdcCoroutineContextTest.kt` | coroutine MDC propagation | Correct technical unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/logging/exception/ExceptionCaptureResolverTest.kt` | exception classification/redaction | Correct technical unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/logging/filter/BaseMdcLoggingFilterTest.kt` | MDC lifecycle | Correct technical unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/logging/interceptor/RoutePatternMdcInterceptorTest.kt` | route pattern extraction | Correct MVC technical unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/sentry/BeatSentryEventProcessorTest.kt` | Sentry event tags/redaction | Correct adapter unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/sentry/BeatSentryMetricsTest.kt` | Sentry metric naming/tags | Correct adapter unit | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/sentry/SentryConfigTest.kt` | environment/config behavior | Correct focused config risk | REWRITE |
| `observability/src/test/kotlin/com/beat/observability/tracing/MicrometerTraceContextResolverTest.kt` | trace/span resolution | Correct technical unit | REWRITE |

### 3.7 Admin tests

| Current path | Current mechanism / risk | Fidelity or duplicate issue | Decision and final owner |
|---|---|---|---|
| `admin/src/test/kotlin/com/beat/admin/AdminApplicationTest.kt` | source/resource architecture assertions | Brittle mixed risk | MERGE into Gradle/ArchUnit + focused boot/deployment tests |
| `admin/src/test/kotlin/com/beat/admin/AdminArchitectureGuardTest.kt` | source scanning | Weaker than compiler/semantic rule | DELETE after replacement guards pass |
| `admin/src/test/kotlin/com/beat/admin/AdminModuleContextBootTest.kt` | Admin context/Swagger bean smoke | Valid composition risk | REWRITE to one Admin boot smoke |
| `admin/src/test/kotlin/com/beat/admin/HikariPoolSizeResolutionTest.kt` | datasource pool configuration resolution | Correct runtime risk | REWRITE as focused config spec |
| `admin/src/test/kotlin/com/beat/admin/application/AdminDtoJsonContractTest.kt` | Admin JSON compatibility | Correct API risk but partial | MERGE into Admin Web slices + Admin OpenAPI baseline |
| `admin/src/test/kotlin/com/beat/admin/application/AdminPromotionCommandServiceTest.kt` | promotion orchestration/policy via mocks | Correct use-case risk; needs MySQL race proof separately | REWRITE after workflow moves to `application:admin` |
| `admin/src/test/kotlin/com/beat/admin/application/AdminPromotionQueryServiceTest.kt` | Admin promotion projection/mapping | Correct consumer query risk | REWRITE with fake reader + real-MySQL query slice |
| `admin/src/test/kotlin/com/beat/admin/application/AdminUserQueryServiceTest.kt` | Admin user projection/mapping | Correct consumer query risk | REWRITE after workflow moves to `application:admin` |
| `admin/src/test/kotlin/com/beat/admin/exception/AdminGlobalExceptionHandlerTest.kt` | failure-to-HTTP mapping | Correct risk, not full HTTP pipeline | MERGE into Admin Web slice error contract |
| `admin/src/test/kotlin/com/beat/admin/promotion/api/response/CarouselPresignedUrlFindAllResponseTest.kt` | response JSON shape | Duplicate API compatibility scope | MERGE into Admin Promotion Web slice/OpenAPI |
| `admin/src/test/kotlin/com/beat/admin/promotion/facade/AdminPromotionFacadeTest.kt` | delegation only | No independent risk | DELETE after Web/use-case tests pass |
| `admin/src/test/kotlin/com/beat/admin/support/AbstractAdminIntegrationTest.kt` | full context + static MySQL manual start + `@JvmStatic` | Broad context and Kotlin interop debt | DELETE after admin slice/boot configs replace subclasses |
| `admin/src/test/kotlin/com/beat/admin/user/facade/AdminUserFacadeTest.kt` | delegation only | No independent risk | DELETE after Admin User Web/use-case tests pass |
| `admin/src/test/kotlin/com/beat/admin/support/MockitoKotlinExtensions.kt` | local Mockito compatibility helper | Obsolete after Kotlin test conversion; not a production risk | DELETE only when no tracked test references it |

### 3.8 Batch and root architecture tests

| Current path | Current mechanism / risk | Fidelity or duplicate issue | Decision and final owner |
|---|---|---|---|
| `batch/src/test/java/com/beat/batch/BatchActuatorHealthBootTest.java` | batch actuator/health boot | Valid deploy smoke | REWRITE as batch runtime smoke |
| `batch/src/test/java/com/beat/batch/BatchModuleContextBootTest.java` | full batch context | Valid composition smoke | REWRITE; keep one boot owner |
| `batch/src/test/java/com/beat/batch/BatchSchedulerOwnerBootTest.java` | scheduler/job bean ownership | Valid runtime wiring | MERGE into batch boot/scheduler wiring spec |
| `batch/src/test/java/com/beat/batch/booking/job/TicketCleanupJobTest.java` | job delegates to workflow | Thin delegation | MERGE into scheduler trigger + system use-case tests |
| `batch/src/test/java/com/beat/batch/promotion/job/PromotionMaintenanceJobTest.java` | job delegates to workflow | Thin delegation | MERGE into scheduler trigger + system use-case tests |
| `batch/src/test/java/com/beat/batch/support/AbstractBatchIntegrationTest.java` | full context + static MySQL manual start | Broad lifecycle | DELETE after batch runtime configuration replacement |
| `batch/src/test/kotlin/com/beat/batch/BatchApplicationTest.kt` | source/resource architecture assertions | Mixed and brittle | MERGE into Gradle/ArchUnit + boot/deployment tests |
| `batch/src/test/kotlin/com/beat/batch/BatchArchitectureGuardTest.kt` | source scanning | Weaker than compiler/semantic guards | DELETE after replacement guards pass |
| `batch/src/test/kotlin/com/beat/batch/booking/facade/TicketCleanupFacadeTest.kt` | facade delegation | No independent risk | DELETE after system use-case test passes |
| `batch/src/test/kotlin/com/beat/batch/config/ScheduledTaskErrorHandlerTest.kt` | scheduler error logging/non-propagation | Correct runtime risk | REWRITE as FunSpec |
| `batch/src/test/kotlin/com/beat/batch/config/SchedulingConfigTest.kt` | scheduling executor/config | Correct runtime risk | REWRITE |
| `batch/src/test/kotlin/com/beat/batch/config/SchedulingErrorHandlingIntegrationTest.kt` | actual scheduling error handler wiring | Useful integration risk | REWRITE as focused context test, not full acceptance |
| `batch/src/test/kotlin/com/beat/batch/promotion/facade/PromotionMaintenanceFacadeTest.kt` | facade delegation | No independent risk | DELETE after system use-case test passes |
| `src/test/java/com/beat/RootRetirementContractTest.java` | scans legacy names/paths | Temporary migration assertion, no final runtime risk | DELETE when physical retirement is compiler/Gradle-proven |
| `src/test/java/com/beat/SharedBoundaryContractTest.java` | broad source boundary scans | Mixes valid boundaries with package strings | MERGE into Gradle dependency verification + focused ArchUnit |
| `src/test/java/com/beat/architecture/PromotionBoundaryTest.java` | Promotion package/dependency source scan | Temporary migration guard | DELETE after Admin Promotion module/ArchUnit guards pass |

### 3.9 `develop`-only path disposition

The following 22 files exist on `develop` but were moved, split, or superseded on the PR9 branch. Their risks are not discarded.

| `develop` path | PR9/current risk owner |
|---|---|
| `apis/src/test/java/com/beat/apis/ApisModuleContextBootTest.java` | `apis/.../ApisModuleContextBootTest.kt` |
| `apis/src/test/java/com/beat/apis/RefreshTokenRepositoryIntegrationTest.java` | `core/infra/.../RedisRefreshTokenAdapterIntegrationTest.kt` |
| `apis/src/test/java/com/beat/apis/booking/BookingCancelServiceTest.java` | `application/frontoffice/.../BookingCancelServiceTest.java` |
| `apis/src/test/java/com/beat/apis/booking/BookingCreationStatusServiceTest.java` | `application/frontoffice/.../BookingCreationStatusServiceTest.java` |
| `apis/src/test/java/com/beat/apis/booking/application/BookingRetrieveServiceBatchLookupTest.java` | `BookerBookingQueryServiceTest.java` + `BookerBookingQueriesIntegrationTest.kt` |
| `apis/src/test/java/com/beat/apis/file/application/FileCommandServiceTest.java` | `application/frontoffice/.../FileCommandServiceTest.kt` |
| `apis/src/test/java/com/beat/apis/member/AuthenticationServiceTest.java` | `AuthenticationCommandServiceTest.kt` + `LoginSessionIssuingServiceTest.kt` |
| `apis/src/test/java/com/beat/apis/member/SocialLoginServiceTest.java` | `SocialLoginCommandServiceTest.kt` + `SocialLoginMemberResolverTest.kt` |
| `apis/src/test/java/com/beat/apis/performance/PerformanceCastStaffBoundaryTest.java` | `ScheduleSynchronizerTest.kt` + frontoffice architecture guard |
| `apis/src/test/java/com/beat/apis/query/ReadPortJdslExecutionIntegrationTest.java` | Kotlin file at the same logical path |
| `apis/src/test/java/com/beat/apis/schedule/ScheduleBookingAvailabilityIntegrationTest.java` | Kotlin file at the same logical path |
| `apis/src/test/java/com/beat/apis/ticket/application/TicketServiceTest.java` | Ticket command/query specs + MySQL ordering spec |
| `apis/src/test/java/com/beat/apis/ticket/facade/TicketFacadeTest.java` | Kotlin facade test, ultimately deleted after stronger owners pass |
| `apis/src/test/kotlin/com/beat/apis/performance/application/PerformanceImageKeyTest.kt` | application frontoffice maker command test |
| `core/infra/src/test/java/com/beat/infra/external/social/kakao/KakaoSocialLoginAdapterTest.java` | Kotlin adapter test |
| `core/infra/src/test/java/com/beat/infra/persistence/booking/repository/query/MakerTicketQueriesOrderingContractTest.java` | real MySQL JDSL/Ticket query integration owner |
| `gateway/src/test/kotlin/com/beat/gateway/GatewayConfigGroupTest.kt` | support-security path-equivalent test |
| `gateway/src/test/kotlin/com/beat/gateway/authentication/internal/CurrentMemberArgumentResolverTest.kt` | support-security path-equivalent test |
| `gateway/src/test/kotlin/com/beat/gateway/authentication/internal/JwtAuthenticationFilterTest.kt` | support-security path-equivalent test |
| `gateway/src/test/kotlin/com/beat/gateway/authentication/internal/SecurityMdcLoggingFilterTest.kt` | support-security path-equivalent test |
| `gateway/src/test/kotlin/com/beat/gateway/jwt/internal/JwtStartupValidationTest.kt` | support-security path-equivalent test |
| `gateway/src/test/kotlin/com/beat/gateway/jwt/internal/JwtTokenProviderTest.kt` | support-security path-equivalent test |

### 3.10 Workspace-only drafts

These are user-owned untracked files. The implementation must not overwrite or stage them without an explicit adoption decision.

| Untracked path | Provisional decision |
|---|---|
| `apis/src/test/java/com/beat/apis/exception/ApiGlobalExceptionHandlerTest.java` | MERGE into Web slice error contract |
| `apis/src/test/java/com/beat/apis/member/MemberRegistrationConcurrencyTest.java` | DO NOT ADOPT AS-IS. Split the risk: PR13 real-MySQL persistence owns `uk_member_social_identity` and adapter exception translation; a later frontoffice use-case integration owner must prove the losing registration rolls back its newly created User through the production `MemberRegistrar` transaction. |
| `apis/src/test/java/com/beat/apis/ticket/application/event/TicketPaymentConfirmedEventListenerTest.java` | MERGE with tracked infrastructure listener/event transaction tests |
| `apis/src/test/kotlin/com/beat/apis/performance/PerformanceNearestDueDateTest.kt` | REVIEW then REWRITE/adopt with actual query/policy owner |
| `apis/src/test/kotlin/com/beat/apis/performance/PerformancePeriodFormatterTest.kt` | REVIEW then REWRITE/adopt as pure formatter test |
| `apis/src/test/kotlin/com/beat/apis/schedule/application/calculator/ScheduleDueDateCalculatorTest.kt` | REVIEW then REWRITE/adopt if production calculator remains |
| `apis/src/test/kotlin/com/beat/apis/ticket/api/TicketControllerTest.kt` | DELETE/replace with WebMvc slice; direct delegation has no independent risk |
| `batch/src/test/java/com/beat/batch/notification/DbJobQueueContentionProbeTest.java` | Keep outside normal PR suite as diagnostic/nightly only if the queue design lands |

## 4. Correctness/Risk Gaps

### 4.1 Risk ownership matrix

| Capability / production risk | Current protection | Desired owner | Level / infra | Decision |
|---|---|---|---|---|
| Booking count 1..10 and state transitions | Booking domain test + duplicated service assertions | Booking domain spec | Unit / none | REWRITE/MERGE |
| Booking amount snapshot from authoritative Performance price | service mocks + mapping/query checks | Booking creation integration + mapping spec | Use-case integration / MySQL | NEW/REWRITE |
| Legacy booking amount fallback using later price | partial query/mapping tests | compatibility query spec with legacy row fixture | Persistence/API compatibility / MySQL | NEW |
| Schedule allocation never exceeds capacity | Schedule domain + guest concurrency | Schedule domain for sequential invariant; Booking concurrency for race | Unit + Concurrency / MySQL | REWRITE |
| Booking close time evaluated by DB clock and rechecked after lock | availability integration | dedicated Schedule persistence/concurrency spec | Concurrency / MySQL | REWRITE without sleeps |
| Performance price modification vs Booking creation serialization | PR11 real-MySQL concurrency owner | dedicated cross-capability concurrency spec | Concurrency / MySQL | NEW completed; retain as correctness gate |
| Performance/Schedule/Booking lock order | partial unit ordering + isolated concurrency tests | one lock topology suite | Concurrency / MySQL | REWRITE/NEW |
| Ticket multi-row deadlock prevention | bulk lock integration | Ticket lock-order spec | Concurrency / MySQL | REWRITE |
| Home genre/projection composition and nearest-Schedule due-date ordering | no tracked Home test; workspace drafts cover only helpers | Home Booker application spec + Home query slice | Application + Persistence / MySQL | NEW |
| Home Promotion carousel ordering/projection | no Home test; Admin Promotion tests cover a different consumer | Home consumer query slice | Persistence / MySQL | NEW; do not reuse Admin DTO assertions |
| Duplicate social identity registration race | unit race recovery; untracked draft | Member repository constraint/translation spec + frontoffice registration transaction spec | Persistence integration + Use-case concurrency / MySQL | SPLIT: PR13 owns the database constraint; later transaction owner proves the losing User rollback |
| DB unique/index/query behavior | mocked repositories and JDSL integration | capability persistence slices | Persistence / MySQL | MERGE/REWRITE |
| Guest duplicate credential lookup determinism | application tests, incomplete DB contract | Booking repository/query spec | Persistence / MySQL | NEW |
| Rehydration purchase-count range validation | domain + mapper fragments | Schedule/Booking domain plus mapper compatibility | Unit | REWRITE |
| Guest session TTL and legacy serialization | round-trip/alias only | Guest Redis slice | Redis / Redis | EXPAND/REWRITE |
| Guest throttle atomicity under concurrency | sequential Redis test | Guest throttle Redis concurrency spec | Redis / Redis | NEW |
| Refresh-token TTL/delete/legacy compatibility | full-context Redis integration | Refresh-token Redis slice | Redis / Redis | REWRITE |
| JWT issue/expiry/role parsing | support-security units | support-security spec with controlled time | Unit | REWRITE |
| Public/member/admin/guest endpoint authorization | filter units only | API/Admin authorization matrix | Web Slice | NEW |
| Guest cookie mutation origin check | filter/config code, no complete endpoint matrix | Booking Web security slice | Web Slice | NEW |
| Request validation, status, error envelope, legacy JSON aliases | DTO and handler tests | capability Web slices + OpenAPI diff | Web Slice/API Compatibility | MERGE/REWRITE |
| OpenAPI breaking change | no baseline | generated general/admin spec diff | API Compatibility | NEW |
| Kakao HTTP path/form/header/response contract | mocked Feign adapter | mock-server client contract | External Adapter / mock HTTP server | NEW after configurable URL seam |
| Slack webhook payload/HTTP contract | payload construction only/none | mock-server external adapter spec | External Adapter / mock HTTP server | NEW, one representative per payload type |
| SMS payload/failure behavior | listener unit | adapter unit; no unproven network emulator | External Adapter unit | REWRITE |
| AFTER_COMMIT: rollback suppresses notification | annotation reflection only | transaction-event integration spec | Use-case integration / MySQL | NEW |
| S3 key/presign/metadata/error mapping | mocked SDK unit | S3 adapter spec | External Adapter unit | REWRITE; no LocalStack |
| API/Admin/Batch independent boot | context tests | one boot smoke per executable app | E2E Smoke | REWRITE |
| Domain framework freedom and module direction | source scans + local ArchUnit | Gradle/compiler + focused ArchUnit | Architecture | MERGE |
| Infrastructure implementation leakage into controllers | broad source scans | compiler/internal + ArchUnit for package access | Architecture | NEW/REWRITE |
| Schema migration replay | no migration system | none until migrations exist | N/A | NOT APPLICABLE |
| Logging/MDC/Sentry/trace contract | focused units | support-observability specs | Unit/config | REWRITE |
| Scheduler wiring/error handling | batch boot/config tests | batch runtime/config + system use-case tests | Focused context/unit | MERGE/REWRITE |

### 4.2 Correctness decisions required before implementation

1. **Resolved in PR11:** Performance modification이 먼저 serialize되면 Booking은 새 가격을 snapshot한다. Booking이 먼저면 기존 가격을 snapshot하고 active Booking 때문에 뒤이은 다른 가격 변경을 거부한다. 두 결과는 real-MySQL concurrency spec이 소유한다.
2. Define one canonical lock order across Performance, sorted Schedule rows, Booking, and Ticket rows. A test cannot legitimize the current order merely because it passes.
3. Define whether a legacy Booking row without stored amount is allowed indefinitely. If temporary, compatibility tests must carry a removal condition; if permanent, the authoritative fallback source and point-in-time semantics must be explicit.
4. Define deterministic behavior for duplicate guest credentials. Repository result ordering or uniqueness must be explicit; “first returned row” is not a contract.
5. Confirm exact notification delivery guarantee. Current `AFTER_COMMIT` plus best-effort catch proves at-most local invocation, not durable delivery. Tests must not claim outbox/exactly-once semantics that do not exist.

## 5. Target Test Portfolio

| Suite | Purpose / included risks | Excluded | Spring | Infra | Fixtures / doubles | Transaction policy | CI |
|---|---|---|---|---|---|---|---|
| Unit | Domain invariants, value objects, pure mapping/formatting, support utilities | SQL, Redis, HTTP, Spring wiring | No | None | named DSL; real object; selective property | none | PR Fast |
| Application | use-case branching, authorization decisions, event/output intent | repository query/lock semantics | No | None | real object -> fake -> MockK boundary | explicit fake state only | PR Fast |
| Web Slice | routing, validation, serialization, exception mapping, security matrix | DB/Redis/business invariant repetition | `@WebMvcTest` | none | request DSL, collaborator stub/fake | none | PR Fast |
| Persistence Slice | JPA/JDSL/JDBC mapping, constraints, query ordering, DB clock | end-to-end HTTP | minimal JPA context | MySQL | named entity/row DSL; no mocked repository | rollback allowed for non-concurrency leaf tests | PR Integration |
| Redis Slice | TTL, aliases, Lua atomicity, deletion | JWT logic, controller flow | minimal Redis context | Redis | deterministic keys/clock tolerance | explicit cleanup; no fake Redis | PR Integration |
| External Adapter | Kakao/Slack HTTP contract; S3/SMS adapter mapping | remote vendor availability | minimal client context when needed | mock HTTP server; no live vendor | recorded request assertions; stub server | none | PR Integration |
| Use-case Integration | transaction, repository composition, AFTER_COMMIT semantics | every controller permutation | `@SpringBootTest` only when required | MySQL/Redis selectively | named Kotlin fixture DSL; stable fake external adapters | production application transaction | PR Correctness |
| Acceptance | few critical Booker/Maker/Admin journeys through HTTP | exhaustive domain boundaries | `@BeatAcceptanceTest` | real MySQL/Redis; fake external vendors | journey DSL; public JSON, not internal DTO coupling | production request transactions; explicit cleanup | PR Correctness/main |
| Concurrency | overselling, lock order, unique races, DB close-time recheck | test-level rollback, sleeps as synchronization | real Spring | MySQL/Redis | deterministic IDs, barriers/latches, bounded timeout | no test-level `@Transactional`; worker calls production service | PR Correctness; periodic repetitions nightly |
| Architecture | dependency direction, framework leakage, cycles, controller-to-infra leakage | naming/package depth and empty package taxonomy | No | none | Gradle/compiler first; ArchUnit second | none | PR Fast |
| API Compatibility | generated OpenAPI breaking diff and selected legacy JSON aliases | consumer behavior not represented in spec | boot or focused generation task | none | checked-in normalized baseline | none | PR Fast |
| Authorization | actual path/method/role matrix for API/Admin | internal token crypto details | Web Slice | none | token/principal fixtures | none | PR Fast |
| E2E Smoke | api/admin/batch bootJar and minimal health/startup | business permutations | real boot | smallest required infra | stable environment | production-like | main/release |
| Migration | versioned schema upgrade and backward compatibility | absent today | N/A | MySQL when introduced | versioned fixtures | migration-managed | conditional future gate |
| Mutation | selected Booking/Schedule/Performance policies | whole repository | No | none | existing unit fixtures | none | optional nightly after baseline |

`Acceptance` is not a synonym for all full-context tests. Initial acceptance journeys are limited to: member Booking creation, guest Booking creation/retrieve/cancel or refund, Maker Performance create/modify with Schedule, Ticket payment/refund, member sign-up/refresh/sign-out, and one Admin Promotion/User journey after those lanes migrate. Each journey owns cross-layer composition only; detailed edge rules stay in their smaller owner.

## 6. KEEP / REWRITE / DELETE / MERGE Matrix

The file-level authoritative disposition is §3. This section states the execution rule by decision so a migration PR cannot remove protection prematurely.

| Decision | Included groups | Required replacement gate |
|---|---|---|
| KEEP | No current file is permanently exempt from Kotlin authoring migration. The *risks* kept unchanged are Ticket lock ordering, guest overselling, DB-clock close check, real JDSL execution, Redis legacy compatibility, three app boot smokes, and focused observability/security units. | Existing test remains green until its FunSpec/focused equivalent proves the same outcome. |
| REWRITE | Domain specs; frontoffice use-case specs; MySQL/Redis integration; security/observability units; runtime config/boot tests; S3/Kakao adapter tests | New test fails when the protected behavior is intentionally broken, then passes on unchanged production behavior. Old and new coexist for at least the migration commit boundary. |
| MERGE | duplicate domain equality/capacity assertions; DTO/handler JSON tests; mocked repository tests; event annotation tests; facade/job delegation tests; source architecture scans | A named stronger owner from §3 is green and the PR report lists the old assertions absorbed. |
| DELETE | facade-only delegation, mocked Redis duplication, JPA `open` shape, brittle source/package scans, temporary root retirement guards, Mockito Kotlin helper | Compiler/Gradle/ArchUnit or the listed semantic replacement is green. `rg` proves no remaining reference to deleted helpers/legacy test base. |

Deletion order is strict:

```text
identify risk -> add/convert target owner -> mutation/negative proof -> run both -> remove old owner -> rerun target + module check
```

No PR may delete a concurrency, compatibility, authorization, or failure-mapping assertion in the same commit that first invents its expected behavior. The expectation must be reviewed as a characterization decision first.

## 7. Fixture Strategy

### 7.1 Named Kotlin fixture DSL — default

Fixtures live with the risk owner and expose valid defaults plus only semantically relevant overrides.

```kotlin
booking(
    count = 2,
    status = BookingStatus.PAYMENT_COMPLETED,
)
```

Rules:

- Names use domain language (`paidBooking`, `bookableSchedule`, `makerPerformance`) rather than generic `FixtureFactory` or `TestDataBuilder` names.
- Default values are deterministic. IDs, timestamps, prices, and credentials are explicit when they affect the scenario.
- A fixture may not bypass public factories merely to make setup shorter, except a dedicated rehydration fixture testing persisted compatibility.
- Module-local fixtures remain local. Gradle `testFixtures` publication is added only after the same semantic fixture is duplicated across at least three consumer modules and ownership is clear.
- Acceptance/concurrency fixtures create data through real repositories or public setup APIs and clean it explicitly; they do not depend on test method rollback.

### 7.2 Fixture Monkey — selective

BEAT will not add Fixture Monkey in PR10. It is introduced in the Web/API compatibility PR only if the audit confirms repeated setup of large production DTO/nested graphs where most fields are irrelevant to the assertion.

Candidate uses:

- bulk serialization/deserialization compatibility for API/Admin response objects;
- validation exploration over request objects with many fields;
- non-semantic bulk rows for query pagination/load tests.

Forbidden uses:

- Booking amount, ticket count, role, ownership, Schedule capacity/close time, payment account, or state-transition values;
- acceptance and concurrency scenario-critical inputs;
- hiding invalid random defaults behind retries.

If admitted, `com.navercorp.fixturemonkey:fixture-monkey-starter-kotlin` is a test dependency only in the consuming app/infra module, not every project. One lane-local factory uses `KotlinPlugin`, non-nullable Kotlin properties are valid by default, nullable generation is explicit, and failed runs print/reuse a seed or the generated fixture. Version is pinned in `gradle/libs.versions.toml` after dependency compatibility verification; the official project currently documents 1.2.0, but the implementation PR must re-resolve it rather than copy this date-bound fact blindly.

### 7.3 Kotest Property — invariants only

Initial property candidates:

- Schedule allocation remains within `[0, totalTicketCount]` for generated valid reserve/cancel sequences.
- Booking purchase count accepts exactly its documented range and rejects outside values.
- Ticket price/running time numerical boundaries.
- Performance period start/end ordering.
- entity identity equality laws.

Property tests use bounded generators and report seeds. They do not replace named examples for `0`, `1`, maximum, and maximum+1. Fixture Monkey generates object graphs; Kotest Property quantifies an invariant. They are not interchangeable.

## 8. Spring, Kotest, and Testcontainers Design

### 8.1 Kotest platform decision

Dependencies added to the version catalog and convention plugin:

- `io.kotest:kotest-runner-junit5-jvm`
- `io.kotest:kotest-assertions-core-jvm`
- `io.kotest:kotest-property-jvm` only for modules with approved property tests
- `io.kotest:kotest-extensions-spring` only for Spring test modules
- `io.mockk:mockk` only for modules that require a boundary double

JUnit Jupiter and Mockito coexist during migration. They are removed from a module only after `git ls-files`/`rg` prove no authoring imports remain; JUnit Platform launcher stays.

Kotest policy:

- `FunSpec` is the only new style.
- Isolation stays explicitly `SingleInstance` initially, matching Kotest's current default. Shared mutable spec fields are forbidden; fresh SUT/fixture state is created in `beforeTest` or test body. `InstancePerRoot` is evaluated only from measured flakiness/boilerplate, not selected preemptively.
- Spring specs register `SpringExtension(SpringTestLifecycleMode.Test)` explicitly. Kotest 6.2.4 names its leaf-test lifecycle mode `Test`; leaf callback/rollback behavior is policy, not an accidental default.
- Concurrency specs do not use Spring test transactions, regardless of Leaf mode.
- JUnit and Kotest tag discovery is proven with a pilot before CI filtering is enabled. Gradle tasks propagate both JUnit Platform tag filters and Kotest tag configuration during coexistence.

Official Kotest 6.2 documentation confirms that FunSpec has no functional disadvantage to other styles, `SingleInstance` is the default isolation mode, Spring callback mapping is not one-to-one with nested functions, and the default `SpringTestLifecycleMode.Test` applies lifecycle callbacks to leaf tests. These framework facts support, but do not replace, BEAT's risk-based decision.

### 8.2 Spring slice rules

**Unit/Application:** direct constructor construction; no Spring annotation or context.

**Web:** start with `@WebMvcTest(controllers = [...])`. Import the real exception advice, argument resolver, JSON configuration, and security filter chain required by the endpoint. If a controller requires many bean overrides, first report the coupling; do not modify production architecture merely to make the slice prettier. Repeated per-class `@MockitoBean` combinations are avoided because they fragment Spring's context cache.

**JPA:** use `@DataJpaTest` plus real MySQL `@ServiceConnection`. Import only concrete custom JDSL/JDBC repository implementations required by the test. Do not replace MySQL with H2. Rollback is acceptable for ordinary query/mapping tests; database commit/lock/constraint races use the concurrency suite instead.

**Redis:** use `@DataRedisTest` plus real Redis and explicit import of repositories/adapters/scripts. TTL assertions use bounded tolerance, not exact wall-clock equality. Atomic Lua tests use concurrent clients and final Redis state.

**Use-case/Acceptance:** use full context only for actual transaction/composition/user-journey risk. `NEW apis/src/test/kotlin/com/beat/apis/support/BeatAcceptanceTest.kt` standardizes API app class, random port, test profile, stable external fakes, and common container configuration. Admin/Batch get a separate meta-annotation only after a second real acceptance need appears.

### 8.3 Context-cache policy

Before/after metrics record unique Spring context cache keys, hit/miss counts, and suite duration. The following are centralized because they change the cache key:

- `@ActiveProfiles` values;
- `@ContextConfiguration` classes;
- inline `@TestPropertySource`/`properties`;
- `@MockitoBean`/bean override sets;
- `@DynamicPropertySource` and other context customizers.

The acceptance suite uses one API meta-annotation/configuration. Test-specific behavior comes from resettable fake adapters or data, not a unique application context. `@DirtiesContext` requires a documented reason.

### 8.4 Testcontainers lifecycle

Current static/manual `.start()` bases are transitional. For context-cached Spring tests:

- `NEW apis/src/test/kotlin/com/beat/apis/support/BeatTestContainersConfig.kt` declares MySQL and Redis as `@Bean` methods in `@TestConfiguration(proxyBeanMethods = false)` with `@ServiceConnection`.
- `NEW core/infra/src/test/kotlin/com/beat/infra/support/InfrastructureTestContainersConfig.kt` provides only the container needed by a persistence/Redis slice; it does not start both services for every test.
- Redis `GenericContainer` service connections name the service explicitly when required by Spring Boot's connection-detail resolution.
- Spring owns container lifetime when the cached context depends on it. Standalone non-Spring Testcontainers may retain JUnit lifecycle if no cached context survives the class.

Spring Boot's official documentation warns that a JUnit-managed container can stop while a cached `ApplicationContext` still references it; this is the concrete reason for Spring-managed lifecycle here, not a blanket rule that every container is a bean.

### 8.5 Concurrency harness

One reusable test utility may provide barriers, executor shutdown, bounded futures, and captured exceptions. It must not encode Booking domain expectations.

Concurrency rules:

- no `Thread.sleep` as synchronization;
- no test-level `@Transactional`;
- committed setup visible to workers before barrier release;
- workers invoke the same application service/transaction proxy used in production;
- deterministic IDs, credentials, and input order;
- assert business outcome, final database/Redis state, timeout, and absence/presence of deadlock as appropriate;
- retry/repetition is a detector, never a mechanism that converts a failed run to green.

### 8.6 Time, async, and flaky-test policy

- Pure domain/application time rules receive an explicit `java.time.Clock` or explicit instant/date input when the production API already supports it or a correctness fix approves the seam. Do not add a `ClockPort` ceremony abstraction.
- Database-close semantics intentionally use MySQL time and are asserted with a bounded before/after window plus lock synchronization, not a JVM fake clock.
- TTL assertions accept a documented tolerance and assert monotonic decrease/expiry behavior.
- Async/event tests use latches, futures, or bounded polling against an observable state. No fixed sleep is accepted as the success criterion.
- A flaky critical test is not disabled or given automatic success retries. Quarantine is time-bounded, linked to an owner/issue, and accompanied by a smaller deterministic guard when possible.
- Nightly repetition may expose races, but a single failed iteration fails/reports the suite; repetition never hides failure.
- Coverage is diagnostic only. No line/branch percentage is a DoD. A coverage increase cannot justify a test without a unique risk owner, while a drop over a critical package triggers review rather than an automatic rewrite mandate.

## 9. Architecture, API, Security, and Migration Guards

### 9.1 Strongest-enforcement order

| Rule | Primary enforcement | ArchUnit only if needed |
|---|---|---|
| `domain` cannot see Spring/JPA/Web/Redis/application/apps | Gradle dependency graph + compile classpath | forbidden package imports as defense in depth |
| application lanes cannot depend on each other/infrastructure | Gradle project dependencies | cycles and same-project logical capability access |
| infrastructure cannot depend on apps | Gradle | no |
| apps web/controller cannot access infrastructure implementations | Kotlin `internal`/compiler where possible | package-level dependency when composition config must see public config API |
| Booking may not call Performance concrete services/query internals | minimal public API/compiler | explicit cross-capability allowed-package ArchUnit rule |
| Domain/Application tests must not boot Spring | build convention/source-set dependency absence | annotation rule if dependencies are transitively visible |
| no module cycle | Gradle graph verification | internal package cycle detection inside a physical project |

ArchUnit will not enforce `Capability/Actor/Command|Query` directory depth, class suffixes, or empty package presence. Those are taxonomy review concerns, not runtime correctness.

### 9.2 OpenAPI gate

1. Boot API and Admin with deterministic documentation configuration.
2. Generate and normalize the existing `general` and `admin` groups.
3. Check in `NEW docs/openapi/baseline/general.json` and `NEW docs/openapi/baseline/admin.json` only after manual review against current deployed routes.
4. A pinned `oasdiff breaking --fail-on ERR` invocation compares baseline to PR output. Pin by checksum/container digest or version; never use `latest`.
5. Intentional breaking changes require explicit compatibility/versioning approval and baseline update in the same PR.
6. Retain focused JSON tests for behaviors OpenAPI cannot express: legacy aliases, exact error envelope, cookie attributes, and nullable/omitted distinctions.

OpenAPI diff is selected over Pact because no repository evidence identifies maintained external consumer contracts or a broker. Pact may be reconsidered when a concrete consumer and ownership workflow exist.

### 9.3 Authorization matrix

The initial matrix comes from the actual controller mappings and `ApisSecurityConfig`/`AdminSecurityConfig`. “Allow” below means the filter-chain decision only; ownership/business authorization remains an application risk.

| Actual method/path | Anonymous | `ROLE_MEMBER` | `ROLE_ADMIN` | Owner |
|---|---:|---:|---:|---|
| `POST /api/users/sign-up` | allow | allow | allow | Member Web slice |
| `GET /api/users/refresh-token` | allow | allow | allow | Member Web slice + Redis/application |
| `POST /api/users/sign-out` | deny | allow | allow | Member Web slice |
| `GET /api/main` | allow | allow | allow | Home Web slice |
| `POST /api/bookings/guest` | allow subject to guest origin/cookie rules | allow | allow | Booking Web security slice |
| `POST /api/bookings/guest/retrieve` | allow subject to guest credential/throttle rules | allow | allow | Booking Web + application/Redis |
| `POST /api/bookings/member` | deny | allow | allow at filter; business rule may reject | Booking Web + application ownership |
| `GET /api/bookings/member/retrieve` | deny | allow | allow at filter; business rule may reject | Booking Web + application ownership |
| `PATCH /api/bookings/refund` | allow at filter; guest cookie/origin path | allow with member context | allow at filter; business rule decides | Booking Web + application ownership |
| `PATCH /api/bookings/cancel` | allow at filter; guest cookie/origin path | allow with member context | allow at filter; business rule decides | Booking Web + application ownership |
| `GET /api/performances/detail/{performanceId}` | allow | allow | allow | Performance Booker Web slice |
| `GET /api/performances/booking/{performanceId}` | allow | allow | allow | Performance Booker Web slice |
| `POST /api/performances` | deny | allow at filter + Maker ownership | allow at filter + business policy | Performance Maker Web/application |
| `PUT /api/performances` | deny | allow at filter + Maker ownership | allow at filter + business policy | Performance Maker Web/application |
| `DELETE /api/performances/{performanceId}` | deny | allow at filter + Maker ownership | allow at filter + business policy | Performance Maker Web/application |
| `GET /api/performances/{performanceId}` | deny | allow | allow | Performance Maker Web/application |
| `GET /api/performances/user` | deny | allow | allow at filter | Performance Maker Web/application |
| `GET /api/schedules/{scheduleId}/availability` | allow | allow | allow | Schedule Booker Web slice |
| `GET /api/files/presigned-url` | deny | allow | allow at filter | File/Performance Maker Web/application |
| `GET /api/tickets/{performanceId}` | deny | allow | allow at filter | Ticket Maker Web/application |
| `GET /api/tickets/search/{performanceId}` | deny | allow | allow at filter | Ticket Maker Web/application |
| `PUT /api/tickets/update` | deny | allow + ownership | allow at filter + business policy | Ticket Maker Web/application |
| `PUT /api/tickets/refund` | deny | allow + ownership | allow at filter + business policy | Ticket Maker Web/application |
| `PUT /api/tickets/delete` | deny | allow + ownership | allow at filter + business policy | Ticket Maker Web/application |
| API-lane `/api/admin/**` matcher | deny | deny | allow | API security config spec; no current API controller was found under this path |
| `GET /api/admin/users` | deny | deny | allow | Admin User Web/security slice |
| `GET /api/admin/carousels/presigned-url` | deny | deny | allow | Admin Promotion Web/security slice |
| `GET /api/admin/banner/presigned-url` | deny | deny | allow | Admin Promotion Web/security slice |
| `GET /api/admin/carousels` | deny | deny | allow | Admin Promotion Web/security slice |
| `PUT /api/admin/carousels` | deny | deny | allow | Admin Promotion Web/security slice |
| health/prometheus | allow at configured management path | allow | allow | app boot/security config |
| Swagger/OpenAPI paths | allow outside `prod`, deny/absent according to prod policy | same | same | profile-specific security/boot spec |

`/api/notifications/**` is present in the API allowlist but no current controller mapping was found. It is a security-audit item, not a presumed feature: retain an explicit characterization until ownership confirms removal or a hidden/runtime mapping. A wildcard-only authorization test is insufficient. Web security owns 401/403 and filter behavior; application tests own resource ownership and actor-specific business authorization.

### 9.4 Schema migration gate

No Flyway/Liquibase/versioned migrations exist. Therefore:

- do not create a fake “migration test” over `ddl-auto=create-drop`;
- keep a MySQL mapping/schema boot smoke for current entity compatibility;
- the first PR introducing versioned migrations must add blank-to-head and previous-production-to-head Testcontainers tests before merge;
- schema compatibility cannot be claimed in final DoD until either this conditional gate exists or the absence of a migration mechanism remains an explicit release risk.

## 10. CI Design

### 10.1 Selected mechanism

Use the existing `src/test` layout with tags and a small number of custom Gradle `Test` tasks. Do not create physical source sets initially.

Reasons:

- 111 tracked test sources do not justify duplicate compilation/configuration trees.
- JUnit/Kotest coexistence is easier under the same JUnit Platform contract.
- tags allow gradual migration without moving files.
- source sets are reconsidered only if measured classpath isolation or task scheduling cannot meet feedback goals.

### 10.2 Tasks and phases

| CI phase | Suites | Trigger | Failure policy |
|---|---|---|---|
| PR Fast | unit, application, Web, architecture, API diff | every PR | required |
| PR Integration | MySQL persistence, Redis, external mock-server, boot smoke | every PR; parallel job after Fast | required |
| PR Correctness | Booking/Performance/Schedule/Ticket concurrency, AFTER_COMMIT, critical acceptance | affected paths always; otherwise every PR until stable timing proves selective execution safe | required |
| main/release | all above + three bootJar/Docker startup smokes + dependency/build health | merge/release | required; buildHealth policy remains explicit |
| nightly/periodic | repeated concurrency, optional mutation, diagnostic queue probe if adopted | scheduled | initially report, then required for stable critical suites |

Proposed task names are implementation names and therefore marked NEW: `fastTest`, `integrationTest`, `correctnessTest`, and `acceptanceTest`. They remain `Test` tasks over existing outputs. `test` remains capable of running the complete normal suite locally; no test disappears behind an undiscoverable task.

### 10.3 Measurement gates

- Phase 0 records three runs for current `check`, container startup, and each future phase on CI-class hardware.
- PR10 records JUnit-only vs coexistence discovery counts and task overhead.
- PR Integration records Spring context count/hit rate and container reuse.
- No numerical timeout/SLA is fixed until those measurements exist.
- A new test/tool is rolled back or scoped down if it materially increases feedback time without owning a unique production risk.

## 11. Ordered Migration Phases and Revised PR Graph

### 11.1 Dependency graph

```text
PR-9 (runtime alignment, complete)
  |
  v
PR-10 Test platform/pilot
  |-------------------|
  v                   v
PR-11 Critical        PR-12 Domain fast-risk rewrite
correctness locks       |
  |---------------------|
  v
PR-13 MySQL/Redis adapter slices
  |
  v
PR-14 Frontoffice application risk owners
  |
  v
PR-15 Web/security/OpenAPI/acceptance foundation
  |------------|-------------|
  v            v             v
PR-16 Home   PR-17 Admin   PR-19 System/Batch
             User            |
               |             |
               v             |
             PR-18 Admin     |
             Promotion       |
  |------------|-------------|
  v
PR-20 Observability/global-support
  |
  v
PR-21 Infrastructure visibility + module-contracts retirement
  |
  v
PR-22 Kotlin interop + legacy test/guard retirement
  |
  v
PR-23 Application failure boundary + apps-to-Domain retirement
  |
  v
PR-24 CI optimization + final gates
```

This replaces the prior post-PR9 PR10~19 hypothesis. The PR-21 final audit then exposed direct API/Admin Domain exception/configuration dependencies, so error-language migration and final verification were split from mechanical test/Kotlin cleanup. The count is now 24; PR number preservation has no architectural value.

### 11.2 Phase 0 — Baseline and risk ledger (planning/pre-PR10)

- **Goal:** freeze develop/PR9 counts, results, timing, context/container topology, and workspace-only drafts.
- **Existing files affected:** none in production; this plan only.
- **NEW files:** none beyond this plan.
- **Build changes:** none.
- **Tests migrated/deleted:** none.
- **Verify:** `git rev-parse develop origin/develop HEAD`; `git ls-files '*/src/test/*'`; `./gradlew projects`; three measured `./gradlew check verifyModuleBootJars` runs when CI resources are available.
- **Exit:** every tracked test appears in §3; all untracked drafts are separated; timing evidence stored in PR description/artifact.
- **Rollback:** delete this planning document only.
- **Risk:** current branch changes may move paths before implementation; re-run inventory at each PR start.

### 11.3 PR-10 — Test platform and coexistence pilot

- **Goal:** add Kotest FunSpec authoring without changing production behavior or deleting legacy tests.
- **Existing files:** `gradle/libs.versions.toml`, `build-logic/src/main/kotlin/beat.test.gradle.kts`, affected module build files, `.github/workflows/ci-pr.yml` only after tag pilot passes.
- **NEW files:** one representative Domain FunSpec, one Application FunSpec, one Spring FunSpec; exact leaf names are selected from existing tests being dual-run, not invented as new behavior. `fastTest`, `integrationTest`, `correctnessTest`, `acceptanceTest` tasks.
- **Dependencies:** Kotest runner/assertions/Spring; MockK only in the pilot module if a boundary mock is necessary; no Fixture Monkey yet.
- **Migrated:** duplicate three existing assertions as discovery pilots; do not delete originals in the first commit.
- **Verify:** `./gradlew test`; each NEW task; compare discovered leaf count with expected JUnit + Kotest count; verify tags select both engines.
- **Exit:** JUnit/Kotest coexist, FunSpec is discovered in pure and Spring contexts, explicit SingleInstance/Leaf policy is executable, no production dependency changed.
- **Rollback:** remove catalog aliases/task configuration/pilot specs; legacy suite remains intact.
- **Risk:** engine/tag mismatch and global Spring extension side effects. Avoid a repository-wide ProjectConfig that boots Spring for pure tests.

### 11.4 PR-11 — Critical Booking/Performance/Schedule/Ticket characterization

- **Goal:** establish executable transaction/lock semantics before further broad migration.
- **Existing files:** `GuestBookingServiceConcurrencyTest.java`, `ScheduleBookingAvailabilityIntegrationTest.kt`, `TicketBulkLockOrderingIntegrationTest.kt`, `ScheduleSynchronizerTest.kt`, their real repositories/services and test support only if deterministic observability requires a narrow seam.
- **NEW files:** a Performance-price-modification vs Booking-creation concurrency spec; a canonical lock-topology decision/ADR if expected behavior was previously undefined.
- **Build changes:** assign correctness tag/task; no runtime/BOM upgrade.
- **Migrated:** rewrite fixed sleeps/random credentials; preserve old tests until new outcomes pass.
- **Deleted/merged:** none until deterministic replacements prove identical risk.
- **Verify:** `./gradlew :apps:api:correctnessTest :application:frontoffice:test`; run the concurrency task repeatedly with bounded timeout; inspect MySQL final state.
- **Exit:** approved old/new price snapshot outcome, no overselling, DB-close recheck, sorted Ticket locks, and canonical lock order are deterministic.
- **Rollback:** revert new specs/harness independently; old tests remain.
- **Risk:** the test may expose a production defect. Stop and create a correctness-fix PR; do not weaken the assertion to preserve current implementation.
- **Implementation evidence:** PR11 reproduced a stale active-Booking decision under MySQL `REPEATABLE READ`, replaced it with a current locking read after Schedule locks, and verified both price serialization orders plus final Performance/Schedule/Booking state. The full API/Application correctness lane passes without fixed sleeps, random credentials, or test-level worker transactions.

### 11.5 PR-12 — Domain fast-risk rewrite

- **Goal:** make domain invariants the single fast owner and retire Java/duplicate domain tests.
- **Existing files:** all ten files in §3.4.
- **NEW files:** Kotlin FunSpec replacements at the same capability-owned test packages; named fixture functions local to domain tests.
- **Build changes:** domain Kotest assertions/property dependency; no Spring/MockK.
- **Migrated:** Booking, Schedule, Performance, Promotion, equality, privacy.
- **Deleted/merged:** Java equality duplicate; duplicated Schedule capacity/service assertions; Performance child ownership into aggregate owner.
- **Verify:** `./gradlew :domain:fastTest --rerun-tasks`; compile classpath and source audit prove no Spring/JUnit/MockK. Property seeds are required only when a property owner is introduced.
- **Exit:** every domain invariant row in §4 has exactly one domain owner; deliberate mutant/temporary change is caught.
- **Rollback:** file-by-file because production source is unchanged.
- **Risk:** accidental behavior changes disguised as translation. Compare assertion inventory before deleting Java files.
- **Implementation evidence:** PR12 replaced or merged all eight Java domain test owners into ten Kotlin FunSpec owners. The 63-test fast suite passes with no Java test source, Spring, MockK, JUnit API, wall-clock time, randomness, or sleeps. Booking risks are owned separately by creation, lifecycle, refund-account, and cross-domain privacy specs rather than a production-class-shaped monolith.

### 11.6 PR-13 — Real MySQL and Redis slices

- **Goal:** replace mocked repositories/broad contexts/manual containers with focused real-adapter tests.
- **Existing files:** all infrastructure persistence/Redis tests in §3.5; API query integrations; `GuestAuthRedisIntegrationTest.java`; three legacy integration bases.
- **NEW files:** separate MySQL/Redis Spring-managed container configs so a slice starts only the service it needs; capability-focused persistence slices; guest throttle atomicity spec; member constraint/translation spec. The untracked registration-concurrency draft is not adopted as an Infrastructure test because losing-User rollback belongs to the frontoffice transaction.
- **Build changes:** slice dependencies and Spring-managed container imports; no H2.
- **Migrated:** JDSL/JDBC queries, scalar lookup, DB constraints and adapter exception translation, Redis TTL/aliases/Lua. Full `MemberRegistrar` rollback remains a use-case integration risk and is not claimed by the repository slice.
- **Deleted/merged:** mocked Redis test, mocked repository semantics, manual container bases only after last subclass moves.
- **Verify:** `./gradlew :infrastructure:integrationTest :apps:api:integrationTest`; inspect context-cache and container counts.
- **Exit:** every query/constraint/TTL/atomicity risk in §4 has a real-infra owner; cached contexts never reference stopped containers.
- **Rollback:** retain old integration base until final migration commit; slice configs are removable independently.
- **Risk:** context imports may accidentally omit custom repository implementations; each slice must execute a non-empty query/result assertion.
- **Implementation evidence:** Spring-managed Redis/MySQL container configurations and focused Redis/JPA slices run against real infrastructure. Redis owners cover refresh-token round trip/TTL/delete/legacy alias/missing values, guest-session TTL, throttle TTL, and Lua atomicity. MySQL owners cover Booker Booking snapshots/null legacy amount, Booking scalar/guest-credential queries, Schedule aggregation, Maker Ticket projection/ordering, Member unique-constraint translation, and KST session/DB-clock contracts. The Member persistence spec deliberately does not claim `MemberRegistrar` losing-User rollback. After Docker Desktop recovery, focused Redis, Redis correctness, and six MySQL slice runs passed; the combined lanes executed 45 fast, 19 integration, and 1 correctness test with zero failures/errors. Nine obsolete broad/mock/manual owners were then retired, and `:infrastructure:check --rerun-tasks` passed with 65 tests.

### 11.7 PR-14 — Frontoffice application risk-owner rewrite

- **Goal:** rewrite Booking, Performance, Schedule, Ticket, Member/Auth application tests without Spring and remove duplicated domain assertions.
- **Existing files:** the 18 Frontoffice behavior/pilot files present after actor alignment; the ArchUnit guard is a separate architecture owner.
- **NEW files:** simple in-memory fakes only where reused behavior warrants them; otherwise local stubs/real objects.
- **Build changes:** Kotest is supplied by the shared test convention. MockK is not added while BEAT targets Java 25 because upstream Java 25 compatibility remains [open](https://github.com/mockk/mockk/issues/1434); existing Mockito is restricted to per-leaf boundary doubles and Kotlin-null-unsafe generic matchers are forbidden.
- **Migrated:** actor/use-case authorization, orchestration, output/event intent, consumer mapping.
- **Deleted/merged:** large Ticket service transition matrix split into domain/application/MySQL owners.
- **Verify:** `./gradlew :application:frontoffice:test`; architecture test proves no Spring Web/JPA/Redis detail.
- **Exit:** each application test explains actor and risk; no “one unit test per service” pattern; command correctness fakes model authoritative semantics explicitly.
- **Rollback:** capability by capability.
- **Risk:** fake repository can diverge from query/locking semantics; such assertions belong to PR13/PR11, not the fake.
- **Implementation evidence:** 18 behavior/pilot files were rewritten or merged into 16 semantic Kotlin FunSpec owners. Three Booking leaves that only duplicated Domain ticket-count/capacity invariants were removed; authorization, authoritative lookup/lock intent, failure translation, saved output, inventory/event side effects, and consumer mapping remain Application-owned. `compileTestJava NO-SOURCE`, 89 fast executions, one correctness execution, and `:application:frontoffice:check --rerun-tasks` all pass. The only remaining JUnit owner in this module is the existing ArchUnit guard, which is intentionally handled by the architecture-guard phase.

### 11.8 PR-15 — Web, authorization, OpenAPI, and acceptance foundation

- **Goal:** create HTTP/security/API compatibility owners and a small stable acceptance lane.
- **Existing files:** API/Admin DTO/exception/controller tests, API/Admin security configs, API boot smoke, `ci-pr.yml`.
- **NEW files:** `BeatAcceptanceTest.kt`, `BeatTestContainersConfig.kt`, controller Web slices by existing capability, complete method/path authorization matrix, normalized OpenAPI baselines, pinned oasdiff invocation.
- **Build changes:** Web slice/acceptance tasks; Fixture Monkey only if §7.2 admission evidence is recorded.
- **Migrated:** JSON aliases/status/error envelope, File direct controller test, guest-origin/security behavior.
- **Deleted/merged:** DTO and handler unit fragments only after Web/OpenAPI owners pass; no facade deletion before controller/use-case owner exists.
- **Verify:** `./gradlew :apps:api:fastTest :apps:api:acceptanceTest :apps:admin:fastTest`; generate specs and run breaking diff; report context cache keys.
- **Exit:** every actual endpoint is in authorization matrix; general/admin OpenAPI baselines are reviewed; critical API acceptance journeys pass with real MySQL/Redis.
- **Rollback:** remove new meta/config and keep prior boot/DTO tests; baseline update is separately reviewable.
- **Risk:** `@WebMvcTest` may need many imports due current composition. Report, do not distort production architecture merely to shrink test context.
- **Implementation evidence:** API/Admin 공통 acceptance meta-annotation과 Spring-managed container configuration을 추가했다. exact handler inventories는 Frontoffice 24 operations와 Admin 5 operations이며, 각 authorization matrix가 anonymous/member/admin filter boundary를 검증한다. File/Ticket Web slices는 실제 query binding과 adapter mapping을 소유하고, API/Admin JSON 및 exception specs는 OpenAPI가 표현하지 못하는 enum/alias/validation/error-envelope 계약을 보존한다. Fixture Monkey와 MockK는 admission evidence가 없어 추가하지 않았다.
- **OpenAPI evidence:** `openApiTest`는 기존 test source set과 `openapi` tag를 사용하고 fast lane에서는 제외된다. 검토된 baselines는 `docs/openapi/baseline/{general,admin}.json`이며 actual authorization inventory와 operation 단위로 일치한다. CI는 checksum-pinned oasdiff `v1.28.0`을 내려받아 두 `breaking --fail-on ERR` 비교를 실행한다; 현재 결과는 모두 `No changes detected`다.
- **Verification evidence:** API/Admin focused fast tests, API/Admin boot+authorization acceptance, 각 module `check`, unused catalog alias 검사, diff whitespace 검사, OpenAPI gate, api/admin/batch bootJar gate가 모두 통과했다. 삭제 파일을 문자열로 읽던 Admin boot test leaf는 더 강한 actual-context owner가 통과한 뒤 제거했다.

### 11.9 PR-16 — Home Booker projection migration with tests

- **Goal:** migrate Home according to architecture plan and write its query/application/Web/MySQL tests in the new standard.
- **Existing production files:** `apis/src/main/kotlin/com/beat/apis/home/application/query/HomeQueryService.kt`, `apis/src/main/kotlin/com/beat/apis/home/application/result/HomeResults.kt`, `apis/src/main/kotlin/com/beat/apis/home/facade/HomeFacade.kt`, `apis/src/main/kotlin/com/beat/apis/home/api/HomeController.kt`, `core/infra/src/main/kotlin/com/beat/infra/persistence/promotion/repository/query/HomePromotionQueries.kt`, and the actual Performance/Schedule readers consumed by `HomeQueryService`. No tracked Home-specific test exists.
- **NEW files:** a Home Booker application query spec, real Home projection query slice, and Home Web contract spec. Exact package follows the migrated production owner; no placeholder class is created before that move.
- **Dependency:** PR15 and Performance/Schedule foundations.
- **Verify:** Home module/app tests, API diff, API boot.
- **Exit:** Home projection is consumer-owned, query fidelity is real MySQL, route/JSON unchanged.
- **Rollback:** capability slice revert.
- **Risk:** duplicating Performance/Schedule domain knowledge in a Home port; query output must remain consumer-specific.
- **Implementation evidence:** `HomeQueryApplicationSpec` owns reader inputs, promotion mapping, display-period formatting, due-date sorting, and null/empty behavior without Spring. `HomeProjectionQueriesIntegrationSpec` owns the current Performance/Schedule/Promotion projection against real MySQL, including genre filtering, minimum schedule date, missing schedule nullability, and semantic carousel order. `HomeControllerWebSpec` owns genre binding and route/JSON/status through the real Controller/Facade with only the Application boundary replaced.
- **Correctness evidence:** the MySQL owner rejected the legacy `EnumType.STRING` lexical order (`ONE, THREE, TWO`). Production mapping now orders typed `CarouselNumber` values by their Domain `number`; the test retains `ONE, TWO, THREE` as the observable contract.
- **Replacement evidence:** Home-specific source scanning, the API-owned Home workflow/results, and the old split Schedule/Promotion read contracts/adapters were removed only after the three replacement levels passed. No business rule is duplicated across levels: Application owns composition policy, MySQL owns query semantics, Web owns delivery compatibility.
- **Verification evidence:** focused Home suites pass; root tests and all checks for `application:frontoffice`, `infrastructure`, and `apps:api` pass with 106 executed tasks. Boot JAR and OpenAPI breaking gates remain green.

### 11.10 PR-17 — Admin User migration with tests

- **Goal:** move Admin User workflow into `application:admin` and establish Admin actor/security test lane.
- **Existing tests:** `AdminUserQueryServiceTest.kt`, `AdminUserFacadeTest.kt`, relevant Admin DTO/boot/error tests.
- **NEW files:** application-admin query FunSpec and Admin User Web/security spec at packages derived from moved production files.
- **Dependency:** PR15.
- **Verify:** `:application:admin:test`, Admin Web/API diff, Admin boot.
- **Exit:** facade workflow removed, `ROLE_ADMIN` matrix enforced, JSON compatible.
- **Rollback:** retain temporary facade adapter until final commit.
- **Risk:** actor package duplication: the application lane already means Admin, so no redundant `admin` actor package.
- **Implementation evidence:** `AdminUserQueryApplicationSpec` owns caller-Member validation, missing-Member failure, persisted User-id/role mapping, and repository-call ordering without Spring. `AdminUserControllerWebSpec` owns the real Controller/Facade route, query binding, response JSON/status, and member-id delegation while replacing only the moved Application boundary.
- **Replacement evidence:** the old isolated service test was rewritten at its new semantic owner; the delegation-only facade test was deleted only after the Web owner passed. Existing exact Admin authorization, failure-envelope, JSON compatibility, real context boot, and OpenAPI suites retain their distinct risks without duplicating the User query rule.
- **Verification evidence:** `:application:admin:check` and `:apps:admin:check` pass; the Admin context discovers the moved service only through `AdminApplicationConfig`. Admin OpenAPI generation and the pinned breaking diff report no change, and module-graph/bootJar gates remain green.

### 11.11 PR-18 — Admin Promotion migration and referential concurrency

- **Goal:** move Promotion workflow and prove Performance reference/Carousel persistence concurrency with real MySQL.
- **Existing tests:** Admin Promotion command/query/response/facade tests, Promotion domain/mapper/entity tests, S3/cache adapter tests involved by actual calls.
- **NEW files:** Promotion persistence/query/concurrency specs only after current relation/lock behavior is characterized.
- **Dependency:** PR17 and Performance foundation.
- **Verify:** domain/application/Admin Web, MySQL correctness, API diff, S3 adapter.
- **Exit:** Promotion risk owners are split across domain/application/MySQL/Web; facade/JPA-shape tests removed after replacements.
- **Rollback:** preserve compatibility adapter until migration commit ends.
- **Risk:** existence-check vs concurrent Performance deletion can reveal orphan behavior; fix in separate correctness commit if confirmed.
- **Implementation evidence:** Spring-free `AdminPromotionCommandApplicationSpec`/`AdminPromotionQueryApplicationSpec`이 validation, authoritative Performance lock order/reference failure, mutation/response ordering, storage/cache intent를 소유한다. `AdminPromotionControllerWebSpec`은 네 Admin route의 binding, polymorphic request mapping, JSON/status/delegation을 소유하고 기존 facade-only test를 대체한다.
- **Persistence/external evidence:** real-MySQL `PromotionLockingIntegrationSpec`은 기존-row 및 empty-carousel namespace serialization과 concurrent Performance deletion을 production transaction topology로 검증한다. 첫 empty-table assertion은 기존 row-lock 구현을 실제로 실패시켰고, cross-instance advisory lock 전략 적용 후 통과했다. S3 adapter spec은 새 public Promotion storage contract의 `exists` behavior를 검증한다. test-level transaction과 H2는 사용하지 않는다.
- **Retirement/verification evidence:** old Admin Promotion service/query/facade tests와 central Performance/storage/CDN contract/query는 replacement owners가 green인 뒤 제거했다. affected Application/Infrastructure/Admin checks, Admin OpenAPI, root architecture/module/boot gates가 통과하며 중요한 risk는 삭제되지 않았다.

### 11.12 PR-19 — System/Batch workflow migration with tests

- **Goal:** move cleanup/maintenance workflows into `application:system`; keep batch as scheduler/bootstrap.
- **Existing tests:** all Batch tests in §3.8 except root guards.
- **NEW files:** system use-case FunSpecs and focused scheduler wiring tests based on actual moved classes.
- **Dependency:** PR12/13; Admin Promotion dependency if maintenance touches its state.
- **Verify:** `:application:system:test`, Batch focused context, Batch boot/actuator, job invocation.
- **Exit:** delegation facades/jobs no longer own business assertions; no Java production/test compatibility remains solely for Batch.
- **Rollback:** keep scheduler adapter delegating to old facade until the final migration commit.
- **Risk:** cron/job timing and failure handling are runtime contracts; do not replace them with pure service mocks only.
- **Implemented ownership:** Spring-free FunSpecs own fixed-clock cutoff and authoritative lock-order/eligibility policy; Kotlin job specs own exact cron/scheduler/owner delegation; Spring-managed Batch boot/actuator specs own runtime wiring; real-MySQL `SystemMaintenanceIntegrationSpec` owns cutoff persistence and Promotion deletion/reorder semantics.
- **Retired after replacement:** Java service/job/boot tests, manual static-container base, and facade delegation tests were removed only after their risks moved to the owners above. Batch production Java is `NO-SOURCE`; the untracked notification contention probe is outside this migration and remains untouched.

### 11.13 PR-20 — Observability and global-support rewrite

- **Goal:** migrate the focused technical specs and settle ownership before removing `global-support`.
- **Existing tests:** all files in §3.6.
- **NEW files:** none unless a utility moves and its package changes with production.
- **Dependency:** can start after PR12 but merges after source-conflicting capability moves.
- **Verify:** support-security, support-observability, app boot smokes, log resource contract.
- **Exit:** MDC/coroutine/async/Sentry/token/password risks remain focused; architecture scans are replaced.
- **Rollback:** file-level.
- **Risk:** losing resource/config compatibility during module relocation; boot/resource tests remain until final physical move.
- **Implemented evidence:** API/Admin delivery contracts and API CDN serialization moved to their consumer modules; the sole image-key utility moved to Admin Promotion. Focused JSON/serializer/utility checks and app/boot/graph gates passed before `global-support` was removed. Existing observability Jupiter tests remain explicit PR22 audit inputs rather than being rewritten solely for syntax uniformity.

### 11.14 PR-21 — Infrastructure visibility and `module-contracts` retirement

- **Goal:** retire remaining contracts/temporary adapters and make implementation visibility executable.
- **Existing tests:** root/shared architecture tests, app architecture guards, infra adapter tests affected by moves.
- **NEW files:** only focused Gradle/ArchUnit rules not already compiler-enforced.
- **Dependency:** PR16~20.
- **Verify:** project dependency graph, zero legacy references, all adapter slices, three app boots.
- **Exit:** `module-contracts` has no references/project; controller/web cannot access infra implementations; application lanes remain independent.
- **Rollback:** contract-by-contract temporary compatibility adapter, removed only when every consumer is migrated.
- **Risk:** replacing one central contracts module with many thin ports. Port deletion/volatility tests remain mandatory.
- **Implemented evidence:** Guest Redis state contracts are Booking-owned, Booking notification reuses the existing event vocabulary, and `OptionalLong`/duplicate DTO debt is gone. `module-contracts` was removed after all consumers migrated. Infrastructure implementations are Kotlin `internal`; public configuration is restricted to the composition-root allowlist. The six module checks plus transition/target graph and all boot jars passed in one 97-task forced run.

### 11.15 PR-22 — Kotlin interop and legacy test/guard retirement

- **Goal:** remove Kotlin-owned `Optional`/nullable lookup-id debt, Java-only `@Jvm*`, and legacy Java/source-string architecture owners only after their risks move to compiler/Gradle/ArchUnit/semantic tests.
- **Existing files:** every remaining REWRITE/MERGE/DELETE row; root/build-logic/CI files.
- **NEW files:** only focused Kotlin risk owners or ArchUnit rules required to replace a deleted legacy assertion.
- **Dependency:** PR21 and all test owners green.
- **Verify:** affected module checks, unfiltered discovery comparison, zero tracked Java production/test unless justified, zero Kotlin `Optional`, zero unnecessary `@Jvm*`, architecture guard suite.
- **Exit:** every deleted owner has a named replacement; source-string architecture assertions are gone or explicitly limited to non-code configuration contracts.
- **Rollback:** Kotlin API, Java caller, and guard cleanup commits separated by category.
- **Risk:** not-found semantics or hidden regression coverage changing during mechanical modernization.
- **Implemented evidence:** all remaining owned behavior tests use FunSpec; repository Optional/nullable-id and Java-only annotation debt is retired. Source-string architecture owners moved to Gradle/compiled ArchUnit. Thirteen non-code deployment/runtime contracts remain as three executable root Kotlin specs; root uses `beat.kotlin-base` for test compilation only and has no production source or executable plugin. Ten module checks, root transition tests, target graph, and three boot jars pass. The untracked Batch contention probe remains outside migration ownership.

### 11.16 PR-23 — Application failure boundary and apps-to-Domain retirement

- **Goal:** translate Domain failures inside Frontoffice/Admin Application and move Domain-service bean composition into Application configuration.
- **Existing files:** API/Admin global exception handlers and DomainServiceConfig, affected Application use cases/config/tests, app build dependencies.
- **NEW files:** lane-owned failure mapping policy/tests only when actual mappings require them.
- **Dependency:** PR22.
- **Verify:** Spring-free mapping owners, affected Application specs, API/Admin HTTP compatibility, authorization/OpenAPI, app checks and boots.
- **Exit:** API/Admin production imports and project dependencies on Domain are zero; existing status/message/code behavior is preserved through Application failure language.
- **Rollback:** Frontoffice/Admin mappings independently revertible.
- **Risk:** changing legacy V1 error status/message or transaction rollback behavior.
- **Implemented evidence:** all Frontoffice/Admin service entry points translate inside the proxied method body and compiled ArchUnit prevents an unwrapped service class. Frontoffice owns the reachable V1 special matrix; Admin retains only reachable Promotion/User semantics and removes copied cross-capability mappings. Application, HTTP, concurrency, graph, boot, and OpenAPI gates pass.

### 11.17 PR-24 — CI optimization and final migration gates

- **Goal:** tune CI only from measured discovery/runtime evidence and close every final architecture/test/runtime checklist item.
- **Existing files:** root/build-logic/CI plus final report/checklist documents.
- **NEW files:** none by default.
- **Dependency:** PR23 and all risk owners green.
- **Verify:** all tasks in §12, unfiltered versus tagged discovery, dependency/build health, OpenAPI, authorization, concurrency, three boot/deploy artifacts.
- **Exit:** §15 DoD all checked and final migration report complete.
- **Rollback:** CI/task changes separate from reports; no semantic deletion in this PR.
- **Risk:** tag filters silently skipping tests. Compare discovered/expected inventory before merge.
- **Implemented evidence:** PR CI keeps the unfiltered `check` task as the complete discovery owner and removes only the redundant forced OpenAPI rerun. Full check/risk lanes, real MySQL/Redis correctness, architecture transitions, all three boot jars, OpenAPI breaking diff, workflow syntax, version-catalog audit, and dependency build health pass. Production-source string assertions are zero; retained root readers cover non-code deployment/runtime contracts only. Final evidence is recorded in `BEAT-SERVER-MIGRATION-FINAL-REPORT.md`.

## 12. Verification Matrix

| Change | Required commands/evidence |
|---|---|
| Platform | `./gradlew test`; discovery report proving JUnit + Kotest; tag/task inclusion tests |
| Domain | `./gradlew :domain:test` plus approved property seeds |
| Frontoffice application | `./gradlew :application:frontoffice:test` |
| Admin/System application | `./gradlew :application:admin:test :application:system:test` |
| MySQL slices | `./gradlew :infrastructure:integrationTest`; non-empty query and real MySQL version evidence |
| Redis slices | real Redis version, TTL tolerance, atomic final state, legacy serialization |
| Concurrency | `./gradlew :apps:api:correctnessTest`; bounded repeated run; no test transaction; final DB state |
| Web/security | API/Admin fast tests; every method/path/role matrix row; 401 vs 403 vs business failure |
| OpenAPI | generate both groups; normalized clean diff; pinned `oasdiff breaking --fail-on ERR` |
| Acceptance | `:apps:api:acceptanceTest`; context-cache report; real MySQL/Redis; stable external fakes |
| Architecture | `./gradlew projects`; dependency assertions; compiler visibility; focused ArchUnit |
| Runtime | API/Admin/Batch context and health smoke; `verifyModuleBootJars` |
| Full PR | `~/.sober/scripts/verify.sh --path /Users/donghoon/IdeaProjects/sopt/BEAT-SERVER` when available, then `./gradlew check verifyModuleBootJars` if the script does not include them |
| Final | `buildHealth`, zero-reference scans, full unfiltered test discovery vs inventory, Docker/startup smoke |

For a rewritten test, “green” is insufficient. The PR must show a negative proof: a temporary local mutation, wrong fake response, or test fixture that would cause the protected risk to fail. The mutation itself is never committed.

## 13. Trade-offs and BEAT Decisions

| Decision | Rejected alternative | BEAT verdict |
|---|---|---|
| Kotest FunSpec authoring | JUnit Jupiter-only | Choose FunSpec for Kotlin-readable, low-ceremony common language; keep JUnit Platform and coexist during migration. Spring lifecycle differences are made explicit. |
| Real object/simple fake before narrow boundary double | mock every collaborator | Choose state/outcome tests resistant to refactor. MockK remains the preferred Kotlin option once Java 25 is supported; meanwhile BEAT uses existing Mockito only at expensive/side-effect boundaries and forbids Kotlin-null-unsafe generic matchers. |
| Named fixture DSL | Fixture Monkey everywhere | Choose descriptive valid defaults for critical scenarios. Fixture Monkey is selective for large irrelevant graphs only. |
| Kotest Property | random fixture generation as invariant proof | Choose bounded property generators only where a universal invariant exists. Keep named boundary examples. |
| Slice context | `@SpringBootTest` everywhere | Choose smallest context that includes the relevant framework semantics. Full context only for transaction/composition/journey. |
| Real MySQL/Redis | H2/fake Redis | Choose fidelity for lock, SQL, DB time, constraint, TTL, serialization, and Lua atomicity despite startup cost. Offset with context/container reuse. |
| Use-case integration | isolated mock-heavy Service test for every service | Choose a few integration owners for transaction/composition, with pure application units only for meaningful branching. |
| Acceptance | broad E2E for every endpoint | Choose a few critical black-box journeys. Web slices and domain specs own permutations. |
| Tags/custom Test tasks | many source sets from day one | Choose minimal Gradle change and coexistence. Reconsider only from measured isolation/scheduling limits. |
| ArchUnit | source scanning or taxonomy policing | Choose only semantic dependencies Gradle/compiler cannot express. Avoid brittle package depth/naming rules. |
| OpenAPI diff | Pact | Choose pinned oasdiff because BEAT exposes Springdoc specs and has no evidenced consumer/broker workflow. |
| Targeted mutation | whole-repo mutation immediately | Optional nightly pilot on Booking/Schedule/Performance only after deterministic fast tests; expand only if kill-rate value exceeds CI cost. |
| Spring-managed containers for cached contexts | every container globally managed or every class owns static container | Choose lifecycle alignment only when Spring caches the dependent context; standalone tests may keep standalone lifecycle. |

Industry articles from Toss and Woowahan are external validation only. The decision hierarchy remains: BEAT risks -> framework semantics -> industry evidence. Toss's article supports selective high-value tests, real objects over pervasive mocks, FunSpec acceptance, and standardized context configuration, but its H2/JSON setup example is not copied because BEAT's MySQL locking/SQL correctness requires real MySQL.

Framework references consulted for implementation verification:

- [Kotest Spring extension](https://kotest.io/docs/extensions/spring.html)
- [Kotest testing styles](https://kotest.io/docs/framework/testing-styles.html)
- [Kotest isolation modes](https://kotest.io/docs/framework/isolation-mode.html)
- [Spring Boot Testcontainers lifecycle and service connections](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Fixture Monkey Kotlin starter](https://github.com/naver/fixture-monkey)
- [Toss: 가치있는 테스트를 위한 전략과 구현](https://toss.tech/article/test-strategy-server)
- [oasdiff breaking-change rules](https://github.com/oasdiff/oasdiff/blob/main/docs/BREAKING-CHANGES.md)

## 14. Non-Goals

- Increasing raw test count or line coverage.
- Rewriting production code in this planning work.
- Coupling the rewrite to a Spring Boot/Kotlin version upgrade.
- Creating a test per production class/service/repository.
- Introducing Spring Modulith, Pact, Chaos tooling, LocalStack, or a test-support module without measured need.
- Replacing MySQL with H2 for speed.
- Claiming durable notification delivery from local `AFTER_COMMIT` listeners.
- Creating nonexistent migration infrastructure for the sake of a checklist.
- Enforcing Capability/Actor/CQRS by brittle package naming tests.
- Randomizing values that define money, inventory, authorization, ownership, or state-transition outcomes.
- Deleting user-owned untracked test drafts.

## 15. Definition of Done

The plan is complete only when the following are answered and the eventual implementation is complete only when all executable boxes are satisfied.

- [x] Every tracked current test source is classified in §3; every `develop`-only path is mapped in §3.9.
- [x] Every DELETE/MERGE has a named replacement owner and deletion gate.
- [x] Pure Unit ownership is assigned to Booking/Schedule/Performance/Promotion/value-object/entity invariants.
- [x] Isolated application units are limited to meaningful branching, actor authorization, orchestration, and output intent; no service-per-test mandate exists.
- [x] Real MySQL owners are identified for JDSL/JDBC queries, uniqueness/constraints, DB clock, pessimistic locks, lock ordering, snapshot concurrency, and overselling.
- [x] Real Redis owners are identified for TTL, serialization aliases, delete, and Lua atomicity.
- [x] Controller risks are limited to routing, binding/validation, serialization, error/status, cookie/origin, and authorization.
- [x] Critical acceptance journeys are enumerated and narrower test owners retain edge permutations.
- [x] Booking concurrency explicitly uses worker calls through production transaction proxies and forbids test-level transactions.
- [x] Fixture Monkey admission/ownership/KotlinPlugin/null/randomness policy and prohibited uses are explicit.
- [x] Gradle/compiler rules and residual ArchUnit rules are separated.
- [x] General/Admin OpenAPI generation, normalized baseline, pinned breaking diff, and intentional-change workflow are specified.
- [x] Actual API/Admin security rules seed a method/path/role authorization matrix; Web vs application authorization ownership is separated.
- [x] The absence of schema migrations is documented; a conditional real-MySQL gate is defined for the first migration PR.
- [x] Spring context cache fragmentation sources and measurement are explicit; acceptance configuration is standardized.
- [x] PR Fast/Integration/Correctness/main/nightly balance is defined without inventing unmeasured time budgets.
- [x] Every phase names existing affected files/groups, marks NEW artifacts, states verification, exit, rollback, and risk.
- [x] Implementation: PR10 coexistence pilot is green and discovery counts match.
- [x] Implementation: critical lock/snapshot decisions are executable before broad deletion.
- [x] Implementation: every §3 migration row has reached its final decision without losing risk coverage.
- [x] Implementation: all §12 gates pass and unfiltered discovery matches the final inventory.

The final implementation report must show one owner per material production risk, remaining deliberate overlaps and their reasons, measured PR feedback time, context/container reuse evidence, and every justified exception to Kotlin-first authoring or the target module boundaries.
