# BEAT-SERVER Architecture — Final SSOT

> **Status:** Final / Adopted  
> **Baseline:** 2026-08-25 · `TEAM-BEAT/BEAT-SERVER` · `develop`  
> **Style:** Pragmatic Clean Architecture + Ports & Adapters + CQRS  
> **Decision:** 11 Product Modules Freeze  
> **Audience:** Backend engineers, reviewers, AI coding agents

---

# AI Summary — Read First

> 아래 YAML은 빠른 판단용이다. 정본은 Axis 1~7의 MUST / MUST NOT 규칙이다.

```yaml
architecture:
  status: ADOPTED
  product_modules: 11
  graph: DAG
  runtime_transitive_classpath_full_isolation: NOT_A_GOAL
  framework_free_application: NOT_A_GOAL

direct_dependencies:
  domain: []
  application:frontoffice: [domain]
  application:admin: [domain]
  application:system: [domain]
  support:security: [application:frontoffice, support:observability]
  support:security-web: [support:security, support:observability]
  support:observability: []
  infrastructure:
    required: [domain, application:frontoffice, application:admin]
    conditional:
      application:system: real_system_port_implementation_exists
  apps:api: [application:frontoffice, infrastructure, support:security-web, support:observability]
  apps:admin: [application:admin, infrastructure, support:security-web, support:observability]
  apps:batch: [application:system, infrastructure, support:observability]

test_only_dependencies:
  apps:api: [domain]
  apps:admin: [domain]
  apps:batch: [domain]

port_contract:
  owner: consumer
  signature_types_allowed: [owning_application_lane, domain, JDK_or_Kotlin_standard]
  signature_types_forbidden: [support, infrastructure, framework, external_SDK]

security:
  business_authentication_workflow: application:frontoffice
  web_independent_security_and_port_adapter: support:security
  spring_security_servlet_integration: support:security-web
  runtime_feature_route_cookie_origin_policy: apps
  route_role_policy: apps
  business_authorization: application_or_domain
  infrastructure_security_module: NOT_ADOPTED
  twelfth_security_core_module: DEFERRED

cqrs:
  classification: business_intent
  aggregate_read: JPA
  decision_read: JPA
  view_read: jOOQ
  command_may_select: true
  query_side_location: infrastructure/persistence/query
  jooq_type_escape: FORBIDDEN
```

### AI placement rule

```text
Invariant / Aggregate / Entity / VO
  -> domain

Use Case / transaction / Port / ReadModel
  -> application:<lane>

HTTP DTO / Controller / Facade / OpenAPI / route policy
  -> apps:<runtime>

JWT / BCrypt / token mechanism /
Frontoffice security Port implementation
  -> support:security

Generic Spring Security Filter / SecurityContext /
CurrentMember / Servlet integration
  -> support:security-web

Runtime/feature-specific route or cookie-origin policy Filter
  -> apps:<runtime>

JPA / jOOQ / Redis / S3 / SMS / external API adapter
  -> infrastructure

Logging / MDC / tracing / metrics
  -> support:observability
```

---

# Axis 1. System Shape & Dependency Direction

## 1.1 11 Product Modules — ADOPTED

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
├── security-web
└── observability
```

`build-logic`은 included build이며 Product Module 수에 포함하지 않는다.

현재 만들지 않는 module:

```text
support:security-core
adapter:security
infrastructure:security
bootstrap:api
bootstrap:admin
bootstrap:batch
module-contracts
```

## 1.2 Dependency Rule — MUST

```text
Outer Adapter
    ↓
Application
    ↓
Domain
```

Runtime 호출 방향과 compile dependency는 같을 필요가 없다.

```text
Runtime:
AuthenticationCommandService
  -> TokenIssuer
  -> JwtTokenProvider

Compile:
support:security
  -> application:frontoffice
```

Application은 `JwtTokenProvider`를 import하지 않는다.

## 1.3 Canonical Direct Dependency Graph

> **이 절이 Gradle direct dependency의 단일 정본이다.**

```text
domain
    -> (없음)

application:frontoffice
    -> domain

application:admin
    -> domain

