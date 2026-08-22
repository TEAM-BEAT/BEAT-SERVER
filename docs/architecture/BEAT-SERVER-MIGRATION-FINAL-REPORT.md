# BEAT-SERVER Architecture Migration Final Report

_Final verification date: 2026-08-23_

## 1. Executive verdict

BEAT-SERVER의 Gradle project graph, source dependency, runtime composition, test ownership을 Architecture Constitution의 경계로 이전했다. `module-contracts`와 `global-support`는 consumer ownership을 확인한 뒤 제거했고, API/Admin/Batch는 각각 독립 실행 가능한 composition root로 유지했다.

최종 경계는 디렉터리 이름이 아니라 executable Gradle project와 compiler dependency로 강제된다. 기존 배포가 사용하는 `apis`, `admin`, `batch`, `core/*`, `gateway`, `observability` 디렉터리는 `projectDir` mapping으로 보존한다. 이 결정은 §14 ADR-FINAL-001에 기록한다.

## 2. Final module tree and source mapping

```text
beat-server
├── :apps:api                 -> apis/
├── :apps:admin               -> admin/
├── :apps:batch               -> batch/
├── :application:frontoffice  -> application/frontoffice/
├── :application:admin        -> application/admin/
├── :application:system       -> application/system/
├── :domain                   -> core/domain/
├── :infrastructure           -> core/infra/
├── :support:security         -> gateway/
├── :support:observability    -> observability/
└── build-logic               -> build-logic/ (included build)
```

`rootProject.name`은 `beat-server`이며 product subproject는 Constitution과 동일한 10개다. `module-contracts`와 `global-support` project는 settings, dependency graph, source에서 제거됐다.

## 3. Final Gradle dependency graph

```text
:apps:api   -> :application:frontoffice
            -> :support:security
            -> :infrastructure
            -> :support:observability

:apps:admin -> :application:admin
            -> :support:security
            -> :infrastructure
            -> :support:observability

:apps:batch -> :application:system
            -> :infrastructure
            -> :support:observability

:application:frontoffice -> :domain
                         -> :support:security
:application:admin       -> :domain
:application:system      -> :domain

:infrastructure -> :domain
                -> :application:frontoffice
                -> :application:admin

:support:security -> :support:observability
:domain           -> no product project
```

API/Admin/Batch의 `:domain` dependency는 test fixtures에만 존재한다. main source에서는 Apps→Domain 직접 import가 없고, Application lane 간 의존과 Application→Infrastructure 의존도 없다. Infrastructure→Apps 의존도 없다.

## 4. Application public APIs

Application의 외부 공개 surface는 Apps가 호출하는 use case, 전달용 command/result/failure language, Infrastructure가 구현하는 output/query contract, 그리고 composition configuration으로 제한한다. 별도 `api` package나 central contract module은 만들지 않았다.

### Frontoffice entry points

- Auth/Member: `AuthenticationCommandService`, `SocialLoginCommandService`
- Booking Booker: member/guest creation, cancellation/refund, guest authentication/session, member/guest query services
- Home Booker: `HomeQueryService`
- Performance Booker: detail query
- Performance Maker: create/modify/delete, file preparation, list/edit query
- Schedule Booker: availability query
- Ticket Maker: command/query
- Bootstrap/failure: `FrontofficeApplicationConfig`, Frontoffice Application failure language

### Admin entry points

- Promotion command/query
- User query
- `AdminApplicationConfig` and Admin Application failure language

### System entry points

- Booking ticket cleanup command
- Promotion maintenance command
- `SystemApplicationConfig`

Use-case 내부 collaborator와 adapter 구현은 `internal`을 기본으로 한다. Cross-capability 호출을 위한 `performance.api`나 concrete Application Service graph는 없다.

## 5. Domain ownership map

