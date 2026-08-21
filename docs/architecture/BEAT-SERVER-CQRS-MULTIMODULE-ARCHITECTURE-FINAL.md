# BEAT-SERVER CQRS 기반 멀티모듈 아키텍처 최종 설계 보고서

**Status:** Final Target Architecture / Adopted for Migration

**Architecture type:** 0→1 Target Architecture + Incremental Migration

**Target repository:** `TEAM-BEAT/BEAT-SERVER`

**Target branch:** `develop`

**Baseline date:** 2026-08-18
**Primary objective:** 비즈니스 변경을 좁은 영역에 가두고, Application / Domain / Infrastructure / Runtime 경계를 Gradle과 코드 수준에서 강제한다.

---

# 1. Executive Verdict

BEAT-SERVER의 최종 Target Architecture는 다음과 같다.

```text
BEAT-SERVER
│
├── apps
│   ├── api
│   ├── admin
│   └── batch
│
├── application
│   ├── frontoffice
│   ├── admin
│   └── system
│
├── domain
│
├── infrastructure
│
├── support
│   ├── security
│   └── observability
│
└── build-logic
```

이 구조는 단순한 Layered Architecture가 아니다.

BEAT는 다음 네 개의 강한 책임 축을 분리한다.

```text
Runtime / Delivery
→ apps

Use Case / Application Policy
→ application

Business Rule / Invariant
→ domain

Technical Implementation / Driven Adapter
→ infrastructure
```

Cross-cutting technical concern은 별도의 narrow support boundary가 소유한다.

```text
Authentication / Security plumbing
→ support:security

Logging / Metrics / Tracing
→ support:observability
```

0→1 Target에서는 `module-contracts`를 만들지 않는다.

Port는 별도 중앙 계약 모듈이 아니라 **그 Port를 필요로 하는 consumer Application이 소유**한다.

또한 `support:web`도 처음부터 만들지 않는다. API와 Admin이 실제로 안정적인 HTTP representation policy를 공유하고, 그 공유 코드가 독립된 변경축으로 성장했을 때만 추출한다.

---

# 2. Architecture의 목적

이 Architecture의 목표는 폴더 구조의 미학이 아니다.

최우선 목표는 **change locality**다.

좋은 변경은 다음과 같이 끝나야 한다.

```text
Booking 정책 변경
→ domain/booking 또는 application/frontoffice/booking에서 종료

Maker 공연 workflow 변경
→ application:frontoffice/performance/maker에서 종료

Admin 승인 workflow 변경
→ application:admin/performance에서 종료

Batch maintenance 정책 변경
→ application:system에서 종료

JPA/JDSL 구현 변경
→ infrastructure에서 종료

SMS provider 변경
→ infrastructure/external/sms에서 종료

HTTP response 변경
→ apps:api 또는 apps:admin에서 종료
```

Architecture 품질은 module 개수보다:

```text
한 변경이 얼마나 좁은 영역에서 끝나는가?
```

로 평가한다.

---

# 3. Architecture를 결정하는 서로 다른 축

## 3.1 Runtime / Delivery Boundary

```text
apps:api
apps:admin
apps:batch
```

각각 독립 Spring Boot executable이다.

## 3.2 Application Boundary

```text
application:frontoffice
application:admin
application:system
```

각 Application lane은 서로 compile-time으로 격리한다.

## 3.3 Domain Boundary

```text
booking
performance
ticket
schedule
member
payment
settlement
promotion
...
```

Business Capability가 logical/change ownership의 1차 축이다.

## 3.4 Actor Boundary

Frontoffice:

```text
Booker
Maker
```

Admin:

```text
Admin
```

System:

```text
System
```

Actor는 Domain 복제 기준이 아니라 Use Case ownership 기준이다.

## 3.5 CQRS Boundary

필요한 Capability 안에서:

```text
Command
Query
```

를 분리한다.

CQRS 자체는 Gradle module 생성 사유가 아니다.

---

# 4. 최종 Gradle Project

```kotlin
rootProject.name = "beat-server"

include(
    ":apps:api",
    ":apps:admin",
    ":apps:batch",

    ":application:frontoffice",
    ":application:admin",
    ":application:system",

    ":domain",
    ":infrastructure",

    ":support:security",
    ":support:observability",
)
```

Product subproject는 10개다.

숫자는 목표가 아니다.

새 Gradle project는 다음 중 하나 이상의 실익을 제공해야 한다.

```text
compile isolation
dependency isolation
change isolation
test isolation
build isolation
runtime/deployment isolation
stable reusable technical boundary
```

---

# 5. `apps` — Inbound Adapter + Composition Root

`apps`는 비즈니스 workflow를 소유하지 않는다.

## 5.1 `apps:api`

Booker / Maker를 위한 외부 HTTP runtime.

소유:

```text
Controller
Request DTO
Response DTO
ControllerAdvice
HTTP validation
OpenAPI
serialization
HTTP security wiring
ApiApplication bootstrap
composition root
```

금지:

```text
business workflow
JPA Repository 직접 사용
EntityManager
RedisTemplate
JDSL implementation
Infrastructure implementation 직접 호출
Domain invariant 구현
```

기본 흐름:

```text
HTTP
 ↓
BookingController
 ↓
CreateBooking
```

Controller는 HTTP input을 Application input으로 변환하고, Application result를 HTTP representation으로 변환한다.

