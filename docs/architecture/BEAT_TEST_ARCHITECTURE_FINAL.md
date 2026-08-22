# BEAT Kotlin + Spring 테스트 아키텍처 최종 보고서

- **Status:** Final / Adopted Test Architecture
- **Target repository:** `TEAM-BEAT/BEAT-SERVER`
- **Baseline branch:** `develop`
- **Report date:** 2026-08-21
**Scope:** Kotlin + Spring backend testing strategy, test architecture, fixtures, CI guardrails, migration rules

---

## 0. Executive Verdict

BEAT의 테스트 전략은 다음 한 문장으로 고정한다.

> **가장 작고 빠른 테스트를 선택하되, 그 테스트가 검증하려는 Production Risk에 대해 거짓말하지 않도록 한다.**

테스트를 Production Class와 1:1로 대응시키지 않는다.

`ControllerTest`, `ServiceTest`, `RepositoryTest`를 기계적으로 하나씩 만드는 것이 목표가 아니다.

대신 다음 질문으로 테스트 레벨을 선택한다.

> **이 코드가 Production에서 잘못될 가능성이 가장 큰 이유는 무엇인가?**

- 순수 Business Rule → **Pure Unit**
- Spring MVC contract → **Web Slice**
- JPA/MySQL semantics → **Persistence Slice + Real MySQL**
- Redis semantics → **Redis Slice + Real Redis**
- 외부 HTTP protocol → **Real Client + Mock Server**
- 여러 계층/transaction 조합 → **Use-case Integration**
- 사용자 관점 핵심 journey → **Acceptance**
- Overselling/Lock/Deadlock → **Dedicated Concurrency**
- 배포된 runtime → **Very Few E2E Smoke**
- 모듈/계층 규칙 → **Architecture Test**
- API 하위호환성 → **OpenAPI Compatibility Gate**
- 권한 경계 → **Authorization Matrix**
- DB schema 진화 → **Migration Test**

BEAT의 중심은 다음 세 축이다.

```text
1. Fast Behavioral Tests
   └─ Domain / Pure Application
      Spring 없이 빠르게

2. Focused Boundary Tests
   └─ MVC / JPA / Redis / External HTTP
      필요한 기술 경계만 실제로

3. High-Fidelity Business Tests
   └─ Critical Use Case / Acceptance / Concurrency
      Real Domain + Real MySQL/Redis
```

그리고 문서로만 존재하던 아키텍처/호환성/보안 규칙을 자동 Guardrail로 만든다.

---

## 1. Current Baseline vs Target Direction

현재 `develop` 기준 Version Catalog는 다음을 사용한다.

- Spring Boot `4.0.7`
- Kotlin `2.3.20`
- JUnit `6.0.3`
- Testcontainers `1.21.3`
- Redis Testcontainers `2.2.2`

현재 Gradle 공통 테스트 convention은 모든 `Test` task에 `useJUnitPlatform()`을 사용한다.

따라서 Kotest로 테스트 authoring을 전환해도 **JUnit Platform을 공통 실행 계약으로 유지**한다.

Spring Boot 최신 stable 4.1.0은 feature-specific test module과 slice를 더 명시적으로 제공하지만, 테스트 전면 재작성과 Spring Boot 업그레이드를 하나의 변경으로 묶지 않는다.

> **Rule:** Test rewrite는 현재 production framework baseline에서 먼저 성립해야 한다.
>
> Framework upgrade는 별도 ADR/PR로 관리한다.

---

## 2. Final Tooling Decision

| Concern | Decision |
|---|---|
| Execution Contract | **JUnit Platform** |
| Kotlin Test Authoring | **Kotest FunSpec** |
| Assertions | **Kotest Assertions** |
| Property Testing | **Kotest Property** |
| Mock Framework | **MockK — 제한적 사용** |
| Fixture Generation | **Named Kotlin Fixture DSL + selective Fixture Monkey** |
| Spring Integration | **Kotest Spring Extension** |
| MVC | **`@WebMvcTest`** |
| JPA | **`@DataJpaTest` + MySQL Testcontainers** |
| Redis | **`@DataRedisTest` + Redis Testcontainers** |
| REST Client | **real HTTP client + MockWebServer/WireMock; `@RestClientTest` when appropriate** |
| Use-case Integration | **`@SpringBootTest`** |
| Acceptance | **common `@BeatAcceptanceTest`** |
| Concurrency | **Real MySQL + production transaction boundary** |
| Architecture Guard | **Gradle boundaries + ArchUnit** |
| API Compatibility | **OpenAPI breaking-change diff** |
| Mutation Testing | **targeted/nightly, not PR default** |
| E2E | **very few deployed black-box smoke tests** |