| Capability | Authoritative ownership |
|---|---|
| Booking | purchaser/guest identity link, payable amount snapshot, refund data, Booking lifecycle/status |
| Performance | maker ownership, title/content, ticket price, payment destination, commercial lifecycle |
| Schedule | occurrence, Performance membership, booking close time, capacity/inventory, sequence |
| Member | social identity and member profile linked to User |
| User | authenticated principal identity and role |
| Promotion | carousel assignment/order and eligibility |
| Ticket | independent aggregate가 아니라 Booking/Schedule state를 조정·조회하는 Maker Application capability |
| Home | aggregate가 아니라 Booker consumer projection |
| File | 독립 Domain capability가 아니라 Performance Maker storage use case |
| Auth | Frontoffice authentication policy + `support:security`의 narrow technical primitives |

Domain은 Kotlin/JDK만 사용한다. Spring, JPA, Redis, Web, Feign, S3 구현 타입은 없다.

## 6. Remaining output ports and rationale

| Consumer owner | Contract | Retention rationale |
|---|---|---|
| Frontoffice Auth | `LoginSessionIssuer`, `RefreshTokenStore`, `SocialLoginProvider` | JWT/session issuance, Redis, social HTTP provider라는 실제 volatility를 숨김 |
| Booking Booker | `GuestAccessThrottle`, `GuestSessionStore`, `GuestBookingCredentialRepository`, `BookingNotificationSender` | Redis atomicity/TTL, authoritative credential lookup, notification delivery boundary를 consumer vocabulary로 격리 |
| Member | `MemberRegistrationNotifier` | registration policy와 external notification delivery 분리 |
| Performance Maker | `PerformanceImageStorage` | object storage/presign/metadata volatility 격리 |
| Admin Promotion | `PromotionImageStorage`, `PromotionImageCache` | Promotion-specific storage와 best-effort CDN prewarm 격리 |

Password hashing은 output port가 아니다. `support:security`가 public `PasswordHasher` technical API와 internal BCrypt 구현을 함께 소유한다. `ClockPort`, `TransactionRunnerPort`, `BookingTermsProvider`, `performance.api`는 evidence가 없어 만들지 않았다.

## 7. Repository abstractions and ownership

Domain aggregate collection은 `BookingRepository`, `MemberRepository`, `PerformanceRepository`, `PromotionRepository`, `ScheduleRepository`, `UserRepository` 여섯 개다. 모두 Domain vocabulary와 non-null lookup id를 사용하고 nullable lookup result를 Kotlin nullability로 표현한다.

Infrastructure의 JPA repository/entity/mapper/implementation은 `internal`이다. Apps가 import할 수 있는 Infrastructure surface는 composition root용 `EnableInfraBaseConfig`, `InfraBaseConfigGroup`, `InfraPersistenceConfig`, `AuthRedisConfig` 네 개뿐이다.

## 8. Query readers

- Booking Booker: `BookerBookingReader`
- Home Booker: `HomeProjectionReader`
- Performance Booker: `PerformanceScheduleAvailabilityReader`
- Performance Maker: `MakerPerformanceListReader`, `PerformanceEditFormReader`
- Ticket Maker: `MakerTicketReader`

각 reader와 read model은 consumer Application package가 소유한다. `PerformanceContentOwnershipReader`는 예외적으로 Performance modify command의 missing-child 404와 foreign-child 403을 구분하기 위한 primary-DB diagnostic reader다. mutation/invariant 판단에는 사용하지 않으며, 삭제하면 JPA leakage·aggregate 전체 scan·오류 의미 손실 중 하나가 발생하므로 유지한다.

## 9. Authoritative consistency rules

Booking 생성은 다음 순서를 사용한다.

```text
Schedule.performanceId scalar lookup (lock-order hint only)
-> authoritative Performance lock/read
-> Schedule lock
-> Performance membership recheck
-> DB-time booking-close check
-> inventory reserve
-> locked Performance price snapshot into Booking
-> Booking and Schedule atomic save
```