application:system
    -> domain

support:security
    -> application:frontoffice
    -> support:observability

support:security-web
    -> support:security
    -> support:observability

support:observability
    -> (없음)

infrastructure
    -> domain
    -> application:frontoffice
    -> application:admin
    -> application:system      # conditional: 실제 System Port 구현이 있을 때만

apps:api
    -> application:frontoffice
    -> infrastructure
    -> support:security-web
    -> support:observability
    -> domain                  # test only

apps:admin
    -> application:admin
    -> infrastructure
    -> support:security-web
    -> support:observability
    -> domain                  # test only

apps:batch
    -> application:system
    -> infrastructure
    -> support:observability
    -> domain                  # test only
```

`infrastructure -> application:system`은 **Allowed Conditional Dependency**다. 실제 System Port 구현이 없으면 dependency도 추가하지 않는다.

## 1.4 Graph & Lane Invariants

MUST:
- 전체 Gradle graph는 DAG다.
- Application lane은 서로 직접 의존하지 않는다.
- Apps는 자신의 Application lane만 직접 의존한다.

MUST NOT:

```text
application:frontoffice -> application:admin/system
application:admin       -> application:frontoffice/system
application:system      -> application:frontoffice/admin

apps:api   -> application:admin/system
apps:admin -> application:frontoffice/system
apps:batch -> application:frontoffice/admin
```

### Transitive classpath

**Status: NOT_A_GOAL**

다음 transitive path 자체는 허용한다.

```text
apps:admin
  -> support:security-web
  -> support:security
  -> application:frontoffice
```

```text
apps:admin
  -> infrastructure
  -> application:frontoffice
```

BEAT가 강제하는 것은 **direct Gradle dependency / direct source dependency / package-level dependency**다. Classpath에 타입이 존재하는 것과 해당 source가 그 타입을 사용하도록 허용하는 것은 다르다.

---

# Axis 2. Module Ownership

| Module | Owns | Must Not Know / Own |
|---|---|---|
| `domain` | Aggregate, Entity, VO, pure Domain Service, invariant | Spring, JPA, jOOQ, Redis, HTTP, Security, Application, Infrastructure |
| `application:*` | Use Case, transaction, consumer-owned Port, ReadModel, orchestration | JPA/jOOQ/Redis, SecurityContext, Servlet, HTTP DTO, outer implementation |
| `infrastructure` | JPA, jOOQ View Query, Redis, S3, SMS, external adapters | Apps, Security Web |
| `support:security` | Web-independent JWT/password capability + Frontoffice Security Port implementation | Web/Servlet responsibility, business workflow |
| `support:security-web` | Spring Security/Servlet inbound integration | Application direct dependency, Domain, Infrastructure |
| `support:observability` | logging, MDC, tracing, metrics | business rule, use case, repository, security policy |
| `apps:*` | HTTP/batch entrypoint, DTO, Controller/Facade, route policy, Composition Root | business invariant, direct persistence logic |

## 2.1 Application rules

Application MAY use:

```text
@Service
@Transactional
Spring DI
java.time.Clock
Domain Repository
Application-owned Port
Application-owned ReadModel
```

Application MUST NOT use:

```text
JPA Entity / Spring Data Repository / EntityManager
DSLContext / jOOQ generated type
RedisTemplate
AWS SDK / external client implementation
Spring Security / SecurityContext
Servlet
HTTP Request / Response DTO
support:security implementation
infrastructure implementation
```

Frontoffice package ownership:

```text
Capability
  -> Actor
      -> command / query
```

예:

```text
application/frontoffice
├── booking/booker/{command,query}
├── performance/booker/query
├── performance/maker/{command,query}
├── schedule/booker/query
├── home/booker/query
├── ticket/maker/{command,query}
├── member
└── auth
```

`admin`, `system`은 lane 자체가 actor/execution context이므로 actor package를 기계적으로 중복하지 않는다.

## 2.2 Apps = Inbound Adapter + Composition Root

`apps:* -> infrastructure`는 wiring 목적으로 허용한다.

Controller/Facade는 Infrastructure implementation을 직접 business collaborator로 사용하면 안 된다.

```kotlin
@RestController
class BookingController(
    private val bookingJpaRepository: BookingJpaRepository, // MUST NOT
)
```

정상 흐름:

```text
Controller
  -> Facade
  -> Application Use Case