---

## 3. Core Philosophy

### 3.1 Behavioral Risk, not Class Coverage

금지:

```text
BookingController -> BookingControllerTest
BookingFacade -> BookingFacadeTest
BookingService -> BookingServiceTest
BookingRepository -> BookingRepositoryTest
```

를 단순 규칙으로 강제하는 것.

예를 들어 "좌석 부족"이라는 동일 규칙을 Controller → Facade → Service → Acceptance에 반복하지 않는다.

대신 test ownership을 정한다.

```text
잔여 티켓 초과 금지
→ Schedule Domain Test

잘못된 JSON/Validation → 400
→ Booking Web Test

SELECT FOR UPDATE 의미
→ MySQL Integration

30명 동시 예매 overselling 없음
→ Concurrency Test
```

### 3.2 Observable Behavior > Implementation Interaction

가능하면:

```kotlin
verify { bookingRepository.save(any()) }
```

보다 최종 결과/상태를 검증한다.

```kotlin
savedBooking.status shouldBe BookingStatus.PENDING
persistedSchedule.allocatedTicketCount shouldBe 2
```

Interaction 자체가 외부 contract일 때만 `verify()`를 우선한다.

예:
- Notification 전송
- Audit publication
- 특정 event publication
- 외부 command dispatch

---

## 4. Test Double Constitution

기본 의사결정:

```text
Cheap Deterministic Real
        ↓ unsuitable
Behaviorally Faithful Fake
        ↓ one predetermined response is enough
Stub
        ↓ interaction itself is the contract
Mock
```

이 순서는 절대 종교가 아니라 **기본 판단 순서**다.

### Real

빠르고 deterministic하면 실제 객체를 사용한다.

- Value Object
- Domain Entity
- Calculator
- `Clock.fixed(...)`
- Pure Policy

Value Object/Entity를 mock하지 않는다.

### Fake

상태를 보존하고 실제 API semantics를 합리적으로 재현할 수 있을 때 사용한다.

예:
- `FakeBookingRepository`
- `RecordingEventPublisher`

장점:
- 빠름
- state-based assertion
- refactoring 내성
- reusable

비용:
- production implementation과 drift 가능
- DB transaction/constraint/locking을 재현하지 못함

공유 Fake가 중요해질수록 real implementation과 **contract test 공유**를 검토한다.

### Stub

특정 시나리오에서 predetermined response 하나가 필요할 때 사용한다.

```kotlin
every { performanceReadPort.findById(1L) } returns performance
```

Stub setup이 테스트 본문보다 커지면 test level 또는 SUT design을 재검토한다.

### Mock

호출 자체가 contract일 때 사용한다.

MockK는 테스트의 기본값이 아니다.

---

## 5. Level 1 — Domain Unit Test

BEAT에서 가장 많아야 하는 테스트다.

대상:
- `Schedule`
- `Booking`
- `Ticket`
- `Performance`
- VO
- Calculation
- State Transition
- Domain Invariant

규칙:

```text
Spring ❌
Repository ❌
MockK ❌
Testcontainers ❌
```

예:

```kotlin
class ScheduleTest : FunSpec({

    context("티켓을 예약할 때") {

        test("잔여 수량 이하이면 예약할 수 있다") {
            val schedule = schedule(
                totalTicketCount = 10,
                allocatedTicketCount = 7,
            )

            val result = schedule.reserveTickets(3)

            result.allocatedTicketCount shouldBe 10
        }

        test("잔여 수량을 초과하면 예약할 수 없다") {
            val schedule = schedule(
                totalTicketCount = 10,
                allocatedTicketCount = 7,
            )

            shouldThrow<DomainException> {
                schedule.reserveTickets(4)
            }
        }
    }
})
```

### Property Testing

예제 몇 개보다 invariant가 중요한 경우 Kotest Property를 사용한다.

예:
- `0 <= allocated <= total`
- 유효한 transition만 발생
- 가격 계산이 유효 입력에서 음수가 되지 않음