## 5.2 `apps:admin`

Admin HTTP runtime.

소유:

```text
Admin Controller
Admin request/response
Admin HTTP validation/policy
Admin security wiring
Admin bootstrap
```

Admin workflow는 `application:admin`에 둔다.

## 5.3 `apps:batch`

System runtime.

소유:

```text
@Scheduled
Spring Batch Job/Step configuration
Cron
Job parameter mapping
Batch bootstrap
```

Business maintenance workflow는 `application:system`이 소유한다.

```text
@Scheduled
   ↓
CleanupExpiredBookings
   ↓
Domain / Output Port
```

---

# 6. `application` — Use Case Layer

Application Layer는 BEAT가 “무엇을 하는가”를 표현한다.

Application은 Delivery와 물리적으로도 분리한다.

Spring 자체는 허용한다.

허용:

```text
@Service
@Transactional
Spring transaction abstraction
필요한 same-process coordination
Java/Kotlin standard library
```

금지:

```text
@RestController
ResponseEntity
HttpStatus
JpaRepository
EntityManager
RedisTemplate
FeignClient
S3Client
JDSL implementation
QueryDSL implementation
HTTP DTO
JPA Entity
Redis Document
```

핵심 원칙:

> Application은 Spring-aware일 수 있지만 infrastructure-aware 또는 delivery-aware하면 안 된다.

---

# 7. `application:frontoffice`

외부 고객-facing Use Case lane.

현재 Actor:

```text
Booker
Maker
```

`frontoffice`는 Domain vocabulary가 아니라 physical application lane 이름이다.

장기적으로 Booker/Maker의 dependency, ownership, build lifecycle, runtime이 실제로 갈라질 경우 다음을 재검토할 수 있다.

```text
application:booker
application:maker
```

현재 기본 structure:

```text
application/frontoffice
└── com.beat.application.frontoffice
    ├── booking
    │   └── booker
    │       ├── command
    │       └── query
    ├── performance
    │   ├── booker
    │   │   └── query
    │   └── maker
    │       ├── command
    │       └── query
    ├── schedule
    │   └── booker
    │       └── query
    ├── ticket
    │   └── maker
    │       ├── command
    │       └── query
    ├── home
    │   └── booker
    │       └── query
    ├── member
    │   └── command
    └── auth
        └── command
```

기본 순서:

```text
Capability
→ Actor
→ Command / Query
```

빈 package를 architecture ceremony 때문에 만들지 않는다.

Actor는 인증 principal type이 아니라 Use Case의 의도와 policy를 바꾸는 비즈니스 행위 주체다.

```text
Booker
= 공연을 탐색하고 회차를 선택하며 예매하는 행위 주체
= 회원/게스트 인증 상태와 동일한 개념이 아님

Maker
= 공연·회차를 생성/수정하고 예매자를 관리하는 행위 주체
```

`application:frontoffice`는 lane 자체가 Actor를 표현하지 않으므로, Use Case가 Booker 또는 Maker의 행위임이 분명하면 현재 한 Actor만 존재해도 Actor package를 명시한다. Actor 생략 근거는 "현재 한 명만 사용"이 아니라 "특정 Actor의 policy가 아닌 frontoffice 공통 capability"여야 한다.

`member`, `auth`는 Booker/Maker 모두가 사용하는 identity/session capability이며 현재 actor-specific policy가 없으므로 Actor-neutral 예외로 둔다. 향후 Actor별 policy가 실제로 분기될 때만 하위 Actor를 추가한다.

`application:admin`, `application:system`은 physical lane이 Actor를 이미 표현하므로 `admin`, `system` package를 반복하지 않는다.

---

# 8. `application:admin`

Admin 전용 Use Case lane.

```text
application/admin
└── com.beat.application.admin
    ├── performance
    │   ├── command
    │   └── query
    ├── maker
    ├── settlement
    ├── payment
    └── statistics
```

Maker/Public workflow와 Admin workflow는 서로 직접 재사용하지 않는다.

```text
Maker ModifyPerformance
X→ Admin SuspendPerformance

Admin SuspendPerformance
X→ Maker ModifyPerformance
```

공통 invariant는 `domain`을 통해 공유한다.

---

# 9. `application:system`

Batch / Scheduler / System Use Case lane.

```text
application/system
├── booking
│   └── command
│       └── CleanupExpiredBookings
├── promotion
│   └── command
│       └── MaintainPromotions
└── settlement
    └── command
        └── CalculateSettlement
```

Batch trigger는 Application Use Case를 호출할 뿐이다.

---

# 10. `domain` — Business Rules

Domain은 가장 보호받는 module이다.

```text
domain
└── com.beat.domain
    ├── booking
    ├── performance
    ├── ticket
    ├── schedule
    ├── member
    ├── payment
    ├── settlement
    └── promotion
```

소유:

```text
Aggregate
Entity
Value Object
Domain Service
Domain Event
Domain Exception
Business Policy
```

금지:

```text
Spring
Spring Web
JPA
Redis
HTTP
Application DTO
ReadModel
Booker/Maker/Admin DTO
JDSL / QueryDSL implementation
Infrastructure Adapter
```

Domain은 BEAT project dependency를 갖지 않는 것을 기본으로 한다.

---

# 11. Domain Service Rule