전역 pessimistic row order는 `Performance -> sorted Schedule -> sorted Booking`이다. Performance 가격 수정은 Schedule lock 뒤에 active Booking을 locking read하여 MySQL `REPEATABLE READ` snapshot gap을 피한다. Booking lifecycle/authorization은 locked Booking state, Performance maker authorization은 authoritative Performance owner, Schedule availability는 locked Schedule와 DB clock을 사용한다.

Query projection/cache/replica는 display와 consumer projection에만 사용한다. `PromotionImageCache`는 commit 이후 best-effort prewarm이며 command correctness input이 아니다.

## 10. Architecture guards

Enforcement priority는 다음과 같다.

1. Gradle project graph: forbidden module edge와 legacy project include 차단
2. Kotlin `internal`: Infrastructure implementation과 Application helper surface 차단
3. Compiler: Domain의 framework dependency 부재, Apps main의 Domain dependency 부재
4. ArchUnit: Apps web/controller→Infrastructure implementation, Application service graph, Domain failure leakage, command→presentation reader 등 source-level semantic edge 차단
5. Focused semantic tests: price/inventory/authorization/lock order처럼 type graph만으로 증명할 수 없는 규칙

Production Kotlin/Java source를 문자열로 읽는 legacy architecture test는 없다. Root tests가 읽는 대상은 deployment, Nginx, Sentry, version catalog 같은 non-code runtime contract뿐이다.

## 11. Test architecture

- Execution contract: JUnit Platform
- New/reworked Kotlin authoring: Kotest FunSpec
- Domain/Application: Spring 없는 fast tests, real object -> simple fake -> narrow mock 순서
- Persistence/Redis: real MySQL/Redis Testcontainers
- Web/Security: focused Web contract + exact authorization matrices
- Acceptance: app별 stable Spring context, context-aligned managed containers
- Concurrency: production transaction proxy를 worker가 호출하고 test-level transaction은 사용하지 않음
- Compatibility: reviewed General/Admin OpenAPI baseline + checksum-pinned breaking diff
- Runtime: API/Admin/Batch context and executable boot jar verification

`fastTest`, `integrationTest`, `correctnessTest`, `acceptanceTest`는 기존 `src/test`와 tags를 재사용한다. PR CI의 full `check`는 tag filter 없이 전체 discovery를 수행하며, 별도 중복 OpenAPI rerun은 제거했다.

## 12. Kotlin modernization

- Production Java source: zero
- Kotlin-owned `java.util.Optional`: zero
- 불필요한 `@JvmStatic`, `@JvmOverloads`, `@JvmField`, `@JvmSuppressWildcards`: zero
- 유지한 `@JvmInline`: semantic value class 9개
- Repository lookup result는 nullable Kotlin type, required id input은 non-null Kotlin type
- Domain/API/Admin의 Java-style compatibility accessor는 tracked Java caller retirement 뒤 Kotlin read-only property로 전환
- 남은 `get*` 함수는 HTTP/query operation 또는 framework interface override이며 property accessor가 아님

유일한 Java test source는 사용자 소유의 opt-in `DbJobQueueContentionProbeTest.java`다. migration code가 아니며 기본 CI에서 skip되고 이번 작업에서 수정하지 않았다.

## 13. Final PR graph and merge order

```text
PR-1 + PR-2 + PR-3 -> PR-4 -> PR-5 -> PR-6 -> PR-7
PR-7 -> PR-8 -> PR-9 -> PR-10
PR-10 -+-> PR-11
       +-> PR-12
PR-11 + PR-12 -> PR-13 -> PR-14 -> PR-15
PR-15 -+-> PR-16
       +-> PR-17 -> PR-18
       +-> PR-19
PR-16 + PR-18 + PR-19 -> PR-20 -> PR-21 -> PR-22 -> PR-23 -> PR-24
```

PR-24는 CI 중복 실행 제거, workflow validity, stale source assertion/legacy documentation retirement, final graph/public-surface/interop audit, full verification과 이 보고서를 소유한다.