Property Test는 모든 test를 randomize하기 위한 도구가 아니다.

---

## 6. Level 2 — Application Unit Test

**모든 Service에 Unit Test를 강제하지 않는다.**

Application Service 자체에 다음과 같은 가치 있는 orchestration이 있을 때 작성한다.

- 복잡한 branching
- fallback
- compensation
- retry decision
- best-effort side effect
- collaborator 결과 조합
- 외부 boundary failure policy

단순:

```text
repository.find
→ domain.change
→ repository.save
```

수준이면 Domain Test + Use-case Integration으로 충분할 수 있다.

Application Unit은 Spring 없이 직접 생성한다.

```text
Spring ❌
Real / Fake / Stub / MockK
```

---

## 7. Level 3 — Web Slice

Controller Test의 책임은 business rule이 아니다.

검증:
- URL
- HTTP Method
- JSON binding
- Bean Validation
- Authentication
- Argument Resolver
- Cookie/Header
- Serialization
- Status
- Exception Mapping

기본:

```kotlin
@WebMvcTest(BookingController::class)
class BookingControllerTest : FunSpec({
    ...
})
```

Controller를 `new`하여 method만 직접 호출하는 테스트는 HTTP contract 검증의 기본값으로 사용하지 않는다.

Controller collaborator는 simple Stub/Mock이 합리적이다.

HTTP layer 밖의 business logic을 다시 검증하지 않는다.

---

## 8. Level 4 — Persistence Slice

Repository Adapter 자체의 위험을 검증한다.

```text
@DataJpaTest
+
Real MySQL Testcontainers
```

검증 대상:
- custom JPQL/Kotlin JDSL
- mapping
- constraint
- fetch join
- pagination
- timezone/date mapping
- pessimistic locking
- MySQL-specific semantics

금지:

```text
FakeRepository로 Repository 자체 테스트 ❌
Mock EntityManager ❌
H2로 MySQL correctness 대체 ❌
```

Spring Data가 이미 제공하는 trivial `save/findById/delete` 자체를 확인하기 위한 테스트는 만들지 않는다.

### Persistence Transaction

일반 `@DataJpaTest` rollback은 허용.

그러나:
- concurrency
- lock
- commit timing
- after-commit behavior

에서는 test-level transaction을 사용하지 않는다.

---

## 9. Level 5 — Redis Slice

Redis semantics가 correctness에 영향을 주는 Adapter는 Real Redis로 검증한다.

대상:
- TTL
- `SET NX`
- atomicity
- expiration
- Lua
- serialization

```text
Application Unit
→ Fake Redis Port 가능

Redis Adapter
→ @DataRedisTest + Real Redis Testcontainer
```

---

## 10. External HTTP Adapter

Application Unit:
- Port를 Fake/Stub 가능

Adapter 자체:
- **Real Adapter**
- **Real HTTP Client**
- **MockWebServer/WireMock**

검증:
- URI
- method
- header
- serialization
- deserialization
- timeout
- 4xx/5xx mapping
- malformed response

`@RestClientTest`는 Spring `RestClient` 계열에 적합할 때 사용한다.

OpenFeign은 `@RestClientTest`를 기계적으로 적용하지 않고 실제 Feign config + mock HTTP server integration을 검토한다.

---

## 11. Level 6 — Use-case Integration

BEAT의 **Primary Integration Confidence Source**.

```text
Application
+
Real Domain
+
Real Repository Adapter
+
JPA
+
Real MySQL
(+ Real Redis when semantics matter)
```

기본:

```kotlin
@SpringBootTest
class MemberBookingUseCaseTest : FunSpec({
    ...
})
```

검증:
- orchestration
- transaction
- repository wiring
- JPA mapping
- DB state
- cross-component collaboration

Domain edge case를 모두 여기서 반복하지 않는다.

---

## 12. Level 7 — Acceptance

사용자 관점의 public behavior를 검증한다.

```text
HTTP
→ Controller
→ Application
→ Domain
→ Persistence
→ Real MySQL/Redis
```

공통:

```kotlin
@BeatAcceptanceTest
class BookingAcceptanceTest : FunSpec({
    ...
})
```

`@BeatAcceptanceTest`가 통일해야 할 것:
- profile
- Spring Boot web environment
- container config
- external fake config
- HTTP test client
- lifecycle