Domain Service를 기계적으로 만들지 않는다.

우선순위:

```text
Aggregate / Entity / Value Object
        ↓
규칙을 자연스럽게 소유할 곳이 없음
        ↓
Domain Service
```

좋은 예:

```text
RefundCalculator
SettlementPolicy
ReservationPolicy
```

Domain Service는:

```text
HTTP를 모름
JPA를 모름
Redis를 모름
Application Result를 모름
Business Rule만 표현
```

단순히 여러 method를 모으기 위해 `XxxDomainService`를 만들지 않는다.

---

# 12. Logical Capability Ownership vs Physical Layer Boundary

BEAT의 1차 **logical/change ownership**은 Business Capability다.

하지만 physical Gradle boundary는:

```text
apps
application
domain
infrastructure
```

라는 dependency responsibility를 표현한다.

Booking을 이해할 때:

```text
apps/api/.../booking
application/frontoffice/.../booking
domain/.../booking
infrastructure/.../booking
```

을 볼 수 있다.

이는 의도된 trade-off다.

Capability-first는 “Booking 파일을 하나의 Gradle project에 몰아넣는다”는 뜻이 아니다.

---

# 13. Repository Ownership

Repository는 하나의 보편 규칙으로 강제하지 않는다.

## 13.1 Aggregate Repository

예:

```kotlin
interface BookingRepository {
    fun findById(id: BookingId): Booking?
    fun save(booking: Booking): Booking
}
```

이 interface가:

```text
Booking aggregate collection
```

이라는 Domain language를 표현한다면 Domain 소유가 가능하다.

## 13.2 Application Output Port

특정 Use Case가 외부 capability를 요구한다면 Application이 소유한다.

예:

```text
PaymentAuthorizer
BookingNotificationSender
BookingTermsProvider
PerformanceContentChecker
```

## 13.3 Query Reader

Consumer-specific read capability는 Query Application이 소유한다.

```text
BookerBookingReader
MakerPerformanceReader
AdminSettlementReader
```

---

# 14. Port Creation Rule

Dependency가 있다는 이유만으로 interface를 만들지 않는다.

Port를 만들기 전에 반드시 다음을 묻는다.

```text
1. 이 interface가 어떤 concrete volatility를 숨기는가?

2. 그 volatility를 Application에서 격리할 가치가 있는가?

3. consumer language에서 의미 있는 capability인가?

4. implementation보다 충분히 깊은 interface인가?

5. 다른 Capability의 Domain knowledge를 복제하고 있지 않은가?

6. 이 interface를 삭제하면 complexity가 실제 consumer/implementation으로 퍼지는가?
```

단순 forwarding layer라면 만들지 않는다.

---

# 15. Consumer-owned Port ≠ UseCase-per-interface

금지:

```text
CreateBookingStore
CancelBookingStore
UpdateBookingStore
FindBookingStore
```

하나의 안정적인 aggregate lifecycle abstraction이면:

```text
BookingRepository
```

하나가 더 자연스럽다.

반대로 external capability가 다르면 분리한다.

```text
PaymentAuthorizer
BookingNotificationSender
```

Port granularity는 Use Case 수가 아니라 **change axis**를 따른다.

---

# 16. Minimal Application Public API

Application module의 public surface를 최소화한다.

Public 후보:

```text
Inbound Use Case entry point
Use Case input/output type
Infrastructure가 구현해야 할 Output Port
의도적으로 공개된 Capability API
```

그 외:

```text
implementation helper
assembler
orchestration detail
policy helper
private/internal component
```

는 Kotlin `internal`을 기본으로 한다.

Application module이 giant public namespace가 되는 것을 금지한다.

---

# 17. Application Service Graph 금지

기본 금지:

```text
CreateBooking
   ↓
GetPerformanceUseCase
   ↓
GetMemberUseCase
   ↓
ProcessPaymentUseCase
```

Application Service graph를 만들지 않는다.

Cross-capability collaboration 우선순위:

```text
1. Domain collaboration
2. Consumer-owned narrow Output Port
3. Explicit stable Capability API
4. 마지막 수단으로 다른 Application Use Case
```

---

# 18. Capability Owns Authoritative State

각 Business Capability는 자신의 authoritative state를 소유한다.

중요:

> Output Port를 만들었다는 이유만으로 다른 Capability의 persistence representation을 Command에서 임의로 우회해서는 안 된다.

예:

```text
Booking Command
→ BookingTermsProvider
→ Infrastructure가 Performance table을 직접 조회
```

가 항상 정답은 아니다.

Performance가 해당 invariant의 진짜 owner라면 명시적인 collaboration boundary가 필요할 수 있다.

Command에서는 Capability ownership을 엄격하게 보호한다.

Query에서는 consumer projection을 위해 cross-capability join을 허용할 수 있다.

---

# 19. CQRS Definition

CQRS:

```text
Command
= state transition + business correctness

Query
= consumer-specific read optimization
```

CQRS가 의미하지 않는 것:

```text
별도 DB 필수
Event Sourcing 필수
Kafka 필수
Command/Query Gradle module 필수
대칭적인 Repository 구조 필수
```

---

# 20. Command Side

기본 흐름:

```text
Controller / Trigger
        ↓
Application Command Use Case
        ↓
Authoritative Repository / Port
        ↓
Domain
        ↓
Infrastructure
        ↓
Primary authoritative state
```