## 14. ADR and deliberate deviations

### ADR-FINAL-001 — Gradle project identity를 target tree로 확정하고 legacy filesystem path를 유지

- Constitution의 강제 대상은 `:apps:*`, `:application:*`, `:domain`, `:infrastructure`, `:support:*` Gradle project boundary와 dependency direction이다.
- 실제 compile isolation, dependency isolation, bootJar policy와 architecture guard는 이 project identity로 작동한다.
- `apis/admin/batch` 경로는 deploy workflow path filter와 artifact production에, `core/infra`는 runtime configuration/deployment contract에 연결돼 있다.
- 디렉터리 rename은 dependency나 change locality를 더 강화하지 않으면서 deployment diff와 rollback 위험만 만든다.
- 따라서 `settings.gradle.kts`의 explicit `projectDir` mapping을 compatibility boundary로 유지한다. 이는 legacy Gradle module을 유지하는 것이 아니며, project graph에는 target 10개만 존재한다.
- 향후 배포 tooling 자체를 교체할 때 디렉터리 cosmetic alignment를 독립적으로 할 수 있지만 Architecture migration 완료 조건으로 간주하지 않는다.

이 결정은 Constitution의 “목표는 폴더 구조의 미학이 아니라 change locality” 원칙을 적용한 micro-layout 판단이다. Macro boundary나 dependency direction은 변경하지 않는다.

## 15. Verification evidence

- Full gate: `./gradlew check transitionBoundaryTest verifyTargetModuleGraph verifyModuleBootJars --rerun-tasks --no-daemon --max-workers=1` — **BUILD SUCCESSFUL**, 5m09s, 115 tasks executed
- Dependency analysis: `./gradlew buildHealth --no-daemon --max-workers=1` — **BUILD SUCCESSFUL**, 41s, 451 tasks
- OpenAPI: `.github/scripts/verify_openapi_compatibility.sh` — checksum valid, General/Admin no breaking change
- Workflow syntax: `actionlint .github/workflows/*.yml` — pass
- Catalog: `.github/scripts/check_unused_version_catalog_aliases.py` — 44 Kotlin DSL files, unused alias zero
- Static: `git diff --check` — pass
- Runtime: API/Admin/Batch boot jars verified independently
- Integration/correctness: real MySQL/Redis, booking overselling, price serialization, Schedule close time, Ticket lock ordering, Promotion referential concurrency, System maintenance all included in the full gate

Dependency-analysis advice was reviewed, not auto-applied. Spring/Kotest reflection causes several false-positive “unused” entries; changing Application Domain/Spring dependencies from `implementation` to `api` would broaden public ABI; splitting Kotest transitive artifacts would defeat the convention-plugin contract. Architecture-specific Gradle/compiler/ArchUnit guards remain authoritative.

## 16. Remaining risks and explicit exceptions

1. Payment destination is Performance-owned and is not snapshotted into Booking. Whether unpaid historical bookings follow account corrections remains a product/data decision.
2. Legacy Booking rows with null `totalPaymentAmount` cannot recover historical price from the tracked schema. Current-price fallback is preserved only as compatibility, not snapshot truth.
3. Strict purchase-count validation during rehydration remains gated on a read-only production-data audit.
4. `Performance.totalScheduleCount` duplicates Schedule-row state and requires a future ownership/drift audit.
5. Booking's authoritative Performance repository read rehydrates child content. Query count/latency must be measured before adding a narrower repository contract or stable Capability API.
6. Repository schema has no Flyway/Liquibase versioned migration system; no fictional replay gate was introduced.
7. The opt-in Java contention probe remains the single documented Kotlin-first exception.
8. Gradle dependency-analysis recommendations remain advisory for the reasons in §15.

These are documented follow-up risks, not hidden temporary migration adapters. None weakens the verified dependency graph or current observable API/runtime behavior.