목적은 **Spring TestContext cache fragmentation 최소화**다.

Acceptance에서는 내부 Application/Repository를 가능한 한 mock하지 않는다.

외부 제3자 boundary만 controlled fake/stub server를 허용한다.

---

## 13. Level 8 — Dedicated Concurrency

BEAT에서는 필수 1급 test type이다.

규칙:

```text
Real MySQL ✅
Production @Transactional boundary ✅
Deterministic synchronization ✅

FakeRepository ❌
H2 ❌
test-level @Transactional ❌
Thread.sleep synchronization ❌
```

예:
- 10좌석
- 2장씩
- 30 workers

검증:
- 성공 수
- booking row 수
- allocated count
- capacity invariant
- duplicate/overselling absence

단순 request success count로 끝내지 않는다.

---

## 14. E2E Smoke

배포된 프로세스를 외부에서 black-box로 호출한다.

극소수만 유지한다.

후보:
- health
- 회원 예매 happy path
- guest booking → session → retrieve
- admin critical path

E2E로 domain edge case를 반복하지 않는다.

---

## 15. Fixture Strategy — Named DSL + Fixture Monkey + Property

Fixture 전략은 세 도구를 역할별로 분리한다.

### 15.1 Named Kotlin Fixture DSL — 기본

Scenario-defining value가 중요한 테스트에서 사용한다.

```kotlin
val schedule = schedule(
    totalTicketCount = 10,
    allocatedTicketCount = 8,
)
```

적합:
- Domain
- Acceptance
- Concurrency
- 중요한 Use-case

### 15.2 Fixture Monkey — Selective

현재 공식 Quick Start 기준 Kotlin starter `1.2.0`이 공개되어 있다.

적합:
- 필드가 많은 DTO
- deeply nested object graph
- validation test
- bulk persistence data
- 관심 없는 필드를 자동으로 채우고 일부만 override하는 경우

예:

```kotlin
val request = fixtureMonkey
    .giveMeBuilder<BookingRequest>()
    .setExp(BookingRequest::purchaseTicketCount, null)
    .sample()
```

핵심 정책:

> **Scenario-defining values MUST be explicit.**

Fixture Monkey random value가 expected behavior를 결정하게 두지 않는다.

BEAT 전용 global configuration은 **valid-by-default**를 목표로 한다.

Nullable/randomness/default constraints를 명시적으로 통제한다.

### 15.3 Kotest Property

Fixture Monkey는 **객체 생성**, Kotest Property는 **invariant 검증 반복**을 주 책임으로 한다.

필요할 때 둘을 결합할 수 있다.

---

## 16. FunSpec Convention

하나의 기본 style만 사용한다.

```kotlin
class ScheduleTest : FunSpec({

    context("예매 기간을 변경할 때") {

        test("유효한 기간이면 변경된다") {
            // given
            // when
            // then
        }
    }
})
```

규칙:
- `context` = 상황/조건
- `test` = 기대 behavior
- G/W/T는 leaf 내부
- 가능하면 `context -> test`
- nested context depth 최대 2
- mutable spec-level shared state 금지

---

## 17. Kotest + Spring Lifecycle

Spring-bound Kotest test는 Spring TestContext와 Kotest nested lifecycle 차이를 암묵적 default에 방치하지 않는다.

팀 정책:
- Kotest 6.2.4의 **`SpringTestLifecycleMode.Test`(leaf semantics)** 를 표준으로 고정
- test fixture는 leaf 내부에서 생성
- shared mutable spec state 금지
- concurrency test에는 Spring-managed test transaction 사용 금지

---

## 18. Testcontainers Lifecycle

현재 BEAT integration base는 static container를 수동 `.start()`한다.

전면 rewrite에서는 Spring Context가 container를 필요로 하는 integration/acceptance suite에 대해 다음을 선호한다.

```text
@TestConfiguration
+
@Bean
+
@ServiceConnection
```

즉 **Spring-managed Testcontainers**.

목적:
- ApplicationContext cache와 container lifetime 정렬
- duplicated startup 방지
- stable common configuration

Unit test에는 container가 존재하면 안 된다.

---

## 19. Architecture Guard — 반드시 추가

멀티모듈/CQRS Constitution을 문서로만 남기지 않는다.

ArchUnit + Gradle compile-time boundaries를 사용한다.