Command는 read할 수 있다.

금지되는 것은:

```text
eventually-consistent cache
read replica
presentation projection
@ReadModel
```

을 다음 correctness input으로 사용하는 것이다.

```text
money
inventory
authorization
state transition
ownership
```

---

# 21. BookingTerms / Authoritative Semantic Port

기존 구조에서 발생할 수 있는:

```text
CreateBooking
→ PerformanceSummaryReadPort
→ @ReadModel
→ ticketPrice
```

같은 semantic hazard를 0→1에서는 금지한다.

예:

```kotlin
interface BookingTermsProvider {
    fun getTerms(performanceId: PerformanceId): BookingTerms
}

data class BookingTerms(
    val ticketPrice: Money,
    val paymentDestination: PaymentDestination,
)
```

이 contract는 authoritative consistency를 요구한다.

단, Port를 만들기 전에 가격/결제조건의 진짜 Domain owner를 먼저 검증한다.

```text
Performance?
Schedule?
Offering?
```

Architecture보다 Domain Modeling이 우선이다.

---

# 22. Query Side

기본 흐름:

```text
Controller
   ↓
Application Query Use Case
   ↓
Consumer-specific Reader
   ↓
JDSL / SQL
   ↓
Primary / Replica / Cache
   ↓
Projection
```

예:

```text
BookerBookingReader
MakerPerformanceReader
AdminSettlementReader
```

Query는 필요하면 여러 table/capability를 join해 consumer projection을 직접 구성할 수 있다.

Domain Aggregate를 억지로 hydrate하지 않는다.

---

# 23. Transaction Ownership

Transaction boundary는 Application Command가 기본으로 소유한다.

```kotlin
@Transactional
class CreateBooking(...)
```

Domain은 transaction을 모른다.

Repository adapter가 전체 Use Case transaction을 소유하지 않는다.

Query는 필요한 경우 read-only transaction을 사용할 수 있다.

---

# 24. Infrastructure

초기에는 하나의 Gradle project로 유지한다.

```text
infrastructure
├── persistence
│   ├── booking
│   ├── performance
│   ├── member
│   └── ...
├── redis
├── external
│   ├── storage
│   ├── sms
│   ├── social
│   └── payment
└── config
```

Dependency:

```text
infrastructure
├── application:frontoffice
├── application:admin
├── application:system
└── domain
```

Infrastructure가 Application에 의존하는 것은 Dependency Inversion의 정상적인 형태다.

---

# 25. Infrastructure High Fan-In

Infrastructure가 세 Application project를 의존하는 비용은 현재 규모에서는 받아들인다.

처음부터:

```text
contracts:frontoffice
contracts:admin
contracts:system
```

을 만들지 않는다.

Compile/rebuild 비용 또는 public API surface가 실제 문제로 확인되면 그때 extraction을 검토한다.

---

# 26. Infrastructure Visibility

Infrastructure implementation은 Kotlin `internal`을 기본으로 한다.

```kotlin
internal class JpaBookingRepository(...)
internal class JdslBookerBookingReader(...)
internal class RedisGuestSessionAdapter(...)
internal class CoolSmsAdapter(...)
```

Public으로 남길 수 있는 것은:

```text
필요한 Spring configuration
bootstrap marker
factory
composition을 위해 외부에서 참조해야 하는 최소 API
```

정도다.

---

# 27. Apps → Infrastructure

Apps는 Composition Root이므로 Infrastructure dependency를 허용한다.

```text
apps
 ├→ application
 └→ infrastructure
       └→ application
```

cycle이 아니다.

단 source-level access는 제한한다.

```text
apps.api.web..
X→ infrastructure..

apps.api.config..
→ infrastructure configuration allowed
```

Controller가 Infrastructure implementation을 직접 사용하지 못하게 한다.

---

# 28. `support:security`

소유:

```text
JWT parsing
credential verification
authentication filter
principal/current identity
Spring Security plumbing
shared authentication primitives
```

금지:

```text
Maker가 Performance owner인가?
Booker가 Booking을 취소할 수 있는가?
Admin이 Settlement를 승인할 수 있는가?
```

이것은 Application / Domain business authorization이다.

Runtime-specific SecurityConfiguration은 필요한 경우 각 apps가 소유한다.

---

# 29. `support:observability`

소유:

```text
Logging
MDC
Metrics
Tracing
OpenTelemetry
Sentry
```

Domain dependency 금지.

Observability는 technical concern이지 Business concern이 아니다.

---

# 30. `support:web`

0→1에서는 생성하지 않는다.

처음에는:

```text
apps:api
apps:admin
```

이 각자의 Web representation concern을 소유한다.

다음이 실제로 생기면 추출을 검토한다.

```text
동일 HTTP representation policy
충분한 코드량
독립 변경축
반복 중복 비용
```

Shared module 역시 existence를 earn해야 한다.

---

# 31. Spring Modulith의 역할

Gradle:

```text
application:frontoffice
application:admin
application:system
```

같은 macro physical boundary를 담당한다.

Spring Modulith / ArchUnit은 각 project 내부 logical boundary를 보조 검증한다.

예:

```text
booking
performance
ticket
payment
```

단 이 Architecture는 physical layer modules를 사용하므로, Spring Modulith이 Booking 전체 vertical slice를 완전히 표현하는 도구는 아니다.