```

Facade는 delivery helper다.

MAY:
- Request -> Application input mapping
- Application Result -> Response mapping
- thin transport-level coordination

MUST NOT:
- transaction
- invariant
- business authorization
- Repository/JPA/jOOQ/Redis access

---

# Axis 3. Port & Security Boundaries

## 3.1 Consumer Owns Port — MUST

```text
application:frontoffice
├── TokenIssuer
├── RefreshTokenAuthenticator
├── PasswordHasher / PasswordVerifier
├── RefreshTokenStore
└── GuestSessionStore
```

Adapter examples:

```text
support:security
├── JwtTokenProvider
└── BCryptPasswordHasher

infrastructure
├── RedisRefreshTokenAdapter
└── RedisGuestSessionAdapter
```

정본:

```text
Consumer owns Port.
Adapter depends on Port owner.
```

## 3.2 Port Signature Ownership — MUST

Port interface의 위치만 Application에 두는 것으로는 충분하지 않다. Parameter / return type도 contract owner가 통제한다.

ALLOWED:

```text
same Application lane-owned type
Domain type
JDK / Kotlin standard type
```

FORBIDDEN:

```text
support-owned DTO/value
infrastructure-owned DTO/value
JPA/jOOQ/Redis type
Spring Security/Servlet type
external SDK type
```

잘못된 예:

```kotlin
interface TokenIssuer {
    fun issue(subject: SupportOwnedTokenSubject): String
}
```

올바른 예:

```kotlin
data class TokenIssueCommand(
    val memberId: Long,
    val role: String,
)

data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
)

interface TokenIssuer {
    fun issue(command: TokenIssueCommand): IssuedTokens
}
```

### Security-like contract types

다음 타입이 Application Port signature에 등장한다면 이름과 무관하게 **`application:frontoffice`가 소유**한다.

```text
TokenSubject
TokenAuthenticationResult
TokenAuthenticationFailure
```

`support:security`는 별도의 internal JWT claims / parser result 같은 technical model을 가질 수 있다.

```text
Application contract
    ↓ mapping
support:security internal technical model
```

Outer technical type이 Application Port boundary를 역으로 통과하면 안 된다.

## 3.3 `support:security`

정의:

> **Web-independent Security Capability + Frontoffice Security Port Adapter**

MAY own:

```text
JwtTokenProvider
JwtTokenIssuer
JwtTokenParser
JwtSigningKeyHolder
BCryptPasswordHasher
JWT claims / crypto internal model
Web-facing security technical API
```

Dependency:

```text
support:security
  -> application:frontoffice
  -> support:observability
```

이것은 Outer Adapter가 Inner Port를 구현하는 정상 Dependency Inversion이다.

```text
application:frontoffice -> support:security   MUST NOT
```

## 3.4 Security Public Surface Separation — MUST

`support:security`의 소비 방향을 분리한다.

```text
A. Application-facing adapter implementation
   - Frontoffice Port 구현
   - Application-owned contract 사용

B. Security-Web-facing technical capability
   - security-web이 소비
   - support:security-owned technical type 사용
```

MUST NOT:

```text
support:security의 Web-facing API
  -> Application Port type 노출
  -> security-web이 application:frontoffice를 직접 알아야 함
```

`support:security-web -> support:security`만으로 compile 가능한 API surface를 유지한다.

## 3.5 `support:security-web`

Owns:

```text
JwtAuthenticationFilter
SecurityMdcLoggingFilter
CurrentMember
CurrentMemberArgumentResolver
MemberAuthentication
AdminAuthentication
AuthenticationEntryPoint
AccessDeniedHandler
SecurityFilterChain bootstrap
Servlet / Web MVC integration
```

`support:security-web`에는 범용 인증·Servlet 연동만 둔다. 특정 런타임의 경로 또는 쿠키 origin 정책처럼
feature context를 알아야 하는 Filter는 해당 `apps:*`가 소유한다.

Direct dependency:

```text
support:security
support:observability
```

MUST NOT directly depend on:

```text
application:*
domain
infrastructure
apps:*
```

## 3.6 Authentication / Authorization Ownership

```text
Business authentication workflow
  -> application:frontoffice