검증 후보:
- domain → infrastructure 금지
- domain → Spring framework 금지
- application → apps/inbound 금지
- controller → persistence implementation 직접 참조 금지
- module/package cycle 금지
- forbidden dependency 방향

주의:

Architecture Test는 **구조 invariant**만 검증한다.

다음 같은 미세 naming/layout rule까지 강제하지 않는다.

```text
모든 class 이름 Service
package depth 정확히 N
모든 파일 위치를 영구 고정
```

Architecture Test가 리팩터링의 족쇄가 되면 실패한 설계다.

---

## 20. API Compatibility Guard — 반드시 추가

현재 Web Test가 새 contract 자체가 맞는지는 검증해도 이전 client와의 하위호환성을 보장하지 않는다.

CI:

```text
develop OpenAPI
        ↓ diff
PR OpenAPI
```

Breaking change:
- property 제거/rename
- required field 추가
- response/status incompatible change
- endpoint removal

은 explicit approval 없이 실패시키는 방향을 권장한다.

현재는 **OpenAPI diff gate**가 Pact보다 비용 대비 효과가 높다.

Pact/consumer-driven contract는 독립 consumer/service가 늘어났을 때 재검토한다.

---

## 21. Authorization Matrix — 반드시 추가

Actor boundary를 별도 risk로 취급한다.

예:

| Capability | Guest | Member | Maker | Admin |
|---|---:|---:|---:|---:|
| Public Performance Read | O | O | O | O |
| Member Booking | X | O | policy | policy |
| Performance Mutation | X | X | O | O |
| Admin Settlement | X | X | X | O |

실제 matrix는 production capability를 기반으로 작성해야 하며 추측해서 만들지 않는다.

검증 위치:
- authentication/argument resolver → Web Slice
- 실제 authorization wiring → Acceptance/Integration

보안 rule은 일반 business edge case보다 높은 priority를 가진다.

---

## 22. DB Migration Test — schema migration 체계 도입 시 필수

Flyway/Liquibase 또는 동등한 versioned migration이 존재하면:

PR:
```text
Empty Real MySQL
→ all migrations
→ application/repository smoke
```

Release:
```text
Previous Release Schema
→ new migrations
→ application boot / critical queries
```

Migration correctness를 Entity/Repository Test가 대신한다고 간주하지 않는다.

---

## 23. Mutation Testing — Optional Diagnostic

PIT/동등 도구는 line coverage가 아닌 **테스트가 fault를 실제로 잡는지** 확인하는 diagnostic으로 사용한다.

적합:
- Domain policy
- money calculation
- status transition
- boundary condition

운영:
- 모든 PR blocking ❌
- targeted/nightly/audit ✅
- mutation score KPI ❌

Kotlin bytecode 특성을 고려하여 도입 비용을 먼저 측정한다.

---

## 24. Flaky Test Policy

Retry로 flaky를 숨기지 않는다.

특히:
- Domain
- Repository
- Concurrency correctness

는 retry 후 성공을 merge 근거로 삼지 않는다.

Retry가 필요한 경우:
- flaky detection/reporting
- quarantine
- 일부 external environment E2E

Concurrency test는 deterministic synchronization을 사용한다.

---

## 25. Time / Async

Production에서 시간은 `Clock` 등 명시적 dependency로 통제한다.

테스트:
- `Clock.fixed(...)`
- `Thread.sleep()` 금지
- bounded polling / deterministic synchronization 사용

---

## 26. TDD Operating Model

Kent Beck식 핵심은 ritual이 아니라 짧은 feedback loop다.

```text
Test List
→ One Step Test
→ RED
→ GREEN
→ REFACTOR
```

강하게 적용:
- Domain
- pure policy
- bug reproduction

Infrastructure:
- contract를 먼저 정의
- real boundary test
- implementation

E2E는 일상적 TDD driver로 사용하지 않는다.

---

## 27. CI Strategy

처음부터 source set 10개를 만들지 않는다.

초기:
- Kotest/JUnit tags
- Gradle tasks

추천 개념:

```text
PR Fast
├─ Domain Unit
├─ Application Unit
├─ Architecture
└─ Web Slice

PR Integration
├─ JPA/MySQL
├─ Redis
├─ External Adapter
├─ Critical Use-case
└─ API Compatibility

PR Correctness
├─ Critical Acceptance
├─ Authorization Matrix
└─ Deterministic Concurrency

main/release
├─ Migration Verification
└─ E2E Smoke

nightly/periodic
├─ Heavy Concurrency / Stress
└─ Mutation Testing
```