따라서 우선순위:

```text
Gradle + Kotlin visibility + ArchUnit
= architecture enforcement 주력

Spring Modulith
= application project 내부 logical capability verification / integration testing에 선택적 사용
```

Spring Modulith을 Architecture의 주인으로 만들지 않는다.

---

# 32. Event Policy

구분:

```text
Domain Event
= business fact

Internal Event
= same-process coordination

Integration Event
= external contract
```

Domain Event와 Integration Event는 동일하지 않다.

Kafka / Outbox / DLQ는 delivery guarantee가 실제 요구될 때 추가한다.

CQRS를 이유로 Event-driven Architecture를 선도입하지 않는다.

---

# 33. Application Error Policy

Application에서 금지:

```text
ApiApplicationException
HttpStatus
ResponseEntity
Admin HTTP error type
```

Application은 자신의 failure language를 사용한다.

예:

```text
BookingClosed
SoldOut
MemberNotFound
PaymentUnavailable
```

Web Adapter가 이를:

```text
409
404
503
```

등으로 변환한다.

---

# 34. Test Architecture

## 34.1 Execution / authoring contract

```text
Execution engine
→ JUnit Platform

Kotlin test authoring baseline
→ Kotest FunSpec
→ Kotest Assertions

Optional test tools
→ Kotest Property: invariant/boundary exploration
→ MockK: real object/simple fake로 대체하기 어려운 경계만
→ Kotest Spring Extension: Spring TestContext가 필요한 spec만
```

`useJUnitPlatform()`은 모든 모듈의 공통 실행 계약으로 유지한다. 최종 Kotlin test authoring style은 `FunSpec`으로 통일한다. JUnit Jupiter/Mockito로 작성된 기존 Java/Kotlin test는 migration 중 temporary compatibility로만 남기고, 각 protected invariant의 대체 coverage를 확인한 뒤 제거한다.

FunSpec의 표준 형태:

```kotlin
class BookingSpec : FunSpec({
    context("예매 가능한 회차에서") {
        test("마지막 재고까지 예매할 수 있다") {
            // given
            // when
            // then
        }
    }
})
```

`context/test`는 scenario tree를 표현하고, leaf body의 Given/When/Then은 행위 흐름을 표현한다. `describe/context/it`, `given/when/then` taxonomy를 모든 test에 강제하지 않는다.

## 34.2 Test double policy

```text
real object
→ simple capability-local fake
→ MockK stub/mock
```

순서로 선택한다. MockK 채택은 모든 dependency의 interaction mocking을 의미하지 않는다. 상태/결과를 우선 검증하고, 외부 system·비결정적 시간·파괴적 side effect·실패 분기 제어처럼 분리 가치가 명확한 경계에만 test double을 사용한다. Test 편의를 위해 production Port를 생성하지 않는다.

## Domain

```text
Kotest FunSpec pure Kotlin unit
Spring 없음
DB 없음
Testcontainers 없음
```

대상:

```text
Aggregate invariant
Value Object
state transition
business calculation
Domain Service
```

## Application

```text
Kotest FunSpec Use Case test
Spring Context 없음
real object / small fake 우선
boundary에만 MockK
transaction/application policy test
```

상태 기반 검증을 우선한다.

Fake가 architecture seam을 정당화하는 근거가 되어서는 안 된다.

## Infrastructure

```text
Kotest FunSpec
MySQL Testcontainers
Redis Testcontainers
JPA mapping
JDSL query
constraint
locking
transaction semantics
Redis adapter
external adapter contract
```

FakeRepository로 persistence correctness를 증명하지 않는다.

BEAT의 production DB semantic은 MySQL이므로 money/inventory/locking/transaction test를 H2/PostgreSQL로 대체하지 않는다. HTTP external adapter는 실제 client + controllable local server로 serialization, timeout, status mapping을 검증한다.

## Apps

```text
Kotest FunSpec
MockMvc
security
serialization
HTTP contract
bootstrap/context
소수 critical E2E
```

## 34.3 Spring / acceptance policy

Spring이 필요 없는 Domain/Application test에 `@SpringBootTest`를 사용하지 않는다. Spring integration/acceptance test도 FunSpec을 유지하되 Kotest Spring Extension을 명시적으로 등록한다.

Spring TestContext event는 method model과 Kotest nested-function model이 1:1이 아니다. Lifecycle mode 지정은 framework 사용의 기술적 필수사항은 아니지만, BEAT는 nested-test와 transaction semantics를 암묵적 default에 맡기지 않는 정책을 채택한다. 기본 acceptance/integration policy는 leaf test별 lifecycle/transaction rollback이며, root mode나 `@Commit`은 scenario가 요구할 때만 선택하고 근거를 남긴다.

User-facing critical journey는 `apps:api` test source가 소유하는 공통 `@BeatAcceptanceTest`로 context configuration을 통일한다.

```text
@SpringBootTest(RANDOM_PORT)
@ActiveProfiles("test")
stable Testcontainers configuration import
Kotest Spring Extension
stable TestExecutionListener set
```

