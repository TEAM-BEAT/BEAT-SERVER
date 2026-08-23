# BEAT 멀티모듈 & CQRS 아키텍처 최종 결정 (ADR)

네. 이번에는 이 구조를 최종 후보라고 가정하고, 반대 방향의 아키텍처들을 다시 대입해서 깨뜨려 본 뒤 판단했습니다.

결론부터 확정하겠습니다.

BEAT에서는 api / admin / batch를 실행 모듈로 유지하고, domain / module-contracts / infrastructure / security / observability / web-support 정도만 Gradle의 강한 경계로 둔 뒤, 비즈니스 코드는 Capability → Actor → Command/Query 방향으로 조직하는 것이 현재 조건에서 가장 적합합니다.

core:application, core:ports, domain-command/domain-query, Booker/Maker별 Gradle 모듈은 만들지 않는 쪽으로 최종 판단합니다.

앞으로 새로운 사실이 나오지 않는 이상 이 기본 결론을 다시 뒤집을 이유가 없습니다. 제가 앞서 왔다 갔다 한 이유는 Gradle module boundary, Domain boundary, Actor boundary, CQRS boundary라는 서로 다른 네 가지 축을 같은 레벨에서 해결하려고 했기 때문입니다. 그걸 분리하고 나니 답이 훨씬 명확해졌습니다.

---

### 1. 먼저, 첨부해주신 자료를 정말 전부 참고했는가?

이번 메시지에 적어주신 11개 레퍼런스 기준으로는 최대한 다시 전부 확인했습니다. 다만 “11개를 전부 원문 전체 정독했다”고 말하면 정확하지 않습니다.

| Phase | 자료 | 이번 검증 상태 | 실제 판단에 사용 |
|---|---|---|---|
| 1 | 우아한형제들 멀티모듈 설계 | 원문 확인 | ★★★★★ |
| 1 | 권용근 우아한 멀티모듈 YouTube | 영상 transcript 직접 수집 불가 | ★★★ |
| 2 | Baeldung Spring CQRS | 원문 확인 | ★★★★ |
| 2 | Baeldung Spring Modulith CQRS | 원문 확인 | ★★★★ |
| 2 | 우아한형제들 회원 이벤트 아키텍처 | 원문 확인 | ★★★★ |
| 3 | Spring Modulith Official | 공식 최신 문서 확인 | ★★★★★ |
| 3 | Baeldung Spring Boot Multi Module | 원문 확인 | ★★ |
| 4 | Netflix TechBlog | 링크가 특정 아키텍처 글이 아닌 포털 | 보조 참고 |
| 4 | Uber modularization | 지정 URL 직접 접근 실패, 관련 Uber 공식 RIB/Plugin 자료 확인 | ★★★ |
| 4 | Google Modularization | 2026 최신 공식 문서 확인 | ★★★★ |
| 5 | jojoldu Gradle Multi Project | 원문 확인 | ★★ |