Suite가 충분히 커진 뒤 physical source set 분리를 검토한다.

---

## 28. Coverage Policy

100% line coverage를 목표로 하지 않는다.

Coverage는 **untested important code를 찾는 signal**이다.

Priority:
1. Domain invariant
2. Critical branch
3. Authorization
4. Transaction
5. Lock
6. External contract
7. Failure handling

Kover 숫자 하나를 품질 KPI로 만들지 않는다.

---

## 29. Anti-patterns — MUST NOT

- 모든 테스트에 `@SpringBootTest`
- 모든 Service마다 Fake/Mock Repository Unit Test
- Production Class ↔ Test Class 1:1 강제
- Value Object/Entity mock
- Repository 자체를 Fake로 검증
- MySQL semantics를 H2로 대체
- Redis TTL/NX를 Map Fake만으로 검증
- Concurrency test-level `@Transactional`
- `Thread.sleep()` synchronization
- `verify(repository.save())` 중심 테스트
- Controller direct method call만으로 HTTP 검증
- Acceptance 내부 Application/Repository mock
- 테스트마다 다른 Spring Context configuration
- shared mutable Kotest state
- scenario-defining random fixture
- test order dependency
- 100% coverage KPI
- Fixture Monkey를 모든 fixture의 기본값으로 강제
- Spring Modulith/Pact/Chaos tooling을 현재 필요성 없이 선도입

---

## 30. Decision Algorithm

```text
Q1. 순수 Business Rule인가?
YES → Pure Unit / Property / TDD

Q2. Spring/기술 boundary 자체가 위험인가?
HTTP → @WebMvcTest
JPA/MySQL → @DataJpaTest + MySQL
Redis → @DataRedisTest + Redis
External HTTP → Real Client + Mock Server

Q3. 여러 component/transaction 조합이 위험인가?
YES → @SpringBootTest Use-case Integration

Q4. public user journey가 중요한가?
YES → Acceptance

Q5. Lock/Concurrency인가?
YES → Dedicated Real-MySQL Concurrency

Q6. 배포 runtime 자체를 검증해야 하는가?
YES → E2E Smoke

Cross-cutting:
Architecture rule → ArchUnit
API backward compatibility → OpenAPI diff
Authorization rule → Security Matrix
Schema evolution → Migration Test
```

---

## 31. Final Constitution

### MUST

1. JUnit Platform을 공통 실행 계약으로 유지한다.
2. 신규/재작성 Kotlin test authoring은 Kotest FunSpec을 기본으로 한다.
3. Domain/Pure Application은 Spring 없이 테스트한다.
4. 테스트는 Class가 아닌 Behavioral Risk를 기준으로 작성한다.
5. Observable State/Output을 implementation interaction보다 우선한다.
6. MySQL semantics가 correctness인 경우 Real MySQL을 사용한다.
7. Redis semantics가 correctness인 경우 Real Redis를 사용한다.
8. Concurrency는 production transaction boundary로 검증한다.
9. Spring-bound Kotest lifecycle policy를 공통화한다.
10. Acceptance configuration을 공통화해 context cache를 안정화한다.
11. Architecture boundaries를 자동 테스트한다.
12. API backward compatibility와 Authorization boundary를 CI guardrail로 둔다.
13. Scenario-defining fixture value는 반드시 명시한다.

### SHOULD

1. Cheap deterministic Real → faithful Fake → Stub → Mock 순으로 검토한다.
2. Named Kotlin Fixture DSL을 기본 fixture 표현으로 사용한다.
3. Fixture Monkey는 복잡한 객체/DTO/bulk data에 selective하게 사용한다.
4. Property Test는 invariant 검증에 사용한다.
5. Controller/JPA/Redis는 해당 risk가 있을 때 focused slice를 우선한다.
6. 외부 HTTP Adapter는 real client + protocol-level mock server로 검증한다.
7. `Clock`과 deterministic synchronization을 사용한다.
8. 공유 Fake가 중요하면 contract test를 검토한다.

### MUST NOT