이 meta-annotation의 목적은 annotation ceremony가 아니라 Spring TestContext cache key 안정화와 black-box journey 표준화다. Spec별 `@MockkBean`/context property/configuration 변형을 기본 전략으로 삼지 않고 stable fake/configuration을 우선한다. Admin/Batch는 각 runtime lane의 필요가 증명될 때 lane-local meta-annotation을 두며 중앙 `test-common`에 합치지 않는다.

## 34.4 Testcontainers / concurrency policy

Spring ApplicationContext와 함께 재사용되는 integration/acceptance container는 lifecycle을 context cache lifetime과 정렬한다. 이 범위에서는 모듈-local `@TestConfiguration` bean + `@ServiceConnection` 또는 `@ImportTestcontainers`를 우선하고, cached context와 분리된 static initializer의 수동 `start()`를 최종 구조로 두지 않는다. Spring Context를 사용하지 않거나 test별 독립 lifecycle이 더 자연스러운 repository/adapter test까지 Spring bean 방식으로 강제하지 않는다.

Concurrency/locking test:

```text
test-level @Transactional 금지
worker는 production Application transaction boundary 사용
실제 MySQL lock/constraint 사용
동시 시작 barrier + bounded timeout
성공/실패 수와 최종 authoritative DB state 모두 검증
```

Property test는 VO 범위, overflow, 상태 전이, 예매 window 등 불변식의 탐색에 사용하되 example-based regression test를 대체하지 않는다.

## 34.5 Migration / version policy

현재 JUnit/Mockito test를 일괄 syntax conversion하지 않는다.

```text
protected invariant inventory
→ target owner의 FunSpec replacement
→ focused + module test
→ legacy test 제거
→ JUnit Jupiter/Mockito authoring dependency 최종 제거
```

Spring Boot/Kotlin/Testcontainers major/minor alignment은 Kotest authoring migration과 별도 PR로 검증한다. Spring Boot 4.1 baseline 채택 시 Boot BOM이 관리하는 Kotlin 2.3.21/Testcontainers 2.0.5 정렬을 우선하고, explicit version override는 근거가 있을 때만 남긴다.

## 34.6 Decision evidence

Normative framework evidence:

- [Kotest Spring Extension](https://kotest.io/docs/extensions/spring.html): `TestContextManager`, constructor injection, lifecycle mode, Spring transaction mapping.
- [Spring Boot Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html): context-managed container bean, `@ServiceConnection`, `@ImportTestcontainers` lifecycle.
- [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html): adopted Boot baseline의 BOM versions.

Industry evidence:

- [토스 — 가치있는 테스트를 위한 전략과 구현](https://toss.tech/article/test-strategy-server): FunSpec 기반 Domain/Acceptance test, real object 우선, acceptance meta-annotation과 Spring TestContext cache 관리 사례.

Industry 사례는 BEAT Architecture의 Source of Truth가 아니다. 이 결정의 기준은 BEAT의 Kotlin-first 목표, MySQL/Redis production semantics, test ownership, feedback speed, rollback 가능한 migration boundary다.

---

# 35. Test Fixture Policy

처음부터:

```text
test-common
test-utils
```

같은 dumping ground를 만들지 않는다.

기본은 capability-local fixture다.

Stable test API가 실제 여러 Gradle project에서 재사용될 때만 별도 test-fixture mechanism을 검토한다.

---

# 36. Architecture Guard Rules

CI에서 최소 다음을 검증한다.

```text
domain
X→ Spring
X→ Application
X→ Infrastructure
X→ Apps
X→ Web/JPA/Redis

apps:api
X→ apps:admin
X→ apps:batch

apps:admin
X→ apps:api
X→ apps:batch

apps:batch
X→ apps:api
X→ apps:admin

application:frontoffice
X→ application:admin
X→ application:system

application:admin
X→ application:frontoffice
X→ application:system

application:system
X→ application:frontoffice
X→ application:admin

Application
X→ JPA Entity
X→ Redis Document
X→ EntityManager
X→ HTTP DTO

Application Service
X→ 다른 Capability concrete Application Service

Command
X→ @ReadModel correctness dependency

Web Adapter
X→ Infrastructure implementation

Infrastructure
X→ Apps
```

---

# 37. Command Rule의 정확한 표현

잘못된 rule:

```text
Command X→ Query package
```

정확한 rule:

> Command는 authoritative state를 읽을 수 있다.
> Command는 presentation/read-optimized/eventually-consistent abstraction을 correctness 판단에 사용해서는 안 된다.

이 semantic rule은 naming, marker, architecture test, review로 보호한다.

---

# 38. Build Logic

`build-logic` included build + convention plugin을 기본으로 한다.

예:

```text
build-logic
├── beat.kotlin-library
├── beat.spring-library
├── beat.spring-boot-app
├── beat.jpa-adapter
├── beat.web
├── beat.security
└── beat.observability
```

`allprojects {}` / `subprojects {}` 기반 giant cross-project configuration을 피한다.

Convention plugin 역시 실제 capability를 표현할 때만 만든다.

---

# 39. BootJar Policy

| Module | Type | bootJar |
|---|---|---:|
| `apps:api` | Spring Boot executable | true |
| `apps:admin` | Spring Boot executable | true |
| `apps:batch` | Spring Boot executable | true |
| `application:frontoffice` | Spring library | false |
| `application:admin` | Spring library | false |
| `application:system` | Spring library | false |
| `domain` | Kotlin library | false |
| `infrastructure` | Spring adapter library | false |
| `support:security` | Spring library | false |
| `support:observability` | Spring library | false |

Library에는 Boot application plugin을 기본 적용하지 않는다.

---

# 40. Kotlin-first Rules

0→1 Target에서는 Kotlin을 기본 language로 한다.

감사 대상:

```text
불필요한 @JvmStatic
불필요한 @JvmOverloads
@JvmField
java.util.Optional in Kotlin-owned API
Java-style getX()/setX()
nullable identifier 남발
Java-only factory/adapter
```

원칙:

```text
Kotlin nullability
Kotlin property
default argument
value class/domain identifier
findByIdOrNull where appropriate
```

실제 Java ABI 요구가 있을 때만 interoperability annotation을 허용한다.

---

# 41. Domain Modeling First

0→1 Architecture가 기존 Entity를 새 폴더에 옮기는 작업이 되어서는 안 된다.

특히 다음 Capability의 authoritative ownership을 먼저 검증한다.

```text
Performance
Schedule
Booking
Payment
Ticket
Settlement
```

예:

```text
ticketPrice의 authoritative owner는 누구인가?
회차별 가격이 가능한가?
Booking은 어떤 값을 snapshot으로 저장해야 하는가?
inventory invariant는 Schedule인가 별도 Inventory인가?
payment 상태와 booking 상태의 transaction boundary는 어디인가?
```

Architecture가 Domain Model을 대신 결정하지 않는다.

---

# 42. Architecture Constitution

## Principle 1 — Apps are Adapters
Apps는 inbound adapter와 composition root만 소유한다.

## Principle 2 — Application owns Use Cases
Application은 Use Case orchestration과 Application policy를 소유한다.

## Principle 3 — Domain owns Business Invariants
Business invariant는 Domain이 소유한다.

## Principle 4 — Capability is Logical Ownership
Business Capability가 logical/change ownership의 1차 축이다.

## Principle 5 — Physical Layers are Dependency Boundaries
`apps/application/domain/infrastructure`는 dependency responsibility를 표현한다.

## Principle 6 — Domain Service Must Earn Its Existence
Aggregate/VO가 자연스럽게 소유할 수 없는 규칙에만 Domain Service를 만든다.

## Principle 7 — Command Protects Correctness
Command correctness는 authoritative state를 사용한다.

## Principle 8 — Query Optimizes for Consumers
Query는 consumer-specific projection과 read optimization을 허용한다.

## Principle 9 — Ports Must Earn Their Existence
Port는 실제 volatility/seam을 숨길 때만 만든다.

## Principle 10 — Consumer Owns Output Port
Output Port의 language는 consumer/use case가 소유한다.

## Principle 11 — No Application Service Graph
다른 Capability concrete Application Service 직접 호출을 기본 금지한다.

## Principle 12 — Capability Owns Authoritative State
Command에서 다른 Capability persistence를 임의 우회하지 않는다.

## Principle 13 — Application Public API is Minimal
Inbound Use Case API와 필요한 Output Port 외에는 기본 `internal`.

## Principle 14 — Infrastructure is Hidden
Infrastructure implementation은 기본 `internal`.

## Principle 15 — Transaction Boundary is Application
Command transaction ownership은 Application Use Case가 기본이다.

## Principle 16 — Shared Modules Must Earn Their Existence
Shared/support module은 중복이라는 이유만으로 만들지 않는다.

## Principle 17 — Gradle Module Must Earn Its Existence
Compile/change/test/dependency isolation의 실익이 있어야 존재한다.

## Principle 18 — Architecture is Enforced
Compiler, Gradle, Kotlin visibility, ArchUnit, tests, review 중 가능한 가장 강한 mechanism을 우선한다.

---

# 43. NOT TO DO

```text
❌ module-contracts를 0→1부터 생성

❌ single giant core:application

❌ CQRS Command/Query별 Gradle module

❌ capability마다 domain/application/infra Gradle module 폭발

❌ 모든 dependency를 Port로 감싸기

❌ UseCase-per-interface

❌ Application Service chaining

❌ Command에서 ReadModel을 correctness input으로 사용

❌ Domain에 Spring/JPA/Web leakage

❌ Controller에서 Infrastructure 직접 사용

❌ Infrastructure implementation 광범위 public

❌ support:web 선생성

❌ test-common dumping ground

❌ CQRS라는 이유로 Kafka/Event Sourcing/Outbox 선도입

❌ Spring Modulith에 맞추기 위한 구조 변경

❌ 모든 persistence entity에 rich domain model을 기계적으로 복제
```

---

# 44. Future Module Promotion

새 Gradle module은 다음 중 실제 실익이 나타날 때 검토한다.

```text
독립 compile classpath
dependency set divergence
independent change frequency
test isolation
build performance
parallel execution
team ownership
release/deployment isolation
failure/scaling isolation
stable reusable subsystem
```

“폴더가 커졌다”는 단독 이유로 만들지 않는다.

---

# 45. Future Evolution

## Booker/Maker split

다음이 실제로 발생하면:

```text
Maker 전담 ownership
별도 lifecycle
별도 inbound runtime
dependency divergence
```

다음을 검토한다.

```text
application:frontoffice
→ application:booker
→ application:maker
```

필요하면:

```text
apps:booker-api
apps:maker-api
```

까지 확장할 수 있다.

## Query Platform

다음이 실제로 발생하면:

```text
Read DB
Search Engine
Materialized Projection
대규모 Redis layer
독립 scaling
```

`infrastructure:read` 같은 physical extraction을 검토한다.

---

# 46. Migration Strategy for Current BEAT

Target은 0→1 quality로 정의하지만 implementation은 Big Bang rewrite로 하지 않는다.

```text
Characterize current behavior
        ↓
Create target boundary
        ↓
Migrate one coherent slice
        ↓
Compile
        ↓
Focused tests
        ↓
Integration / regression
        ↓
Delete legacy path
        ↓
Next slice
```

Architecture는 새로 설계하되 migration은 Strangler / incremental slice 방식으로 수행한다.

---

# 47. First Reference Slice

전체 migration 전에 Booking을 대표 capability 후보로 평가한다.

기본 candidate:

```text
CreateBooking
CancelBooking
GetMyBookings
GetBookingDetail
```

Target:

```text
apps:api
        ↓
application:frontoffice
        ↓
domain
        ↑
infrastructure
```

Port 후보:

```text
BookingRepository
BookingTermsProvider
ScheduleInventory
BookerBookingReader
```

단 각 Port는 seam/deletion test를 통과해야 한다.

검증:

```text
Use Case 이해에 필요한 module hop
Port 개수
Fake setup 복잡도
transaction boundary 자연스러움
cross-capability ownership 우회 여부
infra leakage
test feedback speed
```

Booking slice가 부자연스럽다면 전체 migration 전에 Target micro-design을 수정한다.

---

# 48. PR Strategy

Migration은 여러 PR로 나누는 것을 기본으로 한다.

단 PR 분할 기준은 미리:

```text
모듈별
API별
endpoint별
package별
```

로 고정하지 않는다.

각 PR은 다음 조건을 최대한 만족해야 한다.

```text
독립적인 architecture invariant를 개선
review 가능한 크기
rollback 가능한 단위
behavior compatibility 검증 가능
가능하면 build/test green
다음 PR prerequisite 명확
```

Implementation agent는 실제 dependency graph와 correctness/test risk를 분석한 뒤 다음 후보를 비교해 PR graph를 설계한다.

```text
Capability slice
Runtime lane
Physical module
Cross-cutting prerequisite
Correctness fix
Test foundation
Kotlin modernization
Dependency migration
```

즉:

```text
PR = module
```

또는:

```text
PR = endpoint
```

를 기계적으로 강제하지 않는다.

---

# 49. Suggested Migration Areas

정확한 PR 분할은 implementation agent가 결정한다.

참고 가능한 area:

```text
Correctness / characterization foundation

Target Gradle skeleton / architecture guards

Booking reference slice

Performance / Schedule / Ticket migration

Admin Application extraction

System / Batch Application extraction

Infrastructure consolidation / internal visibility

Security / Observability normalization

module-contracts retirement

Kotlin modernization

Legacy architecture-test retirement

Final dependency / behavior verification
```

이 목록은 PR 순서가 아니라 planning input이다.

---

# 50. Final ADR

## ADR-BEAT-001 — CQRS Modular Architecture

**Decision: ACCEPTED**

Target:

```text
Runtime:
apps:api / apps:admin / apps:batch

Application:
application:frontoffice / application:admin / application:system

Domain:
domain

Driven Adapters:
infrastructure

Cross-cutting:
support:security / support:observability
```

Business Capability가 logical/change ownership의 1차 축이며, physical Gradle modules는 dependency responsibility와 compile isolation을 표현한다.

Application은 Use Case와 필요한 Output Port를 소유하고 Infrastructure가 이를 구현한다.

Port는 실제 semantic seam을 제공할 때만 만든다.

Command와 Query는 비대칭적으로 설계하고, Command correctness는 authoritative state로 보호한다.

Application Service graph와 Command의 ReadModel correctness dependency를 기본 금지한다.

Spring Modulith은 필요 시 application project 내부 logical capability verification에 사용하며 Architecture의 목적이나 physical module model을 결정하지 않는다.

---

# Final Verdict

BEAT를 오늘 0에서 설계한다면 다음 Target을 채택한다.

```text
apps
├── api
├── admin
└── batch

application
├── frontoffice
├── admin
└── system

domain

infrastructure

support
├── security
└── observability

build-logic
```

이 구조의 본질은 Clean Architecture diagram을 복제하는 것이 아니다.

목표는:

```text
Domain Rule 변경
→ Domain에서 끝남

Use Case 변경
→ Application에서 끝남

HTTP 변경
→ Apps에서 끝남

Infrastructure 변경
→ Infrastructure에서 끝남

Actor/runtime 변경
→ 해당 Application lane에서 끝남
```

이라는 change locality를 만드는 것이다.

현재 BEAT는 이 Target을 Big Bang rewrite로 교체하지 않는다.

**0→1 수준의 Target Architecture를 고정하고, 실제 migration은 coherent slice 단위로 진행한다.**

PR은 module별/API별로 미리 고정하지 않는다.

**Implementation agent가 실제 dependency graph, correctness risk, testability, reviewability를 분석해 가장 안전하고 논리적인 multi-PR migration graph를 설계한다.**

이 문서를 BEAT-SERVER Target Architecture Baseline 및 migration 판단 기준으로 채택한다.