특히 우아한형제들 2019 글은 이번 판단의 가장 중요한 자료 중 하나입니다. 이 글은 멀티모듈의 핵심을 “공통 코드를 재사용하는 것”이 아니라 각 모듈에 독립적인 의미와 추상화 계층을 부여하고 의존 방향을 제한하는 것으로 설명합니다. 또한 Application Module은 하위 도메인·내부·공통 모듈을 조립해 서비스 비즈니스를 완성하는 실행 가능한 모듈로 설명합니다. ([우아한형제들 기술블로그 배달의민족을 만드는 기술 이야기 |](https://techblog.woowahan.com/2637/))

Google의 2026년 3월 최신 가이드도 같은 결론입니다. 모듈은 high cohesion/low coupling을 가져야 하지만, 너무 세분화하면 build complexity와 boilerplate가 오히려 유지보수성을 해칩니다. ([Android Developers](https://developer.android.com/topic/modularization/patterns?utm_source=chatgpt.com))

그래서 이번 최종안은 특정 한 블로그 구조를 복사한 것이 아니라 이 자료들의 공통 원칙을 BEAT에 적용한 결과입니다.

---

### 2. 이제 BEAT의 Architecture를 네 축으로 분리하면 답이 나온다

앞으로 architecture를 볼 때 이것만 구분하면 됩니다.

```text
┌─────────────────────────────────────────────┐
│ 1. Deployment / Runtime Boundary            │
│    api / admin / batch                      │
├─────────────────────────────────────────────┤
│ 2. Gradle Compile Boundary                  │
│    domain / contracts / infra / support     │
├─────────────────────────────────────────────┤
│ 3. Business Boundary                        │
│    booking / performance / ticket / ...     │
├─────────────────────────────────────────────┤
│ 4. Use-case Responsibility                  │
│    Booker / Maker / Admin                   │
│         ↓                                   │
│    Command / Query                          │
└─────────────────────────────────────────────┘
```

이 네 개를 하나의 Gradle module tree에 모두 표현하려고 하면 과설계가 됩니다.  
이게 지금까지 가장 중요한 결론입니다.

---

### 3. 1차 경계 — Runtime은 api / admin / batch

이건 유지합니다.

현재 BEAT에는 실제로:
- apis
- admin
- batch

세 개의 Spring Boot executable이 있고 각각 별도의 build dependency를 가집니다. apis/admin은 Web/Security/OpenAPI를 가지고, batch는 HTTP API용 security stack을 갖지 않는 등 실제 runtime 책임도 다릅니다.

우아한형제들의 정의에도 이게 잘 맞습니다. Application Module은 실행 가능한 애플리케이션으로서 하위 모듈을 조립하여 서비스 비즈니스를 완성하는 계층입니다. ([우아한형제들 기술블로그 배달의민족을 만드는 기술 이야기 |](https://techblog.woowahan.com/2637/))

따라서:

```text
apps
├── api
├── admin
└── batch
```

는 좋습니다.

---

### 4. 왜 Booker / Maker를 별도 Gradle/실행 모듈로 안 나누는가

여기도 이제 확정적으로 안 나눕니다.

사용자 유형은 분명히:
- Booker : 공연 조회 / 예매 / 결제
- Maker : 공연 등록 / 회차 관리 / 정산
- Admin : 플랫폼 운영

세 종류입니다.

하지만:  
**Actor boundary ≠ Runtime boundary**  
입니다.

현재 Booker와 Maker는 동일한 public API runtime과 인증 체계 안에서 동작하고, 실제 코드에서도 Maker 공연 기능이 apis에 포함되어 있습니다. 예를 들어 현재 PerformanceController에는 Maker의 공연 생성·수정·삭제와 Booker의 공연 조회가 함께 존재합니다.

Maker를 별도 executable로 만들려면 최소한:
- 별도 scaling
- 별도 배포 주기
- 별도 장애 격리
- 별도 dependency set
- 별도 security realm
- 별도 팀 ownership

중 의미 있는 이유가 있어야 합니다.  
현재는 그 증거보다 운영 비용 증가가 더 큽니다.

Google도 module granularity를 코드베이스의 실제 규모와 복잡성에 맞춰야 하고 지나친 세분화를 피하라고 명시합니다. ([Android Developers](https://developer.android.com/topic/modularization))

그러므로:

```text
Booker + Maker
      ↓
   apps:api

    Admin
      ↓
  apps:admin
```

의 비대칭이 오히려 정상입니다.

---

### 5. 그런데 Booker와 Maker는 코드에서는 반드시 분리한다

이 부분은 사용자께서 처음 짚은 게 맞습니다.

지금 PerformanceController 하나가:
- Maker
  - 공연 생성
  - 공연 수정
  - 공연 삭제
  - 내 공연 조회
- Booker
  - 공연 상세
  - 예매용 공연 조회

를 동시에 가지고 있습니다.

이건 분리하는 게 좋습니다.

다만 최상단을:
- booker/
- maker/

로 만들지는 않습니다.

**Business Capability가 먼저입니다.**

최종적으로 저는 이 순서를 확정하겠습니다.

```text
Capability
    ↓
  Actor
    ↓
  Layer
    ↓
  CQRS
```

예:

```text
apps/api
└── src/main/kotlin/com/beat/api
    │
    ├── performance
    │   │
    │   ├── booker
    │   │   ├── api
    │   │   ├── facade
    │   │   └── application
    │   │       └── query
    │   │
    │   └── maker
    │       ├── api
    │       ├── facade
    │       └── application
    │           ├── command
    │           └── query
    │
    ├── booking
    │   │
    │   ├── booker
    │   │   ├── api
    │   │   ├── facade
    │   │   └── application
    │   │       ├── command
    │   │       └── query
    │   │
    │   └── maker
    │       ├── api
    │       ├── facade
    │       └── application
    │           └── query
    │
    ├── ticket
    ├── member
    └── payment
```

여기에 저는 이제 줏대 있게 Capability-first를 선택합니다.

---

### 6. 왜 Capability → Actor인가

Spring Modulith의 철학과도 이쪽이 더 자연스럽습니다.

Spring Modulith의 CQRS 예제는:
- movie
- ticket

같은 business capability를 root module로 두고, Command/Query라는 기술적 역할로 최상단을 나누지 않습니다. Baeldung의 Modulith CQRS 예제도 “command 전용 최상위 package를 만드는 것은 business capability 중심 조직이라는 Modulith 철학과 맞지 않는다”고 명시적으로 설명합니다. ([Baeldung on Kotlin](https://www.baeldung.com/spring-modulith-cqrs))

Google 역시 module은 특정 domain knowledge 범위 안에서 높은 응집도를 가져야 한다고 설명합니다. ([Android Developers](https://developer.android.com/topic/modularization/patterns?utm_source=chatgpt.com))

따라서:

```text
❌
booker
├── booking
├── performance
├── payment
└── ticket
```

보다:

```text
✅
performance
├── booker
└── maker

✅
booking
├── booker
└── maker
```

이 장기적으로 더 안정적입니다.  
Performance 변경을 할 때 관련 코드가 한 capability 아래 모이기 때문입니다.

---

### 7. 단, 이 패키지 트리를 종교처럼 만들지는 않는다

이게 이번 검토에서 한 단계 더 정제된 부분입니다.

예를 들어 어떤 capability가 Maker에만 존재한다면:
```text
settlement
└── maker
```
처럼 한 단계짜리 wrapper를 무조건 만드는 건 의미가 없을 수 있습니다.

마찬가지로 조회만 존재하는 capability에:
```text
command/
query/
```
두 폴더를 억지로 만들지 않습니다.

현재 BEAT의 자체 apis 문서도 상태 변경과 조회가 모두 존재하는 context에서 command/query를 나누고 빈 package를 기계적으로 만들지 않는다는 방향을 이미 갖고 있습니다.

이게 좋은 판단입니다.

즉:  
**Architecture rule은 강하게, directory ceremony는 약하게.**  
입니다.

---

### 8. CQRS는 Gradle module로 안 나눈다

이건 이제 확정입니다.

Baeldung의 기본 CQRS 설명도 첫 단계는 QueryService / CommandService의 책임 분리입니다. ([Baeldung on Kotlin](https://www.baeldung.com/cqrs-for-a-spring-rest-api))

Microsoft의 CQRS 공식 가이드도 read model과 write model의 논리적 분리가 기본이고, 둘이 동일한 data store를 사용해도 CQRS가 성립한다고 설명합니다. 별도의 read DB는 필요할 때 발전시키는 형태입니다. ([Microsoft Learn](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs?utm_source=chatgpt.com))

따라서:

```text
❌ domain-command
❌ domain-query
❌ application-command
❌ application-query
❌ infra-command
❌ infra-query
```

Gradle modules는 만들지 않습니다.

대신:

```text
performance
└── maker
    └── application
        ├── command
        └── query
```

입니다.

---

### 9. Domain도 Command/Query로 나누지 않는다

이것도 매우 중요합니다.

CQRS의 Write Side:
```text
Command Service
      ↓
  Aggregate
      ↓
Domain Repository
```
입니다.

Read Side:
```text
Query Service
      ↓
  Read Port / Query Adapter
      ↓
 Projection
```
입니다.

즉 core:domain은 사실상 business write model을 보호하는 쪽에 더 가깝습니다.  
Query model까지 넣을 이유가 없습니다.

```text
core/domain
└── com.beat.domain
    ├── booking
    ├── performance
    ├── ticket
    ├── schedule
    ├── member
    ├── payment
    └── settlement
```

로 유지합니다.

현재 core:domain은 다른 project module dependency를 갖지 않는 순수 library입니다.  
이건 현재 BEAT에서 가장 잘된 architecture decision 중 하나라 그대로 유지합니다.

---

### 10. core:application은 최종적으로 만들지 않는다

여기도 최종 결정하겠습니다.

처음에는:
```text
core
├── domain
└── application
```
이 Hexagonal/Clean Architecture처럼 예뻐 보여서 고려했습니다.

하지만 BEAT에는 안 만드는 편이 낫습니다.

우아한형제들 멀티모듈 구조에서는 application module이 하위 module들을 조립하여 서비스 비즈니스를 완성하는 실행 경계입니다. ([우아한형제들 기술블로그 배달의민족을 만드는 기술 이야기 |](https://techblog.woowahan.com/2637/))

현재 BEAT도:
- apis
- admin
- batch

각 executable이 자신의 application use case를 소유합니다.

이걸 전부:
`core:application`  
로 옮기면:
Booker Maker Admin Batch의 서비스 비즈니스가 한 중앙 shared library에 모입니다.

그러면 우아한형제들이 경고한 common 문제를 Application이라는 이름으로 다시 만들 가능성이 있습니다. 공통 module은 가능한 얇아야 하고 가능한 사용하지 않는 것을 권장합니다. ([우아한형제들 기술블로그 배달의민족을 만드는 기술 이야기 |](https://techblog.woowahan.com/2637/))

따라서:
- **`apps:api` owns Booker/Maker Application**
- **`apps:admin` owns Admin Application**
- **`apps:batch` owns System Application**

으로 확정합니다.

---

### 11. core:ports도 만들지 않는다

이것도 동일합니다.

```text
core:ports
├── JwtTokenPort
├── MakerPerformanceReadPort
├── SmsPort
├── StoragePort
└── ...
```

는 결국:
“interface니까 여기”
가 될 위험이 큽니다.

따라서 core:ports는 없습니다.

---

### 12. 그렇다면 module-contracts는 왜 남기나?

여기서는 현재 구조가 생각보다 타당합니다.

현재 module-contracts는 implementation을 갖지 않고:
- Spring/JPA/Redis 금지
- Entity/QueryDSL type 금지
- executable 전용 DTO 금지
- contract-local type만 허용

이라는 명확한 규칙을 가지고 있습니다.

그리고 실제로:
```text
apps:api
   ↓
MakerPerformanceListReadPort
   ↑
infrastructure
```
같은 Gradle dependency inversion을 가능하게 합니다.

현재 MakerPerformanceListQueryService는 MakerPerformanceListReadPort에 의존하고, 해당 port가 별도 contracts module에 존재합니다.

Port를 apps:api에 넣으면 infrastructure가 apps:api를 참조해야 해서 현재 composition 구조에서 dependency cycle 문제가 생깁니다.

따라서 별도 contract/API module 자체는 정당합니다.

Google의 모듈화 패턴도 직접 양방향 dependency가 불가능하거나 바람직하지 않을 때 제3의 mediating/API abstraction module을 두는 패턴을 설명합니다. ([Android Developers](https://developer.android.com/topic/modularization/patterns?utm_source=chatgpt.com))

그래서 최종 판단은:
**module-contracts → KEEP**
입니다.

이름도 지금 당장 application-contracts로 바꾸지 않습니다.  
이름을 바꿔서 architecture가 좋아지는 건 아니기 때문입니다.

---

### 13. 단 module-contracts에는 매우 강한 Admission Rule을 둔다

다음 질문 하나면 됩니다.

> “이 계약이 실제 Gradle module boundary를 넘어야 하는가?”

아니면 그 타입의 owner에게 둡니다.

예를 들어:
- `MakerPerformanceListReadPort` : apps:api가 소비, infra가 구현 → contracts 가능.
- `AccessTokenAuthenticator` : security 내부에서 소비, security 내부에서 구현 → contracts에 넣으면 안 됩니다.

현재 gateway README에서도 이 규칙을 이미 적용하고 있습니다. 내부에서 구현과 소비가 끝나는 AccessTokenAuthenticator는 contracts가 아니라 gateway 내부가 소유합니다.

굉장히 좋은 방향입니다.

---

### 14. Infrastructure도 하나로 유지한다

현재:
`core:infra`
에는 JPA와 외부 adapter, Kotlin JDSL 등이 있습니다.

지금부터:
- infra:persistence
- infra:redis
- infra:s3
- infra:sms
- infra:query
- infra:command

로 Gradle module을 만드는 건 권하지 않습니다.

최종 형태는:

```text
infrastructure
└── com.beat.infrastructure
    │
    ├── persistence
    │   ├── booking
    │   │   ├── entity
    │   │   ├── repository
    │   │   └── query
    │   │       ├── booker
    │   │       └── maker
    │   │
    │   └── performance
    │       ├── entity
    │       ├── repository
    │       └── query
    │           ├── booker
    │           ├── maker
    │           └── admin
    │
    ├── redis
    ├── external
    └── config
```

정도면 충분합니다.

여기에서도 Command/Query를 대칭적으로 억지 분리하지 않습니다.  
JPA entity/repository adapter는 write model persistence이고, 복잡한 조회가 필요할 때 별도의 query package를 두는 정도가 좋습니다.

---

### 15. CQRS에서 가장 중요한 실제 규칙

폴더보다 이게 훨씬 중요합니다.

```text
COMMAND
────────────────────────
Application Command
      ↓
Domain Aggregate
      ↓
  Repository
      ↓
  Primary DB
```

그리고:

```text
QUERY
────────────────────────
Application Query
      ↓
  ReadPort
      ↓
 JDSL / SQL
      ↓
Primary / Replica / Redis
      ↓
  ReadModel
```

Microsoft도 read/write 모델을 독립 최적화하고 별도 read store나 replica로 발전시킬 수 있다고 설명합니다. 단, 별도 store에서는 synchronization과 eventual consistency 문제가 발생합니다. ([Microsoft Learn](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs?utm_source=chatgpt.com))

따라서 예약 재고와 같은 correctness 판단을:
- Redis projection
- Read Replica
- Maker dashboard read model

로 하면 안 됩니다.

CQRS의 Query Side와 Command의 invariant read는 다른 개념입니다.

---

### 16. 이벤트도 지금부터 과하게 넣지 않는다

Baeldung의 Modulith CQRS 예제는 write와 read model을 domain event로 동기화합니다. 동시에 eventual consistency라는 비용도 명확히 지적합니다. ([Baeldung on Kotlin](https://www.baeldung.com/spring-modulith-cqrs))

우아한형제들 사례에서도 AFTER_COMMIT으로 외부 이벤트를 보내면 원 transaction과 메시징 시스템을 decouple할 수 있지만, 그 순간 발행 보장이 사라지는 문제가 생겨 별도의 event store 필요성으로 이어졌습니다. ([우아한형제들 기술블로그 배달의민족을 만드는 기술 이야기 |](https://techblog.woowahan.com/7835/))

그러므로 BEAT에서는:
- **CQRS ≠ Event Sourcing**
- **CQRS ≠ Kafka**
- **CQRS ≠ 무조건 Event-driven projection**

입니다.

먼저:
- Write → JPA
- Read → optimized JDSL

로 충분합니다.  
필요하면 Replica → Redis → materialized projection → event/outbox 순으로 진화합니다.

---

### 17. Spring Modulith에 대한 최종 입장도 고정하겠습니다

필수 아닙니다.

이전에는 조금 적극적으로 추천했는데, 최종적으로는:  
**현재 BEAT의 architecture test가 충분하다면 Spring Modulith를 추가하지 않아도 됩니다.**

Spring Modulith 2.1은 application module cycle, internal package 접근, allowed dependency를 검증하는 좋은 도구입니다. ([Home](https://docs.spring.io/spring-modulith/reference/verification.html?utm_source=chatgpt.com))

하지만 BEAT에는 이미 자체 boundary contract test들이 있습니다.

따라서:

```text
ArchUnit/custom boundary tests
             ↓
      부족함이 생긴다
             ↓
Spring Modulith verification 추가
```

정도로 접근합니다.

Spring Modulith에 맞추기 위해 architecture를 바꾸면 순서가 거꾸로입니다.

---

### 18. build-logic은 그대로 간다

이건 거의 만점입니다.

현재:
```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```
을 사용하고 있습니다.

그리고 capability별 convention plugin으로:
- `beat.spring-boot-app`
- `beat.library`
- `beat.web-mvc`
- `beat.web-security`
- `beat.jpa-adapter`
- ...

를 제공합니다.

Gradle 9.6.1의 현재 공식 Best Practices도 included build-logic composite build와 convention plugins를 권장하고, allprojects/subprojects를 통한 cross-project configuration보다 convention plugin을 선호합니다. ([Gradle Documentation](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html?utm_source=chatgpt.com))

반면 jojoldu 글의:
```groovy
subprojects {
    apply plugin ...
}
```
방식은 당시 글의 시대적 맥락에서 보는 게 맞습니다. 실제 글에서도 해당 방식으로 모든 하위 프로젝트에 Spring Boot/Java plugin을 적용합니다. ([기억보단 기록을](https://jojoldu.tistory.com/123))

이 부분은 현대 Gradle 공식 가이드를 우선합니다.

---

### 19. 그러면 최종 Gradle 구조는 이것으로 확정

```text
BEAT-SERVER
│
├── apps
│   ├── api
│   ├── admin
│   └── batch
│
├── core
│   └── domain
│
├── module-contracts
│
├── infrastructure
│
├── support
│   ├── security
│   ├── observability
│   └── web
│
└── build-logic
```

settings.gradle.kts:
```kotlin
include(
    ":apps:api",
    ":apps:admin",
    ":apps:batch",
    ":core:domain",
    ":module-contracts",
    ":infrastructure",
    ":support:security",
    ":support:observability",
    ":support:web",
)
```

9개 product Gradle module입니다.  
현재도 9개입니다.

따라서 결론은:  
**모듈을 줄이는 것이 목표가 아니라, 각 모듈이 왜 존재하는지 명확하게 만드는 것이 목표입니다.**

---

### 20. Dependency는 이것으로 고정

```text
core:domain
    ▲
    │
infrastructure
    ▲            ▲
    │            │
module-contracts │
    ▲            │
    ┌─────────┼─────────┐
    │         │         │
apps:api  apps:admin  apps:batch
```

조금 더 정확히는:

```text
apps:api
├─ core:domain
├─ module-contracts
├─ infrastructure
├─ support:security
├─ support:observability
└─ support:web

apps:admin
├─ core:domain
├─ module-contracts
├─ infrastructure
├─ support:security
├─ support:observability
└─ support:web

apps:batch
├─ core:domain
├─ module-contracts
├─ infrastructure
└─ support:observability

infrastructure
├─ core:domain
└─ module-contracts

support:security
└─ module-contracts

core:domain
└─ 없음

module-contracts
└─ 없음
```

이 dependency direction을 기준선으로 삼겠습니다.

---

### 21. 대안들을 다시 점수 내보면

정확한 scientific score는 아니지만, 동일한 기준으로 비교하면 이렇습니다.

| 후보 | 장점 | 결정적 단점 | BEAT Fit |
|---|---|---|:---:|
| 현재 그대로 | migration 없음 | Actor ownership / generic module naming | 84 |
| core:application + core:ports | Hexagonal 그림이 예쁨 | Application business 중앙집중 | 89 |
| Booker/Maker/Admin Gradle 분리 | Actor isolation 강함 | runtime/build boundary 과도 | 87 |
| Domain별 Gradle module | 강한 compile isolation | 현재 규모 대비 module tax 큼 | 85 |
| Command/Query Gradle 분리 | CQRS 강제력 | business cohesion 파괴 | 82 |
| 최종안 | 현재 강점 유지 + Actor/CQRS 명확화 | package guard 관리 필요 | 96~97 |

그래서 다시 돌려봐도 최종안이 1위입니다.

---

### 22. 그런데 왜 아직 98점이라고 안 하나?

여기서는 계속 같은 입장을 유지하겠습니다.

Architecture tree만 보고 98점을 주는 것은 객관적이지 않습니다.

98점 이상이라고 말하려면 최소한 실제 코드에서 다음 네 가지를 측정해야 합니다.

1. Booker ↔ Maker Application cross-reference가 정말 0인지,
2. module-contracts에 불필요한 계약이 얼마나 있는지,
3. Domain Repository가 Query 요구로 비대해지는지,
4. 그리고 하나의 기능 변경이 평균 몇 Gradle module/package를 동시에 수정하게 만드는지를 실제 dependency graph로 검증해야 합니다.

이것까지 통과하면 98점에 근접했다고 말할 수 있습니다.  
지금 설계 자체는 제 평가로 96~97점짜리 목표 구조입니다.

---

### 23. 그리고 제가 앞으로 이 결론을 바꿀 조건

이것도 명확하게 해두겠습니다.

아래 같은 실제 변화가 생기지 않는 한 기본 구조는 바꾸지 않습니다.

- Maker가 독립 팀/배포/트래픽/장애 격리를 요구 → apps:maker-api 분리 검토
- 특정 infrastructure의 dependency/build cost가 커짐 → infra:persistence / infra:redis 등 분리 검토
- Booking/Performance가 독립 팀과 lifecycle을 가짐 → business Gradle module 승격 검토
- Read Model이 물리적으로 독립 DB/Kafka projection으로 커짐 → Query infrastructure 분리 검토

그전에는 패키지를 Gradle module로 승격하지 않습니다.  
이게 앞으로의 일관된 판단 기준입니다.

---

### 최종 확정

제가 지금 BEAT의 ADR에 한 문장으로 남긴다면 이렇게 씁니다.

> **BEAT는 api/admin/batch를 독립 실행 경계로 유지하고, domain/contracts/infrastructure/security/observability/web만 Gradle compile-time 경계로 둔다. 비즈니스 코드는 Booking/Performance/... Capability를 1차 소유권으로 하고, 동일 Capability 안에서 Booker/Maker/Admin Actor의 Use Case를 분리하며, 상태 변경과 조회가 모두 존재할 때 Application과 Persistence 수준에서 Command/Query를 분리한다. Actor나 CQRS 자체는 독립 Gradle module을 만들 이유로 사용하지 않으며, 실제 독립 배포·팀 ownership·dependency isolation 요구가 발생했을 때만 Gradle boundary로 승격한다.**

이걸 제 최종 추천으로 고정하겠습니다.

그리고 이번 검토에서 오히려 확신이 커진 부분은, 현재 BEAT가 “모듈이 너무 많아서 뜯어고쳐야 하는 레거시”가 아니라는 점입니다. 현재 좋은 domain/build-logic/api-admin-batch 경계는 살리고, Booker/Maker ownership, module-contracts admission rule, global-support/gateway naming·responsibility를 정리하는 것이 가장 ROI 높은 다음 단계입니다. ([우아한형제들 기술블로그 배달의민족을 만드는 기술 이야기 |](https://techblog.woowahan.com/2637/))