1. Mock framework를 test double의 기본값으로 사용하지 않는다.
2. 동일 business rule을 여러 계층에서 반복하지 않는다.
3. Fake가 real infrastructure semantics를 증명한다고 간주하지 않는다.
4. Fixture generator random value가 expected behavior를 결정하게 하지 않는다.
5. 의미 없는 framework/tool을 “성숙해 보이기 위해” 추가하지 않는다.

---

## 32. Final Trade-offs

| Choice | Gain | Cost | Decision |
|---|---|---|---|
| Kotest FunSpec | Kotlin readability, uniform style | Spring/JUnit lifecycle 차이 학습 | Adopt |
| Real Object | Fidelity, refactor safety | heavy dependency면 비용 | First choice |
| Fake | fast stateful behavior | drift/maintenance | Selective |
| Stub/MockK | cheap scenario/failure | coupling/brittleness | Narrow use |
| Fixture Monkey | boilerplate 감소, complex graph | hidden randomness/abstraction | Selective |
| Property Test | invariant 탐색 | debugging/seed 이해 필요 | Domain selective |
| Spring Slice | focused context | config complexity | Boundary default |
| Real MySQL/Redis | production fidelity | container startup | Mandatory where semantics matter |
| Use-case Integration | high business confidence | slower/failure localization | Core |
| Acceptance | public journey confidence | fixture/context cost | Few critical |
| Concurrency | proves overselling/lock | runtime cost | Mandatory for booking |
| Architecture Test | stops structural drift | over-rule risk | Mandatory, coarse rules |
| OpenAPI Diff | protects clients | versioning governance needed | Mandatory |
| Migration Test | deployment confidence | setup/runtime | Add with migrations |
| Mutation | test quality signal | CPU/tool cost | Targeted/nightly |
| E2E | runtime confidence | slow/flaky | Very few |

---

## 33. Final Decision

BEAT의 최종 테스트 전략은 **도구의 수가 많은 전략이 아니라, 실패 클래스마다 가장 작은 faithful test 하나가 ownership을 가지는 전략**이다.

```text
Business Rule          → Unit
HTTP Contract          → Web Slice
DB Contract            → Real MySQL Slice
Redis Contract         → Real Redis Slice
External Protocol      → Real Client + Mock Server
Use-case Composition   → Integration
User Journey           → Acceptance
Overselling / Lock     → Concurrency
Architecture Drift     → ArchUnit
API Breaking Change    → OpenAPI Diff
Authorization          → Security Matrix
Schema Evolution       → Migration Test
Deployment Runtime     → E2E Smoke
```

테스트를 추가할 때 질문은 하나다.

> **“이 실패는 기존 테스트 중 누가 소유하고 있으며, 없다면 가장 싸고 충실하게 재현할 수 있는 테스트는 무엇인가?”**

이 원칙을 지키면 테스트 suite는 커져도 중복되지 않고, 리팩터링에 강하며, BEAT에서 실제로 위험한 MySQL locking·Redis semantics·authorization·API compatibility·멀티모듈 architecture를 높은 fidelity로 보호할 수 있다.

---

## References

### Official
- Spring Boot 4.1 Testing: https://docs.spring.io/spring-boot/reference/testing/
- Spring Boot Test Modules: https://docs.spring.io/spring-boot/reference/testing/test-modules.html
- Spring Boot Test Slices: https://docs.spring.io/spring-boot/appendix/test-auto-configuration/slices.html
- Spring Framework Testing: https://docs.spring.io/spring-framework/reference/testing.html
- Kotest Spring Extension: https://kotest.io/docs/extensions/spring.html
- Kotest: https://kotest.io/
- Fixture Monkey: https://naver.github.io/fixture-monkey/
- Fixture Monkey GitHub: https://github.com/naver/fixture-monkey
- ArchUnit: https://www.archunit.org/
- PIT: https://pitest.org/

### Industry References
- Google Software Engineering — Testing Overview / Test Doubles:
  https://abseil.io/resources/swe-book/html/ch11.html
  https://abseil.io/resources/swe-book/html/ch13.html
- Toss Test Strategy:
  https://toss.tech/article/test-strategy-server
- DoorDash Functional Testing:
  https://careersatdoordash.com/blog/how-to-boost-code-coverage-with-functional-testing/

### Books / Principles
- Kent Beck, *Test-Driven Development: By Example*
- Tom Long, *Good Code, Bad Code*