Web-independent token/password mechanism
  -> support:security

HTTP authentication
  -> support:security-web

Coarse route/role authorization
  -> apps:* + support:security-web

Business authorization
  -> application + domain
```

Business authorization을 Spring Security annotation으로 대체하지 않는다.

Route/role policy는 각 runtime이 소유한다.

```text
apps:api/config/*SecurityConfig
apps:admin/config/*SecurityConfig
```

## 3.7 Security placement decision

```text
support:security         ADOPTED
support:security-web     ADOPTED

infrastructure:security  NOT_ADOPTED
adapter:security         NOT_ADOPTED
```

Security를 Infrastructure에 두는 것도 Clean Architecture상 가능한 해석이지만 BEAT는 별도 Support Capability를 사용한다.

**이름보다 dependency direction과 Port ownership이 우선한다.**

---

# Axis 4. Runtime Composition

## 4.1 Inbound Authentication

```text
HTTP
  ↓
support:security-web
  ↓
support:security
  ↓
apps Controller / Facade
  ↓
application
```

이 그림은 runtime 요청 인증 흐름이다.

## 4.2 Outbound Ports

```text
Application Use Case
  ↓
Application-owned Port
  ↓ runtime dispatch
  ├─ support:security adapter
  │    └─ JWT / BCrypt
  │
  └─ infrastructure adapter
       ├─ JPA
       ├─ Redis
       └─ External API
```

Compile dependency는 반대로 Outer -> Inner contract owner를 향한다.

## 4.3 Apps runtime responsibilities

```text
apps:api
  -> Frontoffice HTTP API / route policy / wiring

apps:admin
  -> Admin HTTP API / route policy / wiring

apps:batch
  -> batch/scheduled entrypoint / System use case triggering / wiring
```

`apps:batch`는 `support:security-web`을 의존하지 않는다.

## 4.4 Test-only Domain dependency

```text
apps:* main -> domain    MUST NOT
apps:* test -> domain    MAY
```

Architecture assertion, fixture, domain test helper에만 허용한다.

---

# Axis 5. CQRS & Persistence

## 5.1 Classification

Command / Query는 **business intent**로 분류한다.

```text
SELECT == Query Side
```

가 아니다.

## 5.2 Read Taxonomy

| Read | Meaning | Side | Default |
|---|---|---|---|
| Aggregate Read | Command를 위한 authoritative Aggregate load | Command | JPA |
| Decision Read | Command correctness를 위한 authoritative fact | Command | JPA |
| View Read | consumer-facing projection | Query | jOOQ |

Decision Read examples:

```text
ownership
membership
availability
existence
```

View Read examples:

```text
home
booking history
maker dashboard
admin view
cross-table projection
```

## 5.3 Command Correctness — MUST

Command correctness가 다음에 의존하면 안 된다.

```text
Presentation ReadModel
Replica
Eventual Cache
Search Projection
```

Command response는 다음에서 만든다.

```text
changed Aggregate
authoritative fact
CommandResult
```

Query Side read-after-write는 기본 패턴이 아니다.

## 5.4 ReadModel Boundary

```text
ReadModel != HTTP Response
```

Application:

```text
BookingHistoryReadModel
```

Apps:

```text
BookingHistoryResponse
```

Query identity는 endpoint path가 아니라 semantic use case 기준이다.

## 5.5 Naming

View Read:

```text
*QueryService
*Reader
*ReadPort
*ReadModel
*Projection
*Queries
```

Decision Read:

```text
*Provider
*Availability
*Membership
*Ownership
*Policy
```

이름보다 semantic classification이 우선한다.

## 5.6 View Query Physical Location

```text
infrastructure/persistence/query
├── booking/booker
├── performance/booker
├── performance/maker
├── schedule/booker
├── home/booker
└── ticket/maker
```

`persistence/query`는 **View Read / jOOQ Query Side 전용**이다.

Aggregate / Decision SELECT는 authoritative JPA adapter에 남는다.

## 5.7 jOOQ Boundary — MUST

Infrastructure 밖으로 유출 금지:

```text
DSLContext
generated Table
generated Record
Condition
Select*
jOOQ Result
```

Infrastructure에서 즉시 Application-owned ReadModel로 매핑한다.

## 5.8 Aggregate Ownership != Persistence Package

예:

```text
Performance
├── Cast
├── Staff
└── PerformanceImage
```

독립 Aggregate가 아니어도 persistence package는 sibling일 수 있다.

```text
persistence
├── performance
├── cast
├── staff
└── performanceimage
```

Package가 sibling이라는 이유로 독립 Domain Repository / Application Port를 만들지 않는다.

Authoritative lifecycle은 Aggregate Repository adapter가 소유한다.

---

# Axis 6. Enforcement & AI Review Gates

## 6.1 Status Model

Architecture decision과 enforcement를 분리한다.

Decision:

```text
ADOPTED      확정
DEFERRED     Trigger 전까지 의도적으로 하지 않음
NOT_A_GOAL   현재 목표가 아님
NOT_ADOPTED  검토했으나 현재 채택하지 않음
```

Enforcement:

```text
AUTOMATED
  자동 실패 장치가 존재.

MANUAL_REVIEW
  결정은 확정됐지만 자동 검증 범위 밖.

CONDITIONAL_AUTOMATION
  해당 기술/경계 도입 시 Guard를 함께 추가해야 함.
```

> **`ENFORCED`라는 표현은 자동 실패 장치와 evidence가 있을 때만 사용한다.**

## 6.2 Enforcement Matrix

| ID | Rule | Decision | Enforcement | Evidence |
|---|---|---|---|---|
| E-01 | Product Module = 11 | ADOPTED | AUTOMATED | `verifyTargetModuleGraph` project allowlist |
| E-02 | Gradle graph = DAG | ADOPTED | AUTOMATED | `verifyTargetModuleGraph` exact acyclic allowlist validation |
| E-03 | Direct dependency allowlist | ADOPTED | AUTOMATED | `verifyTargetModuleGraph` allowed graph |
| E-04 | `application:* -> domain only` | ADOPTED | AUTOMATED | Gradle guard + Application architecture tests |
| E-05 | Application technical dependency ban | ADOPTED | AUTOMATED | Application ArchUnit rules |
| E-06 | Direct lane crossing ban | ADOPTED | AUTOMATED | Gradle guard + ArchUnit |
| E-07 | Controller/Facade -> Infra implementation ban | ADOPTED | AUTOMATED | Apps architecture guards |
| E-08 | jOOQ containment | ADOPTED | AUTOMATED | `verifyJooqContainment` + `InfrastructureJooqArchitectureTest` (query JPA/JDSL/JdbcTemplate ban) + Application/Domain/Apps ArchUnit jOOQ import bans; `beat.jooq-codegen:validateJooqSchema`는 schema/generated artifact guard |
| E-09 | Port owner = consumer | ADOPTED | MANUAL_REVIEW | Architecture review gate |
| E-10 | Port signature outer-type leakage ban | ADOPTED | MANUAL_REVIEW | Architecture review gate |
| E-11 | `security-web` direct Application dependency ban | ADOPTED | AUTOMATED | Gradle guard + Security architecture tests |
| E-12 | Minimum implementation visibility | ADOPTED | MANUAL_REVIEW | Kotlin visibility/package review |

### Visibility invariant

`internal` 자체가 Architecture invariant는 아니다.

정본:

> **Outer implementation은 wiring에 필요한 최소 visibility만 공개한다.**

```text
internal로 wiring 가능
  -> internal 권장

composition/wiring에 public이 실제 필요
  -> public 허용
```

“모든 Adapter는 무조건 internal”로 해석하지 않는다.

## 6.3 AI Review Algorithm

```text
1. capability / actor / command-query는 무엇인가?
2. contract consumer는 어느 module인가?
3. Port가 필요한가?
4. Port interface를 consumer가 소유하는가?
5. parameter/return type도 Application/Domain/JDK ownership인가?
6. 새 direct dependency가 Axis 1.3 graph에 있는가?
7. conditional dependency라면 trigger가 실제 충족됐는가?
8. 다른 Application lane을 source에서 직접 참조하는가?
9. 기술/framework 타입이 Application/Domain으로 새는가?
10. Read는 Aggregate / Decision / View 중 무엇인가?
11. View가 아닌데 persistence/query로 보내고 있지 않은가?
12. 새 module/dependency가 실제 문제보다 미래 추측 때문에 생기는가?
```

위반이 있으면 구현보다 boundary 수정이 먼저다.

---

# Axis 7. Deferred Decisions & Change Protocol

| Decision | Status | Trigger |
|---|---|---|
| 12th `support:security-core` | DEFERRED | JWT/password primitive의 타 서비스 재사용, 독립 artifact/lifecycle/versioning 필요 |
| Runtime classpath full lane isolation | NOT_A_GOAL | runtime dependency conflict, 독립 deployment, 보안상 classpath isolation 필요 |
| `bootstrap:*` modules | DEFERRED | Composition Root의 독립 ownership/lifecycle 필요 |
| Physical Command/Query DB split | DEFERRED | 실제 scale/consistency 요구 발생 |

## 7.1 12th `security-core`

현재 얻을 수 있는 추가 실익은:

```text
security primitive의 BEAT dependency 0
독립 artifact
타 서비스 재사용
독립 lifecycle/versioning
```

이 실익이 실제로 발생하기 전에는 `support:security`에서 유지한다.

## 7.2 Physical CQRS

현재:

```text
CQRS = semantic / code boundary
not necessarily separate database
```

Replica, 별도 Query DB, event projection은 실제 요구가 있을 때 설계한다.

## 7.3 Module Addition Protocol

MUST NOT:

```text
"나중에 필요할 것 같아서"
"더 Clean해 보여서"
"책에서 layer가 하나 더 있어서"
```

MUST:

새 Gradle module이 아래 중 최소 하나를 실제로 강제해야 한다.

```text
독립 ownership
독립 lifecycle
독립 dependency set
독립 deployment
명확한 compile-time isolation value
```

11-module Freeze 변경은 Architecture Decision 변경이 선행되어야 한다.

---

# Final Architecture Invariants

```text
I-01  Product Module = 11.
I-02  Gradle graph = DAG.
I-03  domain -> BEAT project = none.
I-04  application:* -> domain only.
I-05  Application lane direct crossing = forbidden.

I-06  Port owner = consumer.
I-07  Port signature owner = Application / Domain / JDK.
I-08  Outer/framework type leakage into Port = forbidden.

I-09  support:security = Web-independent Security + Frontoffice Port Adapter.
I-10  support:security-web = Spring Security / Servlet inbound integration.
I-11  security-web -> application:* direct dependency = forbidden.
I-12  infrastructure:security = not adopted.

I-13  apps:* = Inbound Adapter + Composition Root.
I-14  Controller/Facade -> Infrastructure implementation = forbidden.
I-15  Shared outer-module transitive classpath = allowed.
I-16  Direct source/package lane crossing = forbidden.

I-17  Aggregate/Decision Read default = JPA.
I-18  View Read default = jOOQ.
I-19  SELECT != Query Side.
I-20  persistence/query = View Read only.
I-21  jOOQ type escape from Infrastructure = forbidden.
I-22  Aggregate ownership != persistence package nesting.

I-23  Implementation visibility = minimum required for wiring.
I-24  Future speculation alone cannot justify a module/dependency.
```

---

# One-line Definition

> **BEAT-SERVER는 Domain을 완전 독립시키고, Application이 Domain과 consumer-owned Port만 소유하며, Security/Infrastructure 같은 바깥 Adapter가 그 Port를 구현하고, Web Security를 별도 Support module로 분리하며, `apps:*`가 Composition Root로 runtime을 조립하는 11-module Pragmatic Clean Architecture다.**
