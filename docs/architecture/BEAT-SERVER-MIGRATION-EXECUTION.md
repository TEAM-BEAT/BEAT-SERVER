# BEAT-SERVER Migration Execution Record — develop 재감사본

Baseline: `develop` / `eb007147f6aa3824073b108407ea3ae47748aa40`
Architecture Constitution: `BEAT-SERVER-CQRS-MULTIMODULE-ARCHITECTURE-FINAL.md`
Test Constitution: `BEAT_TEST_ARCHITECTURE_FINAL.md`
Test rewrite plan: `BEAT_TEST_REWRITE_PLAN.md`
Audit date: 2026-08-20
Status: **living execution record**. PR graph와 micro-design은 실행 가설이며 실제 완료 상태는 `task_artifact.md`와 각 PR evidence를 따른다.

이 문서는 깨끗한 `develop` Git object를 별도 디렉터리에 전개해 조사했다. 현재 작업 브랜치의 target-module skeleton, `performance/api`, Booking offer 실험 코드는 evidence에서 제외했다. Architecture Constitution은 전체 boundary의 판단 기준이고 Test Constitution은 그 경계 안의 test-specific 결정이다. Test Rewrite Plan은 실제 source/test inventory와 risk ownership을 소유하며, 이 문서는 그 결과를 PR 단위로 실행하는 가설이다.

## 1. Constitution에서 변경할 수 없는 invariant

1. 최종 physical boundary는 `apps:api/admin/batch`, `application:frontoffice/admin/system`, `domain`, `infrastructure`, `support:security/observability`, `build-logic`이다.
2. `apps`는 inbound adapter와 composition root만 소유한다. HTTP/스케줄러 입력 변환을 넘어선 workflow를 소유하지 않는다.
3. `application`은 use case, application policy, command transaction, consumer-owned output port/query reader를 소유한다. Web/JPA/Redis/JDSL/Feign/S3 구현 타입을 참조하지 않는다.
4. `domain`은 aggregate, value object, business invariant와 필요한 domain service를 소유하며 framework-free다.
5. `infrastructure`는 Domain/Application abstraction의 driven adapter다. 구현 클래스는 기본 `internal`이다. 외부 상태를 숨기지 않는 credential verification 같은 공통 기술 기능은 `support:security`가 좁은 public technical API와 구현을 함께 소유할 수 있다.
6. logical/change ownership의 1차 축은 Business Capability다. Frontoffice Use Case가 Booker/Maker 행위임이 분명하면 현재 한 Actor만 존재해도 Actor package를 명시한다. Member/Auth처럼 특정 Actor의 policy가 아닌 공통 capability만 근거를 남기고 Actor를 생략한다.
7. `Capability → Actor → Command/Query`는 분류 순서다. Actor는 security principal이 아니라 use-case policy/change owner다. 빈 package 생성 규칙이나 class naming 규칙은 아니다.
8. Command correctness는 authoritative state를 사용한다. `Query`라는 이름 자체가 금지가 아니며, primary authoritative read 자체도 금지가 아니다.
9. money, inventory, authorization, ownership, state transition은 cache/replica/presentation projection/eventually-consistent read model로 결정하지 않는다.
10. Cross-capability collaboration 우선순위는 Domain collaboration, consumer-owned narrow port, explicit stable Capability API, 마지막 수단인 다른 use case다. 어느 후보도 이름만으로 채택하지 않는다.
11. Port는 volatility, consumer vocabulary, depth, duplication, deletion test를 통과해야 한다. `module-contracts`를 작은 중앙 계약들로 재생산하지 않는다.
12. Application Service가 다른 Capability의 concrete Application Service를 호출하는 graph는 기본 금지다.
13. Application public surface는 inbound use case, input/output, infrastructure가 구현할 output port, 증명된 stable Capability API로 최소화한다.
14. Command transaction은 Application이 기본 소유한다. migration sequencing은 Domain ownership을 바꾸지 않는다.
15. Kotlin-owned API는 Kotlin nullability/property/default argument를 우선한다. Java caller를 먼저 옮긴 뒤 `Optional`, `@Jvm*`, Java getter/factory를 제거한다.
16. 외부 route/JSON/status/auth, DB/Redis/external contract, scheduler/deploy behavior는 명시적 correctness fix가 아니면 보존한다.
17. Architecture는 Gradle/compiler, Kotlin visibility, ArchUnit/semantic test 순으로 가능한 가장 강한 mechanism으로 집행한다.
18. Test execution contract은 JUnit Platform이고 Kotlin authoring baseline은 Kotest FunSpec이다. Domain/Application unit test는 Spring을 사용하지 않고, real object→simple fake→MockK 순으로 선택하며 MySQL/Redis correctness는 실제 Testcontainers로 검증한다.

근거: Constitution `§3-7`, `§12-19`, `§24-28`, `§33`, `§36-43`, `§46-48`.

## 2. 깨끗한 develop의 실제 module graph와 runtime

### 2.1 Gradle graph

```text
apis ───────────────┬→ module-contracts
                    ├→ gateway
                    ├→ core:domain
                    ├→ core:infra
                    ├→ global-support
                    └→ observability

admin ──────────────┬→ module-contracts
                    ├→ gateway
                    ├→ core:domain
                    ├→ core:infra
                    ├→ global-support
                    └→ observability

batch ──────────────┬→ module-contracts
                    ├→ core:domain
                    ├→ core:infra
                    ├→ global-support
                    └→ observability

core:infra ─────────┬→ module-contracts
                    ├→ core:domain
                    └→ global-support

gateway ────────────┬→ module-contracts   (`api`)
                    ├→ observability      (`api`)
                    └→ global-support

core:domain         └→ no BEAT production project
```

`settings.gradle.kts`에는 `module-contracts`, `apis`, `admin`, `batch`, `gateway`, `core:domain`, `core:infra`, `global-support`, `observability`만 있다. 깨끗한 `develop`에는 `apps:*`, `application:*`, `domain`, `infrastructure`, `support:*` target project가 아직 없다.

### 2.2 CI/CD와 runtime constraint

- CI는 `./gradlew check verifyModuleBootJars --parallel --build-cache`를 실행한다.
- 배포 artifact와 Docker/Ansible module 이름은 `apis`, `admin`, `batch`다.
- deploy path filter도 현재 `apis/**`, `admin/**`, `batch/**`와 legacy shared paths를 기준으로 한다.
- 세 boot application은 독립 실행되며 각 runtime이 infra/gateway/observability configuration을 import한다.
- 따라서 physical rename은 source move만으로 끝나지 않는다. jar 이름, Docker `MODULE`, workflow matrix, path filter, rollback/deploy input compatibility를 같은 PR에서 검증해야 한다.
- Clean snapshot에서 `./gradlew check verifyModuleBootJars --no-daemon --max-workers=1`이 3분 28초, 98 tasks로 성공했다. Infra compile warning 3건은 별도 debt이며 build failure는 아니다.

### 2.3 언어·테스트 debt baseline

- production Java는 Batch의 4개 파일뿐이다: `TicketCleanupService.java`, `TicketCleanupJob.java`, `PromotionMaintenanceService.java`, `PromotionMaintenanceJob.java`.
- Domain repository는 Kotlin-owned API인데 `java.util.Optional`, nullable `Long?`, `@JvmSuppressWildcards`를 사용한다.
- Domain을 중심으로 `@JvmStatic`, `@JvmOverloads`, Java-style getter가 다수 남아 있다. 주요 caller는 Batch Java와 Java test다.
- root와 runtime architecture test에는 exact source token을 검사하는 대형 string-scanning suite가 있다. 보호 invariant를 compiler/Gradle/ArchUnit/semantic test로 대체하기 전에는 삭제하지 않는다.

## 3. Capability별 current source inventory와 semantic ownership

표의 경로는 baseline commit 기준 상대 경로다. `*`는 해당 디렉터리의 모든 production source를 뜻하며, 서로 다른 책임이 섞인 디렉터리는 별도 행으로 분리했다.

### 3.1 Booking

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../booking/api/BookingApi.kt`, `BookingController.kt` | HTTP/OpenAPI endpoint; controller가 facade 호출 | Booker inbound adapter | 없음 | `apps:api/booking` | MOVE. HTTP만 유지 |
| `api/request/{BookingCancelRequest,BookingRefundRequest,GuestBookingRequest,GuestBookingRetrieveRequest,MemberBookingRequest}.kt` | JSON/validation | Booker inbound DTO | 없음 | `apps:api/booking` | MOVE |
| `api/response/{BookingCancelResponse,BookingRefundResponse,BookingSuccessCode,GuestBookingResponse,GuestBookingRetrieveResponse,MemberBookingResponse,MemberBookingRetrieveResponse}.kt`, `api/type/BookingStatusType.kt` | HTTP representation | Booker outbound DTO | 없음 | `apps:api/booking` | MOVE. Domain/Application type 직접 노출 금지 |
| `facade/BookingFacade.kt` | DTO mapping 외에 create→guest session, authenticate→query, actor resolution을 orchestration | 혼합: adapter + Booker workflow | guest identity/session | apps + application | SPLIT. mapping/cookie는 apps, workflow는 application inbound use case |
| `facade/GuestBookingSessionOutcome.kt` | response와 session token을 묶는 HTTP 전달 타입 | adapter | 없음 | `apps:api/booking` | MOVE/KEEP |
| `application/command/{GuestBookingCommandService,MemberBookingCommandService}.kt`, `BookingCommands.kt` | Booking 생성, Schedule inventory, Performance summary 조회, persistence | Booker Command | Booking, Schedule, Performance price | `application:frontoffice/booking/booker/command` | MOVE 후 correctness 수정. 현재 `@ReadModel` money dependency 제거 |
| `application/command/BookingCancellationCommandService.kt` | refund/cancel, Booking/Schedule lock과 allocation release | Booker Command | Booking status/refund, Schedule inventory | same | MOVE. lock semantics 유지/검증 |
| `application/command/{GuestBookingAuthenticationCommandService,GuestBookingSessionCommandService}.kt`, `application/credential/GuestBookingCredentialAuthenticator.kt`, `application/GuestBookingIdentityValidation.kt` | guest credential, throttle, session actor resolution | Booker/anonymous Command | authoritative guest credential/session | `application:frontoffice/booking/booker/command` 및 internal | MOVE. 중복 identity 결정 선행 |
| `application/query/{GuestBookingQueryService,MemberBookingQueryService}.kt` | Booking retrieval; legacy null amount fallback; Performance/Schedule 조합 | Booker Query | Booking snapshot + display projection | `application:frontoffice/booking/booker/query` | MOVE. consumer reader 분리; fallback behavior 결정 필요 |
| `application/BookingPaymentAmount.kt` | price×quantity overflow-safe 계산; command/read error가 혼재 | internal policy | TicketPrice, Booking total | `application:frontoffice/booking/booker` internal | MOVE. 별도 Port 불필요 |
| `application/event/{BookingCreatedEvent,BookingCreatedEventListener}.kt` | post-commit notification | Booker internal event/output | committed Booking snapshot | `application:frontoffice/booking/booker` | MOVE. notification output은 Booking 소유 |
| `application/result/BookingResults.kt` | use-case output | Booker Command/Query output | snapshot/display 혼합 | command/query owner 인접 | SPLIT/MOVE. HTTP type과 분리 |
| `exception/BookingApplicationErrorCode.kt` 및 공용 `ApiApplicationException` 사용 | HTTP-coupled application failure | application failure | 없음 | `application:frontoffice/booking/booker` | REPLACE. HTTP 독립 failure language |
| `core/domain/.../booking/{model/Booking,model/BookingStatus,vo/RefundAccount,repository/BookingRepository,exception/BookingErrorCode}.kt` | Booking aggregate/state/invariant/collection | Domain | Booking | `domain/booking` | MOVE; Kotlin-first는 caller와 함께. `rehydrate` validation 감사 |
| `core/infra/.../booking/{entity/*,mapper/*,repository/BookingJpaRepository,BookingRepositoryImpl}.kt` | JPA persistence/locking | Adapter | primary Booking rows | `infrastructure/persistence/booking` | MOVE; implementation `internal` |
| `core/infra/.../booking/repository/query/{GuestCredentialQueries,MakerTicketQueries}.kt` | credential authoritative lookup와 Maker ticket projection이 한 capability path에 공존 | Booking auth / Ticket Maker Query | Booking rows | 각 consumer adapter package | SPLIT/MOVE. 같은 query implementation owner가 아님 |

확인된 gap:

- Member Booking 생성은 `member.userId`로 저장하지만 response `userId`에는 `member.id`를 넣는다.
- 새 Booking은 `totalPaymentAmount`를 저장한다. legacy null row는 조회 시 현재 Performance 가격으로 재계산한다.
- `Booking.create`는 구매 수량 범위를 검증하지만 `rehydrate`는 동일 검증을 하지 않는다. 운영 row 분포를 모르는 상태에서 검증을 켜면 historical Booking read regression이 생긴다.
- guest candidate 조회는 deterministic order/unique identity를 보장하지 않고 첫 password match를 선택한다. 서로 다른 `userId`가 동시에 일치하면 authorization 결과가 DB 반환 순서에 의존한다.

### 3.2 Performance

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../performance/api/{PerformanceApi,PerformanceController}.kt` | Maker create/modify/edit/list/delete와 Booker detail endpoints | Booker + Maker adapter | 없음 | `apps:api/performance` | MOVE |
| `api/request/{CastModifyRequest,CastRequest,PerformanceImageModifyRequest,PerformanceImageRequest,PerformanceModifyRequest,PerformanceRequest,ScheduleModifyRequest,ScheduleRequest,StaffModifyRequest,StaffRequest}.kt` | Maker HTTP input | Maker adapter | 없음 | `apps:api/performance` | MOVE |
| `api/response/*.kt`, `api/type/{BankNameType,GenreType}.kt` | Booker/Maker HTTP output | mixed adapter | 없음 | `apps:api/performance` | MOVE; actor별 mapper 책임 분리 가능, leaf class invent 금지 |
| `facade/PerformanceFacade.kt` | request→command/result→response mapping, six use-case delegation | mixed adapter | 없음 | `apps:api/performance` | MOVE/MERGE. business workflow는 없으므로 adapter mapper로만 유지 가능 |
| `application/command/{PerformanceCreateCommandService,PerformanceModifyCommandService,PerformanceDeleteCommandService,PerformanceCommands}.kt` | Maker Performance lifecycle, ownership, storage validation, transaction | Maker Command | Performance; Schedule collaboration | `application:frontoffice/performance/maker/command` | MOVE. authoritative repository 유지; HTTP/storage DTO 제거 |
| `application/command/ScheduleSynchronizer.kt` | Performance 수정 안에서 Schedule lock/create/reschedule/delete와 active Booking 검사 | Maker command internal helper | Schedule, Booking active allocation | same command package internal | MOVE. Schedule ownership 이전이 아님 |
| `application/query/PerformanceDetailQueryService.kt` | Booker public detail와 Booking-form detail가 한 service에 혼재 | Booker Query | display projection | `application:frontoffice/performance/booker/query` | SPLIT 여부를 output/change reason으로 결정. 이름만으로 split 금지 |
| `application/query/{MakerPerformanceListQueryService,PerformanceEditFormQueryService}.kt` | Maker list/edit form | Maker Query | consumer projection, auth | `application:frontoffice/performance/maker/query` | MOVE; consumer reader 소유 |
| `application/{PerformanceImageKey,PerformancePresentation}.kt` | storage key validation과 query formatting/due-date | mixed helper | object metadata / display | maker command 및 actor query owner 인접 | SPLIT. 현재 하나의 `application` root에 다른 change reason이 혼재 |
| `application/event/{PerformancePosterChangedEvent,PerformancePosterChangedEventListener}.kt` | committed poster 변경 event와 best-effort CDN prewarm가 apps에 함께 존재 | Maker event + driven adapter | committed poster key | event는 `application:frontoffice/performance/maker`, listener는 `infrastructure/external/cdn` | SPLIT. Application은 기술 cache port를 호출하지 않고 사실을 publish하며 infrastructure listener가 현재 CDN 전략을 수행 |
| `application/result/PerformanceResults.kt` | Booker/Maker command/query output가 한 파일에 혼재 | mixed | authoritative + projection | 각 actor/command/query owner 인접 | SPLIT/MOVE |
| `exception/{CastApplicationErrorCode,PerformanceApplicationErrorCode,PerformanceImageApplicationErrorCode,StaffApplicationErrorCode}.kt` | API-coupled errors | Application failure | 없음 | 해당 actor use case | REPLACE/MOVE |
| `core/domain/.../performance/{model/*,vo/*,repository/PerformanceRepository,exception/PerformanceErrorCode}.kt` | Performance aggregate, price/payment/content/owner | Domain | Performance | `domain/performance` | MOVE. 별도 Offering aggregate는 evidence 없음 |
| `core/infra/.../performance/{entity/*,mapper/*,repository/*}.kt`와 `persistence/{cast,staff,performanceimage}/**` | aggregate persistence; repository load가 casts/staff/images 전부 rehydrate | Adapter | primary Performance rows | `infrastructure/persistence/performance` | MOVE/internal. 전체 aggregate load 비용은 측정 후 최적화 |
| `repository/query/{MakerPerformanceListQueries,PerformanceEditFormQueries,PerformanceContentOwnershipQueries,PerformanceSummaryQueries,PerformancePeriodReadSupport}.kt` | 서로 다른 consumer와 command diagnostic을 한 legacy contract module에 맞춤 | mixed Query/Command support | primary projection | consumer별 infrastructure adapter | SPLIT. Maker list/edit와 child ownership diagnostic은 Performance owner 인접 contract를 구현하고 summary는 wholesale 이동 금지 |

실제 ownership:

- `ticketPrice`, payment account, title, linked user는 Performance에 저장된다.
- price 변경은 active Booking이 있으면 Domain에서 거부한다.
- modify/delete는 Performance를 먼저 lock하고 Schedule을 정렬해 lock한다.
- `PerformanceRepository.lockById`는 현재 casts/staff/images까지 읽는다. 이것은 coupling/비용 evidence지만 그 자체가 새 Application API의 충분조건은 아니다.
- `PerformanceContentOwnershipReadPort`는 modify 요청의 foreign child와 missing child를 구별해 기존 403/404를 보존한다. seam 삭제 시 전체 aggregate scan, persistence leakage 또는 오류 의미 손실이 발생하므로 PR-5에서는 Performance Maker command-owned diagnostic reader로 유지한다. 이것은 invariant input이나 다른 Capability API가 아니다.

### 3.3 Schedule

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../schedule/api/{ScheduleApi,ScheduleController}.kt`, `api/response/{ScheduleSuccessCode,TicketAvailabilityResponse}.kt`, `api/type/ScheduleNumberType.kt` | availability HTTP endpoint | Booker adapter | 없음 | `apps:api/schedule` | MOVE |
| `application/query/ScheduleQueryService.kt`, `application/result/ScheduleResults.kt`, `application/DueDate.kt`, `facade/ScheduleFacade.kt`, `exception/ScheduleApplicationErrorCode.kt` | authoritative availability decision, cross-actor due-date calculation/failure language, HTTP mapping | Booker Query + capability-shared policy + adapter | Schedule | app/application으로 SPLIT | facade/HTTP mapping은 apps; query/results는 `application:frontoffice/schedule/booker/query`; Performance Maker도 사용하는 `DueDate`와 failure language는 `schedule` capability 공통 영역에 유지 |
| `core/domain/.../schedule/{model/Schedule,model/ScheduleNumber,repository/ScheduleRepository,service/ScheduleSequenceDomainService,exception/ScheduleErrorCode}.kt` | occurrence, close time, inventory, numbering | Domain | Schedule | `domain/schedule` | MOVE. Booking/Performance에 흡수 금지 |
| `core/infra/.../schedule/{entity/*,mapper/*,repository/*}.kt` | JPA lock, DB-time close query, persistence | Adapter | primary Schedule rows | `infrastructure/persistence/schedule` | MOVE/internal |
| `repository/query/{ScheduleAvailabilityQueries,ScheduleQueries}.kt` | Booker availability, Home min date, Ticket Maker summary가 혼재 | multiple consumer Query | projections | consumer별 adapter | SPLIT |

`PESSIMISTIC_WRITE`는 현재 strategy다. 영구 invariant는 oversell 방지, DB close-time 검증, competing mutation의 serialization/conflict detection이다.

이름이 `ScheduleAvailabilityReadPort`인 legacy contract의 실제 consumer는 `PerformanceDetailQueryService` 하나다. 따라서 상세 화면용 schedule projection은 `application:frontoffice/performance/booker/query`가 vocabulary를 소유한다. 반면 `/schedule/ticket-availability`는 `ScheduleQueryService`가 authoritative `ScheduleRepository`로 close/inventory를 판단하는 독립 Schedule Booker query이며 projection reader로 대체하지 않는다.

### 3.4 Ticket

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../ticket/api/{TicketApi,TicketController}.kt`, `api/request/*.kt`, `api/response/*.kt` | Maker ticket-management HTTP | Maker adapter | 없음 | `apps:api/ticket` | MOVE |
| `facade/TicketFacade.kt` | DTO mapping and delegation | Maker adapter | 없음 | `apps:api/ticket` | MOVE/MERGE |
| `application/command/{TicketCommandService,TicketCommands}.kt` | payment confirmation/refund completion/delete over Booking/Schedule | Maker Command | Booking status, Schedule allocation, Performance owner | `application:frontoffice/ticket/maker/command` | MOVE. Performance `@ReadModel` authorization 제거 |
| `application/query/{TicketQueryService,TicketListQuery}.kt`, `result/TicketRetrieveResult.kt` | Maker ticket list/search | Maker Query | consumer projection; separate auth | `application:frontoffice/ticket/maker/query` | MOVE; reader vocabulary를 consumer가 소유 |
| `application/event/{TicketPaymentConfirmedEvent,TicketPaymentConfirmedEventListener}.kt` | post-commit SMS | Maker event/output | committed Booking state | `application:frontoffice/ticket/maker` | MOVE. generic `SmsPort` 대신 consumer semantic 검토 |
| `exception/TicketApplicationErrorCode.kt` | API-coupled failure | Application | 없음 | ticket command/query owner | REPLACE/MOVE |

현재 Domain에는 Ticket aggregate/repository가 없다. Ticket은 Maker가 인식하는 application capability이지만 state owner는 Booking과 Schedule이다. 따라서 `ticket` package를 유지하는 것과 Ticket Domain aggregate를 발명하는 것은 별개의 결정이다.

### 3.5 Member / Auth / User identity

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../member/api/{MemberApi,MemberController}.kt`, `api/request/*`, `api/response/*`, `api/type/SocialTypeRequest.kt` | signup/refresh/signout HTTP, cookie/header mapping | anonymous/Booker adapter | 없음 | `apps:api/member` | MOVE |
| `facade/MemberFacade.kt`와 `MemberLoginSession` | DTO mapping, refresh-token cookie material 전달 | adapter | 없음 | `apps:api/member` | MOVE/MERGE; token business policy는 application |
| `application/command/{SocialLoginCommandService,SocialLoginCommand,SocialLoginMemberResolver,MemberRegistrar}.kt` | social provider login, member/user registration race resolution | anonymous Command | Member, Users | `application:frontoffice/member/command` | MOVE; provider port consumer-owned |
| `application/command/{AuthenticationCommandService,LoginTokenIssuer}.kt` | access/refresh issue, refresh validation, signout | Auth Command | token/session | `application:frontoffice/auth/command` | MOVE. mixed JWT contract split 검토 |
| `application/event/{MemberRegisteredEvent,MemberRegisteredEventListener}.kt`, `result/*.kt`, `exception/*.kt` | notification, outputs, failures | Member/Auth | Member/User snapshot | actor command owner 인접 | MOVE/REPLACE |
| `core/domain/.../member/**` | social identity/profile linked to userId | Domain | Member | `domain/member` | MOVE/Kotlin-first |
| `core/domain/.../user/**` | principal role and user identity | Domain | Users | `domain/user` | MOVE/Kotlin-first |
| `core/infra/.../member/**`, `persistence/user/**` | JPA adapters | Adapter | primary Member/User | `infrastructure/persistence/{member,user}` | MOVE/internal |
| `core/infra/external/social/kakao/**` | external social provider | Adapter | provider response | `infrastructure/external/social` | MOVE/internal |
| `core/infra/redis/auth/**` | refresh/guest session/throttle | Adapter | Redis technical state | `infrastructure/redis/auth` | MOVE/internal |
| `gateway/**` | JWT, password hash, filter, principal, resolver, security config | technical support + inbound wiring | token/credential | `support:security` + apps config | SPLIT/MOVE. password API/BCrypt 구현은 support가 함께 소유하고 business authorization은 금지 |

Member와 Users는 현재 서로 다른 aggregate다. Member는 social identity/profile, Users는 role/principal identity를 소유한다. `member`와 `auth` package split은 이 evidence를 반영하며 별도 Actor package는 만들지 않는다.

### 3.6 Home

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../home/api/{HomeApi,HomeController}.kt`, `api/response/*.kt`, `api/type/HomeGenreType.kt`, `facade/HomeFacade.kt` | Home HTTP and mapping | Booker adapter | 없음 | `apps:api/home` | MOVE/MERGE |
| `application/query/HomeQueryService.kt`, `application/result/HomeResults.kt` | Performance/Schedule/Promotion consumer projection 조합 | Booker Query | 없음; read projection | `application:frontoffice/home/booker/query` | MOVE. 하나의 Home-owned reader shape로 fold 가능 |

Home은 aggregate가 아니라 consumer query capability다. cross-capability join/projection을 허용하되 Command seam으로 재사용하지 않는다.

### 3.7 File / Storage

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../file/api/{FileApi,FileController}.kt`, `api/response/*`, `facade/FileFacade.kt` | `/file/presigned-url` HTTP surface | Performance Maker adapter | 없음 | `apps:api/file` | MOVE. route 보존; 앱 package 이름이 Domain owner를 의미하지 않음 |
| `application/command/FileCommandService.kt` | Performance maker image names validate + presigned upload request | Performance Maker Command | external object contract | `application:frontoffice/performance/maker/command` | MOVE; class rename은 caller/result 이동 후 책임이 실제로 더 선명해질 때만 수행. 현재 `File`은 business capability evidence가 없음 |
| `module-contracts/storage/**`, `core/infra/external/storage/s3/**` | Performance와 Admin Promotion storage methods/DTO를 한 interface에 결합 | multiple consumers | S3 object metadata | Performance Maker-owned storage output + temporary Admin legacy contract implemented by one infra adapter | SPLIT. PR-5에서 Performance presign/metadata vocabulary만 이동하고 Admin methods/DTO는 PR-15까지 compatibility로 남긴다 |

Performance Maker storage seam은 presigned upload 발급과 저장 object metadata 확인이라는 동일 외부 object-store volatility를 숨긴다. 삭제하면 Application이 S3 구현을 알거나 broad Admin contract를 계속 사용해야 하므로 port creation/deletion test를 통과한다. 반면 poster CDN pre-warm은 별도 thin port를 만들지 않는다. committed `PerformancePosterChangedEvent`를 infrastructure listener가 구독하고 현재 CDN adapter를 호출한다. 따라서 PR-5 후 Performance Application은 `FileStoragePort`와 `ImageCachePort`를 모두 알지 않는다.

### 3.8 Promotion

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `admin/.../promotion/api/{AdminPromotionApi,AdminPromotionController}.kt`, `api/request/*`, `api/response/*`, `facade/AdminPromotionFacade.kt` | Admin HTTP and mapping | Admin adapter | 없음 | `apps:admin/promotion` | MOVE/MERGE |
| `application/command/{AdminPromotionCommandService,CarouselHandleCommand}.kt` | carousel mutation, member/existence/storage validation, cache prewarm | Admin Command | Promotion; Performance existence | `application:admin/promotion/command` | MOVE. Performance `@ReadModel` existence 제거 |
| `application/query/AdminPromotionQueryService.kt`, assembler/result files | promotion list and presigned URL queries/results | Admin Query/Command output | Promotion + storage | `application:admin/promotion/{query,command}` | SPLIT/MOVE by consumer semantics |
| `exception/PromotionApplicationErrorCode.kt` | HTTP-coupled failure | Application | 없음 | application admin promotion | REPLACE/MOVE |
| `core/domain/.../promotion/{model/*,repository/*,service/*,exception/*}.kt` | carousel assignment and eligibility | Domain | Promotion | `domain/promotion` | MOVE |
| `core/infra/.../promotion/**` | JPA adapter and Home projection | Adapter | Promotion rows | persistence + Home query adapter | SPLIT/MOVE/internal |
| `batch/.../promotion/{application/PromotionMaintenanceService.java,job/PromotionMaintenanceJob.java,facade/PromotionMaintenanceFacade.kt}` | scheduled eligibility cleanup/reorder | System Command + scheduler adapter | Promotion, Schedule dates | `application:system/promotion/command` + `apps:batch/promotion` | SPLIT/MOVE; Kotlin 전환과 Clock 주입 |

### 3.9 User / Admin user query

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `admin/.../user/api/{AdminUserApi,AdminUserController}.kt`, `api/response/*`, `facade/AdminUserFacade.kt` | Admin user list HTTP/mapping | Admin adapter | 없음 | `apps:admin/user` | MOVE/MERGE |
| `application/query/AdminUserQueryService.kt`, `application/result/AdminUserResults.kt` | caller Member validation + all Users query | Admin Query | Users, Member | `application:admin/user/query` | MOVE. business authorization와 mere existence check 재검증 |
| `exception/UserApplicationErrorCode.kt`, `apis/.../user/exception/UserApplicationErrorCode.kt` | runtime별 duplicated HTTP-coupled failure | mixed | 없음 | application failure + app mapping | MERGE/REPLACE |

Users aggregate/JPA files는 3.5에 함께 기록했다. `application:admin` lane 자체가 Admin actor이므로 `user/admin/query`를 만들지 않는다.

### 3.10 Batch / System and runtime-common files

| Current file(s) | Current responsibility | Target | Action / reason |
|---|---|---|---|
| `batch/BatchApplication.kt`, `config/{InfraConfig,SchedulingConfig,ScheduledTaskErrorHandler}.kt`, Java `*Job.java` | bootstrap, scheduler adapter, error handling | `apps:batch` | MOVE/Kotlin. scheduler만 apps에 유지 |
| `batch/config/DomainServiceConfig.kt` | manual Domain service bean assembly | composition/config | REVIEW. Domain service 생성 방식과 public surface 최소화 후 위치 결정 |
| `TicketCleanupService.java`, `PromotionMaintenanceService.java` | business maintenance workflow | `application:system/{booking,promotion}/command` | MOVE + Kotlin. `LocalDateTime.now/LocalDate.now`는 `Clock` 사용 |
| `TicketCleanupFacade.kt`, `PromotionMaintenanceFacade.kt` | one-line pass-through | apps scheduler adapter | INLINE/DELETE after job Kotlin migration |
| `apis/admin` boot/config/swagger/exception common files | bootstrap, Web/security/HTTP error | `apps:api`, `apps:admin` | MOVE. Domain exception direct mapping 제거 |
| `global-support/**` | HTTP response/Jackson CDN serializer + generic image-key helper가 혼재 | apps/support/consumer owner | SPLIT; `global-support` retirement. 새 giant shared module 금지 |
| `observability/**` | logging/MDC/metrics/tracing/Sentry | `support:observability` | MOVE; Domain dependency 금지 |

### 3.11 아직 닫지 않은 modeling/correctness question

1. **Payment destination snapshot:** 기존 Booking은 Performance payment account를 저장하지 않는다. 기존 unpaid Booking이 account correction을 따라야 하는지 product decision이 필요하다.
2. **Legacy amount:** `totalPaymentAmount == null` row의 historical price를 tracked schema만으로 복원할 수 없다. 현재 가격 fallback은 compatibility behavior로 유지한다. authoritative backfill source가 확인되기 전 이를 historical snapshot이라고 부르거나 임의 보정하지 않는다.
3. **Rehydration range:** persisted purchase count가 `1..10` 밖인지 확인하기 전에는 strict rehydrate validation을 켜지 않는다. 배포 전 DB owner는 아래 read-only audit을 실행하고, historical policy로 허용할지 repair/reject할지를 Booking domain owner와 결정해야 한다. 현재 source behavior는 보존한다.
   ```sql
   SELECT purchase_ticket_count, COUNT(*)
   FROM booking
   WHERE purchase_ticket_count NOT BETWEEN 1 AND 10
   GROUP BY purchase_ticket_count
   ORDER BY purchase_ticket_count;
   ```
4. **Guest identity:** name/phone/birth/password가 일치한 candidate의 distinct `userId`가 정확히 하나일 때만 인증한다. 같은 `userId`의 여러 Booking row는 허용하지만, 서로 다른 `userId`가 동시에 일치하면 정렬로 임의 선택하지 않고 기존 인증 실패 결과로 fail-closed한다.
5. **Price scope:** 현재는 Performance-wide price다. per-Schedule price/Offering lifecycle evidence가 없다.
6. **Performance schedule count:** `Performance.totalScheduleCount`와 실제 Schedule rows가 중복된다. authoritative metadata인지 derived projection인지 data drift audit가 필요하다.
7. **Promotion referential race:** `promotion.performance_id`는 JPA scalar이며 code-level relation/lock이 없다. Admin의 existence check와 concurrent Performance deletion 사이 orphan 가능성을 Testcontainers로 먼저 검증한다.
8. **Payment capability:** 별도 Payment aggregate는 없다. payable amount와 payment-confirmed status는 Booking, payment destination은 Performance, refund account/status는 Booking이 소유한다. 독립 결제 lifecycle이 생기기 전 `domain.payment`를 만들지 않는다.

## 4. Capability → Actor → Command/Query 실제 mapping

| Lane | Capability | Actual actor | Command | Query | Actor package decision |
|---|---|---|---|---|---|
| Frontoffice | Booking | Booker/anonymous guest | create, authenticate/session, refund/cancel | member/guest retrieval | `booking/booker`; guest는 별도 Actor가 아니라 Booker use case의 인증 상태다 |
| Frontoffice | Performance | Booker | 없음 | public detail/booking form | `performance/booker/query` 필요 |
| Frontoffice | Performance | Maker | create/modify/delete, Schedule orchestration, upload preparation | list/edit form | `performance/maker/{command,query}` 필요 |
| Frontoffice | Schedule | Booker | 없음 | availability | `schedule/booker/query` |
| Frontoffice | Ticket | Maker | payment/refund/delete workflow | list/search | `ticket/maker/{command,query}` |
| Frontoffice | Member | anonymous/Booker | social login/registration | 없음 | Actor package 불필요 |
| Frontoffice | Auth | anonymous/Booker | token refresh/signout/issuance | 없음 | Actor package 불필요 |
| Frontoffice | Home | Booker | 없음 | home projection | `home/booker/query` |
| Admin | Promotion | lane 자체가 Admin | carousel/storage command | promotion/storage query | `promotion/{command,query}`; `admin` 중복 금지 |
| Admin | User | lane 자체가 Admin | 없음 | user list | `user/query` |
| System | Booking | lane 자체가 System | old-cancelled cleanup | 없음 | `booking/command`; `system` 중복 금지 |
| System | Promotion | lane 자체가 System | eligibility cleanup/reorder | 없음 | `promotion/command` |

Domain ownership과 Application capability는 일대일일 필요가 없다. Ticket과 Home은 Application capability지만 현재 Ticket/Home aggregate는 없다. Schedule lifecycle을 Performance Maker use case가 orchestration해도 Schedule aggregate ownership은 유지된다. Payment도 현재 독립 aggregate가 아니라 Performance와 Booking에 분산된 state 이름이다.

## 5. Booking / Performance / Schedule collaboration 재결정

### 5.1 Field semantics

| Value | Classification | Authoritative owner/source | Snapshot/consistency |
|---|---|---|---|
| Performance ticket price | money command input | Performance aggregate/table | Booking 생성과 price 수정이 serialize되어야 함 |
| Booking payable amount | Booking state | Booking `totalPaymentAmount` | 새 Booking 생성 시 snapshot |
| Payment destination | payment instruction/display | Performance | 현재 Booking은 snapshot하지 않음; product decision open |
| Performance title | event/display snapshot | Performance | Booking correctness input 아님. transaction 안에서 notification payload capture 가능 |
| Schedule performance link | ownership/reference | Schedule | lock 후 재검증 |
| booking close time | state-transition input | Schedule + DB clock | Schedule lock 아래 authoritative DB time 검증 |
| inventory | money-adjacent state transition | Schedule | reserve/release serialization |
| Performance owner | authorization | Performance | primary authoritative state. Query projection 금지 |
| Booking status/purchaser | state/authorization | Booking | mutation 시 Booking lock |

현재 `PerformanceSummaryQueries`는 primary JPA/JDSL DB를 읽어 즉시 stale한 production bug가 입증된 것은 아니다. 그러나 price, owner, display fields를 하나의 `@ReadModel`로 섞어 미래 projection optimization이 Command correctness를 약화시킬 수 있다.

### 5.2 Candidate comparison

| Candidate | Evidence for | Evidence against | Decision |
|---|---|---|---|
| A. `PerformanceRepository` direct Domain collaboration | authoritative aggregate collection, 새 seam 없음, Constitution 우선순위 1. `Performance`가 이미 price/account/title/owner invariant를 소유 | Booking에 full repository surface가 보이고 casts/staff/images까지 rehydrate. read-only 사용을 compiler로 제한하기 어려움 | **Reference migration의 최소 초기 선택.** `lockById` read만 사용하고 semantic/concurrency guard를 둔다. 부하 측정 없이 새 API를 만들지 않는다 |
| B. Booking-owned narrow Output Port | Booking vocabulary와 test isolation 가능 | 현재 구현은 Performance table 직접 조회 또는 repository forwarding뿐. owner bypass/thin forwarding이고 real volatility 없음 | Reject now |
| C. Explicit Performance Capability API | repository surface/locking/error translation을 provider가 감추고 future representation change를 격리 가능 | 현재 provider application policy가 repository authoritative read 외에 입증되지 않음. 한 consumer를 위해 public Application exception과 service graph seam을 만들며 deletion test가 아직 약함 | Reject as baseline. 실제 두 번째 consumer/remote boundary/provider policy가 생기면 재검토 |
| D. Offering/Price aggregate 또는 Schedule price로 remodel | 향후 회차별 가격/독립 offering lifecycle 지원 가능 | schema, endpoint, test, business behavior evidence 없음 | Reject; speculative modeling |
| E. Performance repository에 consumer-specific scalar projection 추가 | full aggregate load를 피할 수 있음 | query shape를 Domain repository에 넣고 price/title/account의 우연한 묶음을 영구화. 성능 문제 미측정 | Reject now; 측정 후 재검토 |

### 5.3 선택의 정확한 의미

Reference slice에서는 새로운 `performance/api`, `collaboration`, `BookingTermsProvider`를 만들지 않는다. Booking Application이 Performance Domain repository에서 authoritative aggregate를 읽는 Domain collaboration을 사용한다.

이 결정은 “다른 Capability가 Performance를 마음대로 수정해도 된다”는 뜻이 아니다.

- Booking package는 Performance repository의 authoritative read/lock만 사용한다.
- `save`, `delete`, price/account mutation은 Performance Maker owner에 남는다.
- Booking이 필요로 하는 값은 Domain value/property를 통해 읽고, JPA entity/projection은 보지 않는다.
- direct repository usage가 두 곳 이상으로 확산되거나 provider policy가 추가되면 C를 다시 deletion/volatility test한다.

`PerformanceRepository`가 casts/staff/images를 모두 rehydrate하는 비용은 기록된 risk다. 먼저 query count/latency를 측정하고, 악화가 확인될 때 aggregate load strategy 또는 repository contract를 좁힌다. 구현 편의나 예상 성능을 근거로 public Capability API를 선도입하지 않는다.

### 5.4 Transaction / lock rationale

현재 Performance modify/delete와 Booking create는 모두 `Performance → sorted Schedule` 순서를 사용한다. Booking은 먼저 Schedule의 Performance 식별자를 조회한 뒤 authoritative Performance row를 잠그고, 그 다음 Schedule row를 잠근다. 이 순서를 뒤집는 변경은 반대 lock order와 deadlock 가능성을 만들므로 금지한다.

따라서 command flow를 다음 결과가 보장되도록 재구성한다.

```text
authoritative scalar Schedule.performanceId read (no aggregate rehydration)
→ Performance authoritative lock/read
→ Schedule lock
→ Schedule.performanceId 재검증
→ DB-time booking-close 검증
→ Schedule inventory reserve
→ locked Performance price로 Booking amount snapshot
→ Booking + Schedule atomic save
```

사전 조회는 lock order를 결정하기 위한 Schedule-owned identity lookup일 뿐이며, 최종 판단 값이 아니다. 여기서 `Schedule` aggregate를 먼저 rehydrate하면 동일 transaction의 JPA 1차 캐시에 stale inventory가 남아, 나중의 pessimistic lock 조회가 갱신된 재고 대신 cached entity를 반환할 수 있다. 실제 concurrency test에서 5건만 성공해야 할 요청 30건이 모두 성공하는 것을 재현했다. 따라서 `ScheduleRepository.findPerformanceIdById(Long): Long?`는 primary DB scalar만 조회하고, authoritative inventory/membership는 항상 이후 `lockById` 결과와 `belongsTo` 재검증으로 확정한다.

PR11은 또 다른 `REPEATABLE READ` gap을 확인했다. Performance 수정 transaction이 owner 조회 후 Performance lock에서 대기하면 기존 snapshot이 먼저 만들어질 수 있으므로, Schedule lock 뒤의 active Booking 판단을 일반 `COUNT` projection으로 수행해서는 안 된다. 현재 구현은 해당 Schedule들의 Booking row를 locking read로 조회한다. 이미 잡은 Schedule lock이 새 Booking insert를 막고 locking read가 최신 committed state를 보므로, 가격 변경 판단은 stale snapshot에 의존하지 않는다.

현재 pessimistic implementation의 global row order:

```text
Performance → sorted Schedule → sorted Booking
```

영구 Architecture contract는 lock 자체가 아니라 다음 observable semantics다.

1. price 변경과 Booking 생성 중 하나가 먼저 serialize되거나 version conflict로 실패한다.
2. Booking이 먼저면 active Booking 때문에 서로 다른 price로의 변경이 거부된다.
3. Performance 변경이 먼저면 Booking은 새 authoritative price를 snapshot한다.
4. Schedule oversell과 close-time race가 없다.
5. Booking/Schedule 저장은 하나의 transaction이다.

Payment account/title은 amount invariant와 분리한다. 현재 behavior를 보존하되, 기존 Booking이 이후 account/title 변경을 따라야 하는지는 product/data decision으로 남긴다.

## 6. 새 abstraction과 `module-contracts` disposition

### 6.1 새 seam이 필요한 곳

- external social provider, refresh token store, guest session/throttle
- notification/SMS delivery
- S3 upload/object metadata
- consumer-specific Home/Performance/Ticket/Schedule query readers
- application이 JPA를 보지 않게 하는 aggregate repository implementation

이들은 external technology, persistence/query strategy, delivery failure semantics이라는 실제 volatility를 숨긴다.

Password hashing은 위 output-port 목록과 다르다. 외부 상태나 다른 Business Capability를 숨기지 않고 BCrypt/legacy verification/hash-upgrade라는 shared security primitive를 제공하므로 `support:security`가 좁은 public technical API와 internal 구현을 함께 소유한다. Application은 이 API를 사용하고, support가 Application contract를 구현하지 않는다.

### 6.2 새 seam이 필요하지 않은 곳

- Booking의 price multiplication helper
- Performance repository를 한 번 forwarding하는 `BookingTermsProvider`
- cross-capability class가 public이라는 이유만으로 만든 `performance/api` package
- facade 한 method마다 만드는 inbound interface
- `ClockPort`, `TransactionRunnerPort`, `UuidPort`
- HTTP route가 `/file`이라는 이유로 만든 독립 File domain/application capability

### 6.3 Contract-by-contract disposition

| Current contract(s) | Consumer / implementer / volatility | Final disposition |
|---|---|---|
| `ReadModel` | marker만 제공 | INLINE-OR-DELETE. semantic CQRS guard로 대체 |
| `PerformanceSummaryReadPort`, `PerformanceSummaryReadModel` | Booking/Home/Ticket/Admin이 서로 다른 fields 사용; JDSL primary projection | REQUIRES-CORRECTNESS-FIX. Command money/owner/existence는 authoritative Domain collaboration으로 교체; Home/Ticket 등은 consumer reader로 분해 |
| `MakerPerformanceListReadPort`, `MakerPerformanceListItemReadModel` | Performance Maker query / JDSL | MOVE-TO-QUERY-READER: `application:frontoffice/performance/maker/query` |
| `PerformanceEditFormReadPort`, `PerformanceEditFormReadModel` | Performance Maker edit query / JDSL | MOVE-TO-QUERY-READER: same owner |
| `PerformanceContentOwnershipReadPort` | Performance modify의 foreign child와 missing child 403/404 diagnostic / JDSL | MOVE-TO-APPLICATION-READER: `application:frontoffice/performance/maker/command`. 삭제하면 persistence leakage, aggregate scan 또는 오류 의미 손실이 생기며 invariant input으로 재사용 금지 |
| `ScheduleAvailabilityReadPort`, `ScheduleAvailabilityReadModel` | `PerformanceDetailQueryService`만 소비하는 detail projection / JDBC | MOVE-TO-QUERY-READER: `application:frontoffice/performance/booker/query`. Schedule availability endpoint는 별도 authoritative repository query 유지 |
| `ScheduleReadPort`, `MinPerformanceDateReadModel`, `ScheduleSummaryReadModel` | Home min date + Ticket Maker summary를 한 interface에 결합 | SPLIT/MOVE-TO-QUERY-READER: Home과 Ticket consumer 각각 소유 |
| `MakerTicketReadPort`, `MakerTicketListItemReadModel`, `MakerTicketBookingStatus`, `MakerTicketScheduleNumber` | Ticket Maker list/search / JDSL | MOVE-TO-QUERY-READER: `application:frontoffice/ticket/maker/query` |
| `HomePromotionReadPort`, `HomePromotionReadModel` | Home / JDSL | MOVE-TO-QUERY-READER: Home projection에 fold |
| `GuestCredentialReadPort`, `GuestCredentialReadModel` | Booking guest authentication / Booking rows | MOVE-TO-APPLICATION-PORT: authoritative credential lookup. Query projection으로 최적화 금지; duplicate policy 포함 |
| `GuestAccessThrottlePort` | Booking guest auth / Redis | MOVE-TO-APPLICATION-PORT: Booking command |
| `GuestSessionPort` | Booking actor session / Redis | MOVE-TO-APPLICATION-PORT: Booking command; `OptionalLong`→nullable after Java caller migration |
| `GuestPasswordHashPort` | Booking이 사용하지만 구현은 state 없는 BCrypt/legacy verification/hash-upgrade technical primitive | REPLACE-WITH-SUPPORT-TECHNICAL-API. `support:security/password`가 public hashing contract와 internal BCrypt 구현을 함께 소유; `support:security → application` dependency 금지 |
| `JwtTokenPort`, `JwtSubject`, `JwtTokenType`, `TokenValidationResult` | Member/Auth issuance와 inbound gateway parsing을 한 interface에 결합 | SPLIT. Application Auth가 필요한 issue/refresh semantics와 `support:security` inbound access-token parsing을 각 owner로 이동. 이름은 caller migration 때 결정 |
| `RefreshTokenPort` | Auth command / Redis | MOVE-TO-APPLICATION-PORT: Auth; nullable Kotlin API |
| `SocialLoginPort`, `SocialLoginRequest`, `SocialLoginType`, `SocialMemberInfo`, `SocialLoginFailure` | Member social login / Kakao | MOVE-TO-APPLICATION-PORT: Member/Auth consumer vocabulary. Domain `SocialType` duplication 제거 시 mapper 유지 |
| `BookingNotificationPort`, `BookingNotification` | Booking created listener / Slack | MOVE-TO-APPLICATION-PORT: Booking event/output |
| `MemberNotificationPort`, `MemberNotification` | Member registered listener / Slack | MOVE-TO-APPLICATION-PORT: Member event/output |
| `SmsPort`, `SmsMessage` | Ticket payment-confirmed listener만 사용 / CoolSMS | REPLACE consumer semantic output. generic SMS transport vocabulary를 Application public API로 유지하지 않음 |
| `ImageCachePort` | Performance poster와 Admin Promotion prewarm / CDN adapter | PR-5에서 Performance 사용 제거: committed poster event를 infrastructure가 구독. Admin legacy contract는 PR-15까지 유지한 뒤 central contract 삭제 |
| `FileStoragePort` | Performance Maker + Admin Promotion + metadata를 한 interface로 결합 / S3 | REQUIRES-SPLIT. PR-5에서 Performance presign/metadata를 Maker-owned object-storage output으로 이동; S3 adapter는 새 port와 temporary Admin legacy contract를 함께 구현 |
| `PerformancePresignedUrls`, `ImagePresignedUpload` | Performance Maker consumer | 해당 Performance Maker output port/input-output에 MOVE |
| `CarouselPresignedUrls`, `CarouselPresignedUpload`, `BannerPresignedUrl` | Admin Promotion consumer | `application:admin/promotion`에 MOVE |
| `ImageObjectMetadata` | Performance/Admin command validation | 각 consumer vocabulary로 fold하거나 implementation-neutral value를 각 port와 함께 소유; 중앙 shared DTO 금지 |

최종에는 `module-contracts` project와 package가 모두 사라진다. retirement는 마지막 relocation PR 한 번이 아니라 consumer slice마다 수행하고, 마지막 PR은 빈 module/reference 제거만 담당한다.

## 7. 기존 Execution Record의 잘못된 해석

1. **Dirty worktree를 develop 상태로 오인:** target alias/skeleton과 application project를 이미 존재하는 것처럼 기록했다. 깨끗한 develop에는 없다.
2. **`Performance Capability API`를 package/class 설계로 즉시 변환:** Constitution의 public 후보를 `application:frontoffice/performance/api` 생성 의무로 해석했다. 해당 package 결정은 철회한다.
3. **API와 lock을 결합:** authoritative offer semantic과 `PESSIMISTIC_WRITE` strategy를 같은 contract로 만들려 했다. lock은 교체 가능한 consistency implementation이다.
4. **Lock inversion 미검출:** Performance modify의 `Performance→Schedule`과 기존 Booking create의 `Schedule→Performance` 가능성을 충분히 비교하지 않았다.
5. **Actor package 과소 지정:** 단일 Actor라는 이유로 Booking/Schedule/Ticket/Home의 Actor를 생략하려 했다. `frontoffice` lane은 Actor를 표현하지 않으므로 실제 행위가 Booker/Maker로 분명한 use case는 Actor package를 명시한다.
6. **Schedule ownership 표현 혼동:** reference slice sequencing을 “Booking enabling seam”으로 표현해 Schedule의 독립 aggregate ownership을 흐렸다.
7. **Contract relocation 중심 분류:** mixed summary/storage/JWT contracts를 consumer semantics보다 새 package 위치로 먼저 분류했다.
8. **Kotlin-first 후순위화:** 별도 말기 PR로 대부분 미뤘다. 실제로는 Java caller를 각 slice에서 옮긴 직후 compatibility surface를 제거해야 한다.
9. **Guard 후순위화:** invented `booking→performance.api` allowlist를 먼저 상정하고 compiler-level macro graph와 apps→domain policy를 충분히 앞세우지 않았다.
10. **PR 완료 상태 오기:** 작업 브랜치 실험 결과를 baseline PR 완료처럼 기록했다. 재감사 시점에는 모든 migration PR을 미착수로 재설정했으며, 이후 절의 완료 표시는 각 PR 경계에서 직접 검증한 evidence만 반영한다.

## 8. Revised target package tree

다음은 directory/package 수준만 표현한다. leaf class/interface 이름은 각 PR의 source inventory와 tests를 다시 확인한 뒤 결정한다.

```text
apps
├── api
│   └── com.beat.apps.api
│       ├── booking
│       ├── performance
│       ├── schedule
│       ├── ticket
│       ├── member
│       ├── home
│       ├── file                 # external route ownership; Domain capability 선언 아님
│       ├── config
│       └── bootstrap
├── admin
│   └── com.beat.apps.admin
│       ├── promotion
│       ├── user
│       ├── config
│       └── bootstrap
└── batch
    └── com.beat.apps.batch
        ├── booking
        ├── promotion
        ├── config
        └── bootstrap

application
├── frontoffice
│   └── com.beat.application.frontoffice
│       ├── booking
│       │   └── booker
│       │       ├── command
│       │       └── query
│       ├── performance
│       │   ├── booker
│       │   │   └── query
│       │   └── maker
│       │       ├── command
│       │       └── query
│       ├── schedule
│       │   └── booker
│       │       └── query
│       ├── ticket
│       │   └── maker
│       │       ├── command
│       │       └── query
│       ├── member
│       │   └── command
│       ├── auth
│       │   └── command
│       └── home
│           └── booker
│               └── query
├── admin
│   └── com.beat.application.admin
│       ├── promotion
│       │   ├── command
│       │   └── query
│       └── user
│           └── query
└── system
    └── com.beat.application.system
        ├── booking
        │   └── command
        └── promotion
            └── command

domain
└── com.beat.domain
    ├── booking
    ├── performance
    ├── schedule
    ├── member
    ├── user
    ├── promotion
    └── sharedkernel

infrastructure
└── com.beat.infrastructure
    ├── persistence
    ├── redis
    ├── external
    └── config

support
├── security
│   └── password
└── observability
```

의도적으로 없는 package:

- `application.frontoffice.performance.api`: public API가 필요하다는 증거도 없고, public surface가 생겨도 `api` package가 필수는 아니다.
- `application.frontoffice.file`: 현재 use case change reason은 Performance Maker다.
- `application.admin.*.admin` / `application.system.*.system`: physical lane과 Actor가 중복된다.
- `domain.ticket`, `domain.home`: 현재 aggregate/invariant evidence가 없다.

Actor 표기 근거:

- `booking`, `schedule`, `home`은 현재 Booker 행위만 있지만 `frontoffice`가 Actor를 대신 표현하지 않으므로 `booker`를 명시한다.
- `ticket`의 endpoint vocabulary는 ticket이지만 실제 Use Case는 공연 Maker의 예매자 관리이므로 `maker`를 명시한다.
- `member`, `auth`는 Booker/Maker 공통 identity/session capability이며 actor-specific policy가 없어 Actor-neutral로 둔다. 단순히 actor가 하나라서 생략한 것이 아니다.
- Admin/System은 physical application lane이 Actor를 이미 표현하므로 하위에 동일 actor package를 반복하지 않는다.

## 9. Revised PR graph

### 9.1 Revision reason

PR-7 이후 graph를 다시 나눈 이유는 다음과 같다.

- 단일 Actor 생략 가설이 Frontoffice Actor ownership과 충돌했다.
- runtime/BOM upgrade, test authoring foundation, test semantic rewrite, Testcontainers lifecycle 변경은 rollback 위험이 서로 다르다.
- 이미 이동한 Booking/Performance/Schedule/Ticket/Member/Auth의 package를 먼저 정렬해야 테스트를 두 번 이동하지 않는다.
- Spring Boot 4.1 채택은 Constitution의 조건부 선택인데 기존 PR-9가 이를 확정안처럼 닫았다. 현재 Spring Cloud `2025.1.x` 공식 지원선은 Boot `4.0.x`이고 최신 `2025.1.3` BOM도 Boot `4.0.8`을 가리키므로, PR-9는 지원되는 patch line 정렬로 교정한다.
- PR-9 이후 전체 test inventory와 production-risk ownership을 재감사한 결과 lock characterization, adapter fidelity, Web/API/security gate, capability migration, legacy deletion은 서로 다른 rollback surface를 가진다. 따라서 기존 PR-10~19 가설을 폐기하고 PR-10~22로 재분해했다.
- PR-21 이후 final audit에서 API/Admin이 Domain exception과 Domain service configuration을 직접 소유하는 실제 위반이 확인됐다. Error-language 경계는 Kotlin/test cleanup과 behavior/rollback 위험이 다르므로 PR-23으로 분리하고, 최종 gate/report는 PR-24로 이동한다.

### 9.2 Current dependency graph

```text
PR-1 + PR-2 + PR-3 → PR-4 → PR-5 → PR-6 → PR-7
PR-7 → PR-8 → PR-9 → PR-10
PR-10 ─┬→ PR-11
       └→ PR-12
PR-11 + PR-12 → PR-13 → PR-14 → PR-15
PR-15 ─┬→ PR-16
       ├→ PR-17 → PR-18
       └→ PR-19
PR-16 + PR-18 + PR-19 → PR-20 → PR-21 → PR-22 → PR-23 → PR-24
```

PR-11/PR-12는 source overlap이 없을 때 병렬 진행할 수 있고, PR-16/17/19도 각 capability source가 독립적일 때 병렬 진행할 수 있다. PR-20은 source-conflicting capability move가 합쳐진 뒤 merge한다. 신규 또는 재작성 Kotlin test는 PR-10 이후 FunSpec으로 작성한다. 번호는 Architecture가 아니며 correctness dependency나 source overlap이 달라지면 graph와 DoD를 먼저 수정한다.

### PR-1 — Correctness characterization: Booking identity, snapshots, rehydration, guest ambiguity

- Objective: memberId/userId mismatch, new/legacy amount semantics, purchase-count rehydrate, duplicate/concurrent guest credential behavior를 regression test와 data audit로 확정한다.
- Invariant gained: 구조 이동 전에 intended/actual behavior와 허용 가능한 data state가 명시된다.
- Dependencies: none.
- Correctness risk: legacy invalid rows를 audit 없이 reject하거나 amount fallback을 바꾸면 production regression 가능. 반대로 ambiguous guest를 임의 선택하면 authorization 위반이다.
- Compatibility: route/schema/JSON과 legacy null amount/current payment destination 표시를 유지한다. member response identity는 수정하고 multi-user guest match는 기존 인증 실패 representation으로 fail-closed한다. strict rehydrate validation은 data gate 전 보류한다.
- Rollback: tests/작은 field correction 단위 revert.
- Tests: Booking unit/application/API, stored/null amount fixtures, same-user/multi-user guest credential cases, guest concurrency integration.
- DoD: 네 위험 각각 decision+coverage; legacy amount/payment destination/rehydration처럼 source만으로 닫을 수 없는 결정은 behavior 유지, owner, executable data audit를 명시한다.

### PR-2 — Booking/Performance/Schedule lock and authoritative-state characterization

- Objective: MySQL에서 price-modify/create 양방향 interleaving, close-time, sold-out, cancellation/Ticket lock order를 재현한다.
- Invariant gained: global outcome와 row order가 코드 이동 전에 executable evidence가 된다.
- Dependencies: none; PR-1과 병렬 가능하나 PR-4 전에 둘 다 merge.
- Correctness risk: flaky concurrency test, DB vendor lock semantics, lock 전 aggregate read의 JPA 1차 캐시 stale state.
- Compatibility: characterization 우선; lock-order correction은 동일 PR의 좁은 change로 분리.
- Rollback: test fixture와 lock change를 분리 revert.
- Tests: Testcontainers MySQL, timeout/deadlock assertion, stored amount snapshot, preliminary identity lookup을 포함한 Guest Booking concurrency.
- DoD: no deadlock, either old-price-booking blocks price change or new price is snapshotted; Schedule oversell 없음; lock 전 entity rehydration으로 stale inventory를 복원하지 않음.

### PR-3 — Target Gradle skeleton, build logic, runtime compatibility, macro guards

- Objective: target projects/convention plugins를 만들고 legacy source는 아직 옮기지 않은 채 executable graph와 jar/deploy compatibility를 마련한다.
- Invariant gained: domain/application/infrastructure/apps dependency direction이 compiler/Gradle로 집행된다.
- Dependencies: none; PR-1/2와 병렬 가능.
- Correctness risk: jar archive name, bootJar staging, component scan/config import, deploy path filter.
- Compatibility: `apis/admin/batch` external deployment module names를 transition 동안 유지.
- Rollback: new empty projects/conventions와 workflow mapping revert; legacy runtime untouched.
- Tests: `projects`, dependency graph assertions, `check`, `verifyModuleBootJars`, three boot context tests, staged jar/Docker smoke.
- DoD: target graph exists, apps independently executable, no source move, CI/deploy mapping green.

### PR-4 — Booking reference slice, authoritative Performance Domain collaboration, guest password security API

- Objective: Booking adapter/use case/domain/infra ownership을 분리하고 `PerformanceSummaryReadPort`의 Booking Command 사용을 authoritative Domain collaboration으로 교체하며 guest password hashing을 `support:security` technical API로 이동한다.
- Invariant gained: apps workflow 제거, application transaction ownership, command authoritative money/inventory.
- Dependencies: PR-1, PR-2, PR-3.
- Correctness risk: lock order, preliminary Schedule identity lookup/JPA 1차 캐시, guest session best-effort flow, response identity, legacy amount.
- Compatibility: existing endpoints/cookies/JSON/DB/Redis 유지. temporary bridge는 한 PR 안에서 제거.
- Rollback: Booking slice 단위; schema change 없음.
- Tests: existing Booking tests + PR-1/2 concurrency, scalar identity→Performance lock→Schedule lock order, BCrypt/legacy match/upgrade security tests, HTTP/security/serialization, infra Booking adapter.
- DoD: no Booking workflow in apps; no Booking Command→ReadModel; no `performance/api`; Schedule preliminary read does not rehydrate/cache inventory aggregate; `GuestPasswordHashPort` retired; Application→support password API only, support→Application edge zero; touched Java callers Kotlin migration 후 unnecessary `@Jvm*` 제거.

### PR-5 — Performance/Schedule Frontoffice ownership and Maker storage

- Objective: 실제 caller 기준으로 Performance Booker/Maker application, 독립 Schedule Booker query, Performance Maker upload preparation을 target boundary로 이동한다. 새 Capability API는 만들지 않는다.
- Invariant gained: Capability→Actor→CQRS change locality, Performance→Schedule orchestration ownership, Application의 Web/central-contract 의존 제거.
- Dependencies: PR-4 (shared summary/schedule contract conflict 회피).
- Correctness risk: modify/delete의 Performance→sorted Schedule lock, active Booking price restriction, foreign-vs-missing child 403/404, DB-clock availability, image metadata/category validation, edit/detail JSON.
- Compatibility: Performance/Schedule/File routes, status/message/JSON, transaction boundary, S3 key/presigned URL semantics, after-commit best-effort CDN prewarm 유지. Admin storage/cache legacy contract는 PR-15까지 temporary compatibility로 남긴다.
- Rollback: capability slice revert; no schema change.
- Tests: Performance/Schedule domain/application, modification/delete lock and active-booking behavior, child diagnostic, JPA/JDSL/JDBC readers, File/Performance/Schedule API contract, S3 adapter, context bootstrap, capability/implementation-access guards.
- DoD: 실제 use case가 있는 `performance/booker/query`, `performance/maker/{command,query}`, `schedule/booker/query`만 존재; no `performance/api`; Schedule ownership 독립; Performance application에서 `ApiApplicationException`, apps error, Web, infrastructure, `module-contracts` import zero; `PerformanceEditFormReadPort`의 `Optional` 제거; Performance 용 broad storage/cache methods retired; touched Java caller를 Kotlin으로 옮긴 뒤 불필요한 `@Jvm*` 제거; apps facade/controller는 application public use case/output 외 infrastructure implementation에 접근하지 않음.

### PR-6 — Ticket Maker capability

- Objective: Ticket Maker command/query/event를 이동하고 Performance owner authorization을 authoritative state로 교체한다.
- Invariant gained: Ticket application capability와 Booking/Schedule authoritative ownership이 분리된다.
- Dependencies: PR-5.
- Correctness risk: bulk lock ordering, allocation release, SMS after-commit semantics.
- Compatibility: ticket routes/filters/status/SMS behavior 유지.
- Rollback: Ticket slice revert.
- Tests: Ticket command/query, sorted multi-row concurrency, API, SMS adapter failure.
- DoD: no Ticket Command→Performance read projection; Ticket reader consumer-owned; Domain Ticket aggregate를 발명하지 않음.

### PR-7 — Member/Auth and remaining support:security

- Objective: PR-4에서 먼저 이동한 password technical API를 유지하면서 Member/Auth use cases와 JWT/social/refresh ports, filter/principal security plumbing을 분리한다.
- Invariant gained: business authentication policy는 Application, JWT/password/filter/principal은 support:security, Redis/social은 Infrastructure이며 support는 Application에 의존하지 않는다.
- Dependencies: PR-3; Booking guest-security overlap 때문에 PR-4 후 merge 권장.
- Correctness risk: token claims/expiry, cookie/signout, duplicate social signup race, filter ordering.
- Compatibility: token/cookie/header/auth behavior와 Redis keys 유지.
- Rollback: 기존 gateway bootstrap compatibility를 transition adapter로 한 PR 유지 후 제거.
- Tests: token unit/security integration, social mapping, refresh Redis, three runtime auth smoke.
- DoD: mixed `JwtTokenPort` retired; no central auth contract; Java callers migrated and nullable/property API verified.

### PR-8 — Frontoffice Actor ownership alignment

- Objective: 이미 이동한 Booking 전체, Schedule Booker query, Ticket 전체 source와 tests를 각각 `booking/booker`, `schedule/booker/query`, `ticket/maker` 아래로 정렬하고 Actor 규칙을 executable guard로 고정한다. Performance Maker도 사용하는 Schedule 공통 policy/failure는 capability root에 유지한다.
- Invariant gained: Frontoffice의 모든 actor-specific use case가 Capability→Actor→CQRS 순으로 동일하게 탐색된다.
- Dependencies: PR-7; PR-4/5/6 source가 모두 도착한 뒤 한 번만 이동한다.
- Correctness risk: Spring component scan, package-private/internal test access, import 누락.
- Compatibility: class behavior와 public use-case signature를 바꾸지 않는다. 기존 JUnit tests는 이 PR에서 그대로 이동한다.
- Rollback: package move commit 단위 revert; schema/runtime contract 변화 없음.
- Tests: affected compile/test, three app boot, actor/package ArchUnit.
- DoD: Booking/Schedule/Ticket의 production/test package 정렬; Performance의 기존 Booker/Maker 보존; Member/Auth actor-neutral allowlist; Admin/System actor 중복 금지.

### PR-9 — Supported runtime/BOM patch alignment

- Objective: 공식 호환 조합인 Spring Boot `4.0.8`/Spring Cloud `2025.1.3`과 Kotlin `2.3.21`/Testcontainers `2.0.5`를 검증하고, 새 Boot BOM보다 낮은 Tomcat/Jackson/Netty override를 제거한다.
- Invariant gained: test/tool version compatibility가 application migration과 분리된 한 rollback boundary에 놓인다.
- Dependencies: PR-8.
- Correctness risk: plugin/API binary compatibility, Testcontainers 2.x package/API, Spring bootstrap/security behavior.
- Compatibility: route/schema/runtime behavior 유지; version upgrade 외 source migration 금지.
- Rollback: dependency/build-logic commit revert.
- Tests: full current suite, dependency resolution, three boot contexts, MySQL/Redis container smoke, bootJar/deploy smoke.
- DoD: adopted versions and remaining security overrides documented; Spring Cloud compatibility verifier와 Boot BOM 기준 dependency graph green; Boot 4.1 보류 근거 기록.

### PR-10 — Test platform and coexistence pilot

- Objective: production behavior를 바꾸지 않고 JUnit Platform 위에 Kotest FunSpec authoring과 risk-based test tasks를 추가한다.
- Invariant gained: execution contract와 Kotlin authoring style이 분리되고 JUnit/Kotest coexistence가 executable evidence로 검증된다.
- Dependencies: PR-9.
- Correctness risk: engine/tag discovery 누락, IDE/Gradle 불일치, global Spring extension side effect.
- Compatibility: 기존 JUnit/Mockito test를 유지하고 대표 Domain/Application/Spring assertion을 dual-run한다.
- Rollback: catalog alias, convention/task, pilot spec만 제거하면 legacy suite가 그대로 남는다.
- Tests: `test`, `fastTest`, `integrationTest`, `correctnessTest`, `acceptanceTest`; engine별 discovery count와 tag selection 비교.
- DoD: `useJUnitPlatform()` 유지; pure/Spring FunSpec discovery; SingleInstance와 Spring leaf lifecycle policy가 명시적으로 실행됨; Fixture Monkey 미도입.

### PR-11 — Critical Booking/Performance/Schedule/Ticket characterization

- Objective: broad test rewrite 전에 Booking overselling, Performance 가격 변경, Schedule close/inventory, Ticket multi-row lock의 production transaction/lock semantics를 확정한다.
- Invariant gained: money/inventory/lock correctness가 실제 MySQL과 production transaction topology로 보호된다.
- Dependencies: PR-10.
- Correctness risk: 테스트가 기존 production defect 또는 정의되지 않은 snapshot 결과를 드러낼 수 있다.
- Compatibility: 기존 concurrency/integration tests는 deterministic replacement가 동일 risk를 증명할 때까지 유지한다.
- Rollback: 새 spec/harness를 독립 revert하며 기존 tests를 보존한다.
- Tests: API correctness task, Frontoffice focused test, barrier+bounded timeout 반복 실행, 최종 authoritative DB state 검증.
- DoD: approved old/new price snapshot 결과, no overselling, DB close 재확인, sorted Ticket lock, canonical lock order가 deterministic하다. defect 발견 시 assertion을 약화하지 않고 별도 correctness fix로 분리한다.

### PR-12 — Domain fast-risk rewrite

- Objective: Domain invariant를 capability-owned Kotlin FunSpec의 단일 fast owner로 옮기고 Java/중복 Domain test를 제거한다.
- Invariant gained: aggregate/value-object/state-transition correctness가 Spring과 persistence 없이 검증된다.
- Dependencies: PR-10; PR-11과 source overlap이 없으면 병렬 가능.
- Correctness risk: syntax translation 과정에서 legacy edge case 또는 equality/privacy rule 누락.
- Compatibility: assertion inventory를 비교하고 replacement가 통과한 파일만 제거한다.
- Rollback: production source 변화가 없으므로 capability/file 단위 revert.
- Tests: `:domain:test`, bounded property generators와 seed report; Spring compile classpath 부재 확인.
- DoD: Domain risk matrix의 각 invariant에 단일 owner가 있고 deliberate mutant가 검출된다. Domain test에서 Spring/MockK를 사용하지 않는다.

### PR-13 — Real MySQL and Redis adapter slices

- Objective: mocked repository/Redis와 broad context/manual container를 실제 MySQL/Redis 기반 capability slice로 교체한다.
- Invariant gained: JPA/JDSL/JDBC query, constraint, locking, Redis TTL/alias/Lua atomicity가 production infra에서 검증된다.
- Dependencies: PR-11, PR-12.
- Correctness risk: slice import가 custom repository implementation을 누락하거나 cached context가 종료된 container를 참조할 수 있다.
- Compatibility: 마지막 consumer가 이동할 때까지 legacy integration base를 유지한다.
- Rollback: 새 slice/config를 독립 제거하고 legacy base로 복귀한다.
- Tests: `:infrastructure:integrationTest`, `:apps:api:integrationTest`, context-cache/container count와 non-empty query 결과.
- DoD: persistence/Redis risk마다 real-infra owner가 있고 Spring-cached context와 container lifecycle이 정렬된다. H2와 mocked Redis semantics를 correctness 근거로 사용하지 않는다.

### PR-14 — Frontoffice application risk-owner rewrite

- Objective: Booking/Performance/Schedule/Ticket/Member/Auth Application tests를 Spring 없이 actor/use-case risk별 FunSpec으로 재작성한다.
- Invariant gained: Application orchestration, authorization, output/event intent가 Domain 및 persistence risk와 중복 없이 검증된다.
- Dependencies: PR-13.
- Correctness risk: fake repository가 query/lock semantics를 거짓으로 표현하거나 service별 test 생성으로 퇴행할 수 있다.
- Compatibility: protected risk mapping을 유지하고 capability별 replacement 후 기존 test를 제거한다.
- Rollback: capability별 test commit revert.
- Tests: `:application:frontoffice:test`; real object→simple fake→boundary MockK; application framework-leakage guard.
- DoD: 각 test가 Actor와 production risk를 명시하며 Domain/DB assertion을 중복하지 않는다. authoritative command semantics를 fake가 임의로 축약하지 않는다.

### PR-15 — Web, authorization, OpenAPI, and acceptance foundation

- Objective: HTTP/security/API compatibility owner와 소수 critical acceptance lane을 확립한다.
- Invariant gained: route/JSON/status/error/security backward compatibility가 Web slice, authorization matrix, OpenAPI diff, real-infra acceptance로 분리된다.
- Dependencies: PR-14.
- Correctness risk: `@WebMvcTest` import 폭증, TestContext cache fragmentation, spec/tag 누락.
- Compatibility: DTO/handler/facade fragment는 Web/OpenAPI/acceptance replacement가 통과한 뒤에만 제거한다.
- Rollback: meta-annotation/config, baseline, capability Web spec을 별도 commit으로 되돌린다.
- Tests: API/Admin fast tests, API acceptance, normalized OpenAPI breaking diff, context cache key report.
- DoD: 실제 endpoint 전체 authorization matrix, reviewed general/admin OpenAPI baseline, critical journeys의 real MySQL/Redis 검증. Fixture Monkey는 admission evidence가 있을 때만 도입한다.
- Implementation evidence: API의 실제 24 operations와 Admin의 실제 5 operations를 handler mapping에서 추출한 exact authorization matrix가 anonymous/member/admin filter boundary를 검증한다. API/Admin acceptance는 공통 meta-annotation과 Spring-managed Testcontainers configuration을 사용하며, API는 MySQL/Redis, Admin은 실제 MySQL과 stable storage fake로 현재 composition을 부팅한다. File query alias와 Ticket API→Application filter mapping은 focused Web slice가 소유하고, JSON alias/enum/validation 및 error envelope는 OpenAPI가 표현하지 못하는 compatibility spec으로 남겼다.
- OpenAPI evidence: `openApiTest`가 현재 Springdoc general/admin JSON을 각 module build directory에 생성한다. 검토된 baseline은 general 24 operations/22 paths, admin 5 operations/4 paths이며 authorization matrix와 정확히 일치한다. CI는 oasdiff `v1.28.0`의 플랫폼별 고정 asset/checksum을 검증한 뒤 두 문서에 `breaking --fail-on ERR`를 적용한다. 현재 두 비교 모두 `No changes detected`다.
- Verification evidence: focused JSON/Web specs, API/Admin authorization/boot acceptance, `:apps:api:check`, `:apps:admin:check`, version-catalog alias 검사, `git diff --check`, OpenAPI compatibility script, api/admin/batch `verifyModuleBootJars`가 통과했다. 삭제된 Admin boot source-string leaf의 risk는 실제 context boot가 대체한다.

### PR-16 — Home Booker projection migration with tests

- Objective: Home을 `home/booker/query` consumer-owned projection으로 이동하고 Application/MySQL/Web risk owner를 함께 만든다.
- Invariant gained: Home consumer가 projection vocabulary와 optimization을 소유하고 Performance/Schedule Domain knowledge를 복제하지 않는다.
- Dependencies: PR-15와 Performance/Schedule foundations.
- Correctness risk: ordering, due-date, genre/filter, carousel composition과 JSON drift.
- Compatibility: Home route, JSON, ordering 유지.
- Rollback: Home capability slice revert.
- Tests: Home Booker application query, real projection query, Web/API diff, API boot.
- DoD: Home model은 Query 전용이며 Command에 노출되지 않고 actor/package guard가 통과한다.
- Implementation evidence: API adapter가 소유하던 Home workflow/result를 `application:frontoffice/home/booker/query`로 이동했다. 기존 `PerformanceSummaryReadPort.findAll/findByGenre`, `ScheduleReadPort`, `HomePromotionReadPort`의 세 consumer 조각은 Home vocabulary를 소유하는 하나의 `HomeProjectionReader`로 fold했다. Infrastructure의 `HomeProjectionQueries`가 현재 세-query composition을 내부 구현하고 Apps는 facade/HTTP/JSON mapping만 유지한다. 새로운 Home aggregate, `api` package, central contract module은 만들지 않았다.
- Correctness evidence: real MySQL에서 `@Enumerated(EnumType.STRING)` carousel ordering이 `ONE, THREE, TWO`가 되는 결함을 확인했다. SQL 문자열 정렬을 제거하고 typed `CarouselNumber.number` 순서로 adapter에서 정렬해 Domain carousel 의미를 보존한다. Application은 `Clock`과 genre를 reader에 전달하고 period/due-date/sort policy를 소유하며, projection fidelity와 min-Schedule semantics는 MySQL spec이 소유한다.
- Retirement/compatibility: API-owned Home service/result, `HomePromotionReadPort`/read model, mixed `ScheduleReadPort`/minimum-date read model, old Home Promotion/Schedule query adapters를 replacement green 이후 제거했다. `/api/main` route와 response JSON/OpenAPI는 변경하지 않았다.
- Verification evidence: focused Home Application/MySQL/Web specs, root `test`, `:application:frontoffice:check`, `:infrastructure:check`, `:apps:api:check`, api/admin/batch bootJar verification, both OpenAPI breaking diffs, and `git diff --check` pass.

### PR-17 — Admin User migration with tests

- Objective: Admin User workflow를 `application:admin`으로 이동하고 Admin actor/security lane을 검증한다.
- Invariant gained: Admin lane 자체가 Actor를 표현하고 apps:admin은 HTTP mapping/bootstrap만 소유한다.
- Dependencies: PR-15.
- Correctness risk: admin role과 member-existence policy, facade compatibility.
- Compatibility: `/users` behavior와 JSON을 유지하며 마지막 commit까지 temporary facade adapter를 둘 수 있다.
- Rollback: 작은 User slice와 adapter를 함께 revert.
- Tests: `:application:admin:test`, Admin User Web/security, OpenAPI diff, Admin boot.
- DoD: facade workflow 제거; `ROLE_ADMIN` matrix 집행; 중복 `user/admin` actor package와 Domain failure 직접 HTTP mapping 없음.
- Implementation evidence: `AdminUserQueryService`, `AdminUserResults`, `UserApplicationErrorCode`, Admin failure wrapper를 `application:admin/user/query`와 application-owned failure language로 이동했다. Admin lane 자체가 actor이므로 중복 `admin` actor package를 만들지 않았고, 현재 `Users` aggregate/repository가 id/role의 authoritative collection이어서 새 Port나 `api` package를 만들지 않았다.
- Composition evidence: `apps:admin`은 공개 bootstrap type인 `AdminApplicationConfig`만 import하고 Application 내부 구현 package를 scan하지 않는다. Admin Web/facade는 moved query boundary만 호출하며 Infrastructure 구현을 직접 알지 않는다.
- Correctness/compatibility evidence: existing caller-Member existence validation과 persisted `Users.id` response semantics를 보존했다. `ROLE_ADMIN`은 기존 exact authorization matrix가 소유하며 Application query는 role을 재판단하지 않는다. route/JSON/OpenAPI breaking change는 없다.
- Replacement/verification evidence: Spring-free `AdminUserQueryApplicationSpec`과 focused `AdminUserControllerWebSpec`이 workflow와 delivery risk를 각각 소유한다. old service test와 delegation-only facade test는 replacement green 후 제거했다. `:application:admin:check`, `:apps:admin:check`, Admin OpenAPI generation, `transitionBoundaryTest`, `verifyTargetModuleGraph`, `verifyModuleBootJars`, pinned OpenAPI breaking diff, `git diff --check`가 통과한다.

### PR-18 — Admin Promotion migration and referential concurrency

- Objective: Admin Promotion command/query를 이동하고 Performance reference 및 Carousel persistence concurrency를 실제 MySQL로 검증한다.
- Invariant gained: Promotion risk가 Domain/Application/MySQL/Web 및 실제 storage/cache adapter owner로 분리된다.
- Dependencies: PR-17와 Performance foundation.
- Correctness risk: carousel uniqueness/order, concurrent Performance delete, redirect, S3/cache failure.
- Compatibility: Admin routes/JSON/storage keys를 유지하고 migration commit 끝까지 compatibility adapter를 보존할 수 있다.
- Rollback: Promotion capability slice revert.
- Tests: Promotion Domain/Application, MySQL referential concurrency, JPA/JDSL/S3/cache adapter, Admin Web/security/API diff.
- DoD: existence-check race 결과가 확정되고 facade/JPA-shape tests는 semantic replacement 후 제거된다. production defect이면 별도 correctness commit으로 분리한다.
- Implementation evidence: Admin adapter가 소유하던 Promotion command/query/result/failure language를 `application:admin/promotion/{command,query}`로 이동했다. Admin lane 자체가 actor를 표현하므로 중복 `admin` package와 speculative `api` package를 만들지 않았다. Application은 HTTP/JPA/S3/Redis/JDSL 타입에 의존하지 않는다.
- Collaboration evidence: command가 사용하던 mixed `PerformanceSummaryReadPort` projection을 제거했다. Promotion은 이미 Domain에서 `Performance.Id` reference를 소유하고 command에 필요한 것은 authoritative reference validity뿐이다. 따라서 distinct Performance ids를 정렬해 기존 `PerformanceRepository.lockById`로 확인한다. Domain redesign은 invariant가 바뀌지 않아 과도하고, consumer-owned existence Port는 repository를 thin-forward하며, explicit Performance Capability API는 provider policy/remote boundary/다중 consumer evidence가 없어 탈락했다.
- Concurrency evidence: real MySQL은 Promotion row가 존재할 때 `PESSIMISTIC_WRITE`가 일괄 변경을 직렬화하지만 empty table에서는 두 `SELECT ... FOR UPDATE`가 동시에 통과함을 재현했다. 새 schema/JVM-local lock 대신 Infrastructure repository implementation이 transaction-scoped business namespace를 MySQL advisory lock으로 직렬화하고 현재 row lock을 함께 유지한다. lock 전략은 Domain/Application contract가 아니며 다른 cross-instance 전략으로 교체 가능하다. Promotion 생성과 Performance 삭제도 같은 authoritative Performance row lock을 사용해 orphan 없이 직렬화된다.
- Contract/compatibility evidence: `PromotionImageStorage`는 presigned upload와 object existence라는 S3 volatility를 Promotion vocabulary로 숨기고, `PromotionImageCache`는 외부 CDN pre-warm 실패/latency boundary를 숨긴다. Infrastructure S3/CDN adapter가 구현하며 Admin route, JSON, storage key, synchronous pre-warm behavior는 유지된다. central storage/CDN contracts와 Performance summary read contract/query는 마지막 consumer가 사라진 뒤 삭제했다. `application:admin → global-support`의 `ImageKeyExtractor`만 PR-20까지 temporary compatibility로 남는다.
- Replacement/verification evidence: Spring-free command/query specs, focused Admin Web spec, S3 adapter spec, 세 가지 real-MySQL locking outcome이 각각 policy/delivery/external/persistence risk를 소유한다. old service/facade/JPA-shape tests는 replacement green 후 제거했다. `:application:admin:check`, `:infrastructure:check`, `:apps:admin:check`, Admin OpenAPI generation, `transitionBoundaryTest`, `verifyTargetModuleGraph`, `verifyModuleBootJars`, pinned OpenAPI breaking diff, `git diff --check`가 통과한다.

### PR-19 — System/Batch workflow migration with tests

- Objective: cleanup/maintenance workflow를 `application:system` Kotlin use case로 옮기고 batch를 scheduler/bootstrap adapter로 제한한다.
- Invariant gained: apps:batch가 business workflow를 소유하지 않고 System lane이 transaction/time policy를 소유한다.
- Dependencies: PR-12/13; maintenance가 Promotion state를 건드리면 PR-18.
- Correctness risk: clock/timezone, cron/error handling, large delete/reorder transaction, Java compatibility.
- Compatibility: scheduler names/cron/job/deploy behavior 유지; final migration commit까지 scheduler delegation adapter 허용.
- Rollback: job/use-case별 commit revert.
- Tests: `:application:system:test`, focused scheduler context, repository integration, Batch boot/actuator/job invocation.
- DoD: delegation facade/job의 business assertion 제거; Batch 때문에만 남은 Java production/test compatibility 없음; 중복 system actor package 없음.
- Implementation evidence: ticket cleanup은 System Booking command, carousel maintenance는 System Promotion command로 `application:system`에 이동했다. physical lane이 actor를 이미 표현하므로 중복 `system` package나 새 API/Port를 만들지 않았다. Batch job은 해당 use case만 호출하며 one-line facade와 Batch-owned Java service/job은 제거됐다.
- Consistency evidence: 두 use case는 고정 가능한 `Clock`과 Application transaction을 소유한다. Promotion maintenance는 발견된 Performance id와 각 Schedule id를 정렬해 authoritative lock을 획득한 뒤 Promotion namespace/row lock을 잡는다. 잠금 snapshot 이후 추가된 reference는 unlocked state로 판정하지 않고 다음 실행으로 미룬다.
- Compatibility/verification evidence: cron, scheduler qualifier, owner flag, actuator/bootstrap behavior는 Kotlin job에서도 동일하다. Spring-free System specs, Spring-managed Batch context, real MySQL cutoff/deletion/reorder integration, Batch check, target module graph, 세 bootJar가 통과했다. Batch production은 Java `NO-SOURCE`이며 unrelated untracked contention probe는 변경하지 않았다.

### PR-20 — Observability and global-support rewrite

- Objective: observability와 shared technical support의 focused test를 이동하고 `global-support` retirement ownership을 확정한다.
- Invariant gained: MDC/coroutine/async/Sentry/token/password는 좁은 technical owner를 갖고 HTTP/Jackson policy는 apps가 소유한다.
- Dependencies: PR-12 이후 시작 가능하나 source-conflicting PR-16/18/19 뒤 merge.
- Correctness risk: log field, trace ID, resource/config, serialization/error-envelope drift.
- Compatibility: 기존 resource/bootstrap tests를 physical move가 끝날 때까지 유지한다.
- Rollback: concern/file 단위 revert.
- Tests: support-security/observability focused specs, serialization/API contract, three app boot smokes.
- DoD: focused technical risks가 유지되고 source-string architecture scan은 더 강한 guard로 대체된다. observability→Domain dependency 없음.
- Implemented: API/Admin response envelope은 각 inbound adapter가 소유하고 API CDN annotation/serializer는 API Web/Jackson policy로 이동했다. `ImageKeyExtractor`는 유일한 Admin Promotion consumer 옆의 `internal` utility로 이동했으며 `global-support` project/dependency/source는 제거됐다. central shared replacement module은 만들지 않았다.
- Evidence: API/Admin JSON, CDN serializer, Promotion image-key, support-security/observability focused checks, three app checks/boot jars, target graph가 통과했다. 기존 observability Jupiter owner는 risk fidelity가 올바르므로 PR-22 authoring audit 전까지 유지한다.

### PR-21 — Infrastructure visibility and `module-contracts` retirement

- Objective: 모든 consumer/test 이동 뒤 remaining contract와 temporary adapter를 제거하고 infrastructure implementation을 internal로 닫는다.
- Invariant gained: central contracts가 사라지고 apps web/controller→infra implementation 및 application lane cross-dependency가 executable하게 금지된다.
- Dependencies: PR-16~20.
- Correctness risk: Spring bean discovery/configuration, thin port 증식, bootstrap access breakage.
- Compatibility: contract별 temporary adapter를 마지막 consumer migration까지 유지하고 bootstrap configuration만 allowlist한다.
- Rollback: adapter family별 commit; module deletion은 마지막 독립 commit.
- Tests: project dependency graph, focused Gradle/ArchUnit guard, all adapter slices, three app boots, zero-reference scans.
- DoD: `module-contracts` project/package/reference zero; controller/web infra implementation 접근 zero; application lanes independent; public infra exception은 ADR로 명시.
- Implemented: Redis guest session/throttle는 Booking-owned `GuestSessionStore`/`GuestAccessThrottle`, Slack notification은 기존 `BookingCreatedEvent`를 받는 `BookingNotificationSender`로 이동했다. `OptionalLong`과 중복 notification DTO를 제거하고 `module-contracts` project/dependency/source를 삭제했다. Infrastructure 구현은 `internal`이며 apps가 보는 public surface는 `EnableInfraBaseConfig`, `InfraBaseConfigGroup`, `InfraPersistenceConfig`, `AuthRedisConfig`뿐이다.
- Evidence: Frontoffice/Infrastructure/Security/API/Admin/Batch checks, transition/target graph, three boot jars를 강제 재실행해 97 tasks가 통과했다. Batch integration은 Domain repository와 JDBC cleanup만 사용하고 infrastructure 구현 타입을 import하지 않는다.

### PR-22 — Kotlin interop and legacy test retirement

- Objective: Kotlin-owned repository API의 `Optional`/nullable lookup id, Java-only `@Jvm*`, replacement가 완료된 Java/JUnit authoring debt와 source scans를 risk owner 단위로 제거한다.
- Invariant gained: Kotlin-owned API가 idiomatic하고 architecture rule은 source 문자열보다 compiler/Gradle/ArchUnit/risk-owner test가 소유한다.
- Dependencies: PR-21과 모든 test owner green.
- Correctness risk: nullable/Optional 변환이 not-found semantics를 바꾸거나 cleanup이 hidden coverage를 제거할 수 있다.
- Compatibility: Java caller를 Kotlin으로 옮긴 뒤에만 `@Jvm*`를 제거하며 사용자 소유 untracked probe는 변경하지 않는다.
- Rollback: cleanup category별 revert.
- Tests: affected Domain/Application/Infrastructure/API suites, architecture guards, unfiltered discovery comparison.
- DoD: tracked Java production/test와 unnecessary `@Jvm*`/Kotlin `Optional`은 zero; source-string architecture assertion은 compiler/Gradle/ArchUnit으로 대체되거나 non-code contract risk로 구체적 정당화된다.
- Evidence: Kotlin repository API는 nullable result/non-null id로 전환됐고 tracked Java caller는 zero다. Domain/API/Admin의 Java compatibility accessor는 Kotlin read-only property로 전환했다. 불필요한 `@JvmStatic` 54개, `@JvmOverloads` 6개, `@JvmSuppressWildcards` 6개를 제거했으며 value class의 `@JvmInline`만 유지한다. API/Admin/Batch/Frontoffice architecture rule은 compiled ArchUnit/Gradle이 소유한다. Root의 배포/Nginx/Sentry/inventory/version-catalog 계약 13개는 세 Kotlin FunSpec으로 분리했고, 실행을 위해 root의 기존 Java test coordination plugin을 `beat.kotlin-base`로 교체했다. Root main source와 executable plugin은 여전히 없다.
- Verification: ten module checks passed (93 tasks, 4m35s); root `compileTestKotlin`/`transitionBoundaryTest --rerun-tasks` passed (83 tasks); target graph and three boot jars passed. 사용자 소유 untracked Batch contention probe는 변경하지 않았다.

### PR-23 — Application failure boundary and apps-to-Domain retirement

- Objective: Domain failure를 Frontoffice/Admin Application failure language로 번역하고 Domain service composition을 각 Application configuration으로 이동해 API/Admin의 direct Domain dependency를 제거한다.
- Invariant gained: `Domain failure → Application failure → Web status`; apps web/controller는 Domain exception/error code를 알지 않으며 apps→Domain direct dependency는 explicit allowlist 없이 zero다.
- Dependencies: PR-22.
- Correctness risk: 기존 V1 status/message와 transaction rollback semantics가 달라질 수 있다.
- Compatibility: 현재 API/Admin domain-error mapping matrix를 characterization으로 고정하고 동일한 status/message/code를 Application failure mapping으로 이전한다.
- Rollback: Frontoffice와 Admin lane을 별도 commit으로 유지한다.
- Tests: Spring-free translation specs, affected Application use-case specs, API/Admin HTTP contract, concurrency failure type, two app checks/boot/OpenAPI.
- DoD: API/Admin production Domain imports/dependencies/config zero; Domain exception이 inbound adapter까지 직접 노출되지 않음; HTTP observable contract unchanged.
- Evidence: 모든 Frontoffice/Admin `@Service` public entry point가 method body 안에서 Domain failure를 lane Application exception으로 번역하며 compiled ArchUnit이 translator dependency 누락을 막는다. 따라서 transaction interceptor는 동일한 RuntimeException rollback semantics를 유지한다. Schedule/Promotion Domain service bean은 각 Application configuration이 소유한다. API/Admin main Domain import와 main dependency는 zero이며 test-only fixture dependency만 명시적으로 남는다.
- Admin correction: 실제 Admin use case는 Promotion/User이고 reachable Domain failure는 Promotion carousel invariant다. 기존 Admin handler의 Booking/Performance/Schedule special mapping은 도달 불가능한 copied knowledge라서 Application으로 이식하지 않았다. Promotion code/message와 generic Domain type semantics만 보존한다.
- Verification: Frontoffice/Admin Application checks, forced API/Admin checks (2m56s), booking overselling and price-lock concurrency, target graph, three boot jars, API/Admin OpenAPI tests, compatibility script가 통과했다.

### PR-24 — CI optimization and final migration gates

- Objective: measured test discovery를 기준으로 CI를 조정하고 전체 dependency/public surface/temporary adapter/runtime report를 확정한다.
- Invariant gained: 최종 architecture/test portfolio가 compiler, risk-owner tests, API/security/concurrency/OpenAPI/deploy gates로 증명된다.
- Dependencies: PR-23과 모든 risk owner green.
- Correctness risk: tag filter가 test를 조용히 누락하거나 최종 cleanup이 deploy artifact를 바꿀 수 있다.
- Compatibility: CI optimization과 semantic deletion을 분리하고 unfiltered `test`와 discovered suite를 비교한다.
- Rollback: CI/task/report category별 revert.
- Tests: full `check`, risk lanes, boot jars, MySQL/Redis, security, batch, OpenAPI, authorization, concurrency, dependency/build health, deploy artifact smoke.
- DoD: final graph/public API/Kotlin interop/source scan/cross-service/public infrastructure/temp adapter audit 완료; three apps independently executable; final report complete.
- Evidence: PR CI의 unfiltered `check`를 authoritative discovery로 유지하고 중복 `openApiTest --rerun-tasks`를 제거했다. 모든 deploy workflow는 `actionlint`를 통과하며 삭제된 legacy project path filter와 지원되지 않는 concurrency key가 없다. Production source를 읽는 architecture assertion은 Gradle/compiler/ArchUnit guard로 대체됐다.
- Final verification: clean commit `60afcb9e`에서 `check transitionBoundaryTest verifyTargetModuleGraph verifyModuleBootJars --rerun-tasks --no-build-cache`가 114 tasks로 통과했고, unfiltered 490 tests는 failure/error/skip이 모두 zero다. `buildHealth` 450 tasks, General/Admin OpenAPI compatibility, 29개 source Gradle Kotlin DSL 기준 unused catalog, workflow syntax, zero-reference/interop/public-surface audit도 모두 통과했다. 최종 결과와 deliberate filesystem mapping ADR은 `BEAT-SERVER-MIGRATION-FINAL-REPORT.md`에 기록한다.

### ADR-MIG-009 — Target Gradle identity와 deployment-compatible source path를 분리

- 최종 compile/dependency boundary는 Constitution의 `:apps:*`, `:application:*`, `:domain`, `:infrastructure`, `:support:*` 10개 project로 확정했다.
- 디스크의 `apis/admin/batch`, `core/domain`, `core/infra`, `gateway`, `observability` 경로는 deploy path filter와 runtime contract가 사용한다. `settings.gradle.kts`의 explicit `projectDir` mapping으로 target project identity에 연결한다.
- 디렉터리 rename은 compile/change isolation을 추가하지 않고 deploy diff와 rollback surface만 늘린다. Constitution의 “폴더 모양이 아니라 change locality” 원칙에 따라 이를 PR-24 semantic migration에 섞지 않는다.
- 이는 legacy Gradle module의 유지가 아니다. settings와 dependency graph에는 target project만 존재하며, legacy project name/reference는 executable negative guard 외에는 없다.

## 10. Architecture guard migration plan과 ADR

### 10.1 Executable dependency graph

```text
domain
X→ application / infrastructure / apps / support / Spring / JPA / Web / Redis

application:frontoffice
X→ application:admin / application:system / infrastructure / apps
→ support:security의 명시적 public technical API
application:admin
X→ application:frontoffice / application:system / infrastructure / apps
application:system
X→ application:frontoffice / application:admin / infrastructure / apps

infrastructure
→ domain
→ 필요한 application-owned ports/readers
X→ apps

apps
→ application
→ infrastructure configuration
→ support
X→ domain (default)

support:security
X→ application / infrastructure / apps / domain business model
→ Spring Security와 표준 library
```

Apps의 Domain 직접 dependency는 기본 금지한다. Domain failure를 ControllerAdvice가 직접 HTTP로 번역하지 않고 Application failure language를 거친다. 정말 필요한 composition-level type이 발견될 때만 Gradle/source allowlist와 ADR을 동시에 추가한다. 현재 develop evidence에는 그런 예외가 없다.

### 10.2 Enforcement order

1. **Gradle/compiler:** project dependency matrix, forbidden framework dependencies, bootJar policy, domain classpath test.
2. **Kotlin visibility:** Application helper/assembler/policy와 infra implementation `internal`; configuration/marker만 public.
3. **ArchUnit:** apps controller/web→infra implementation 금지, application→Web/JPA/Redis/JDSL type 금지, cross-lane application edge 금지, concrete Application Service cross-capability 호출 금지, support:security→application 금지.
4. **Capability/Actor ownership:** Frontoffice actor-specific use case는 `booking/booker`, `performance/{booker,maker}`, `schedule/booker`, `ticket/maker`, `home/booker` 아래에만 둔다. Member/Auth는 actor-neutral allowlist로 관리하고 Admin/System에는 중복 actor package를 금지한다. Actor 사이와 capability 사이의 concrete Application Service 호출을 금지한다.
5. **CQRS semantic tests:** Command monetary/ownership/inventory dependency가 read-model/cache/replica adapter를 사용하지 않음을 type/annotation graph로 검증한다. 단순 package-name scan으로 대체하지 않는다.
6. **Concurrency/integration:** Performance/Booking/Schedule lock outcome, bulk Ticket locks, guest identity race, DB clock을 Testcontainers로 검증한다.
7. **Legacy guard retirement:** 각 source-string assertion의 protected invariant를 위 mechanism에 매핑한 뒤 한 개씩 제거한다.
8. **Test authoring/build logic:** JUnit Platform discovery와 Kotest runner를 convention plugin으로 고정한다. module별 migration이 끝나면 Jupiter/Mockito authoring dependency를 제거해 신규 Kotlin test가 legacy style로 늘지 않게 compiler classpath로 제한한다.
9. **Test semantics:** Domain/Application test source에서 Spring context annotation을 금지하고, Spring integration은 lane-local meta-annotation/configuration을 사용한다. manual static container lifecycle과 test-level transaction을 사용하는 concurrency spec은 semantic guard와 review checklist로 막는다.

### ADR-MIG-001 — `performance/api`를 만들지 않고 Domain collaboration을 초기 선택

- Constitution example과 충돌하지 않는다. explicit Capability API는 후보이지 필수다.
- 현재 provider policy/remote volatility/multiple consumer가 없고, API가 결국 repository lock+field extraction을 forwarding한다.
- direct authoritative aggregate read가 더 작은 seam이며 Command correctness를 만족시킬 수 있다.
- downside인 broad repository surface/full aggregate load는 기록하고, usage guard와 performance measurement trigger를 둔다.
- 재검토 trigger: 두 번째 command consumer, provider-owned booking eligibility policy, remote/independent transaction boundary, measured aggregate-load regression.

### ADR-MIG-002 — Schedule은 독립 aggregate

- occurrence/close/inventory/numbering invariant와 repository/lock을 자체 소유한다.
- Performance Maker가 lifecycle을 orchestration하고 Booking이 inventory를 mutate해도 ownership은 바뀌지 않는다.
- 같은 PR/transaction은 ownership 증거가 아니다.

### ADR-MIG-003 — Ticket은 Application capability, Domain aggregate는 아님

- Maker endpoint와 change reason은 분명하지만 persistent state와 transition은 Booking/Schedule에 있다.
- 따라서 `application:frontoffice/ticket/maker`는 유지할 수 있으나 `domain.ticket`은 만들지 않는다.

### ADR-MIG-004 — Frontoffice actor-specific use case는 Actor를 명시

- `application:frontoffice`는 delivery/runtime lane이지 Booker/Maker 중 하나를 의미하지 않는다.
- Performance는 Booker/Maker를 모두 명시하고, Booking/Schedule/Home은 Booker, Ticket은 Maker를 명시한다. 현재 한 Actor만 존재한다는 사실은 생략 근거가 아니다.
- guest/member는 Booking Booker use case의 인증 상태이며 별도 Actor package로 복제하지 않는다.
- Member/Auth는 Booker/Maker 공통 identity/session capability이고 현재 actor-specific policy가 없어 actor-neutral allowlist로 둔다.
- Admin/System은 physical lane이 Actor를 이미 표현하므로 중복 package를 만들지 않는다.

### ADR-MIG-005 — Guest password hashing은 Application Output Port가 아니다

- AS-IS `GuestPasswordHashPort`는 `module-contracts`에 있고 gateway의 BCrypt service가 구현한다.
- 실제 기능은 DB/Redis/network/provider를 숨기지 않고 encode/matches/legacy verification/upgrade 판단만 수행한다.
- Constitution `§28`은 credential verification과 shared authentication primitives를 `support:security` 소유로 둔다.
- TO-BE에서 `support:security/password`가 좁은 public technical API와 internal BCrypt 구현을 함께 소유하고 Booking Application이 이를 직접 사용한다.
- `GuestSessionPort`, refresh-token store, social provider처럼 외부 상태/시스템을 숨기는 seam은 계속 Application-owned output port다.
- executable invariant는 `application:frontoffice → support:security`의 public API 사용 허용과 `support:security X→ application:frontoffice`다.

### ADR-MIG-006 — Booking lock-order resolution은 Schedule scalar identity를 사용한다

- Booking은 `scheduleId`만 받으므로 global `Performance → Schedule` lock order를 지키려면 먼저 Schedule이 소유한 `performanceId`를 확인해야 한다.
- 첫 구현의 `ScheduleRepository.findById`는 JPA 1차 캐시에 stale inventory를 남겨 concurrent oversell을 재현했다. 변경 전 `HEAD`는 동일 concurrency test를 통과했고, aggregate preliminary read를 추가한 후 5건 대신 30건이 성공했다.
- 새 Application Port/Capability API를 만들지 않고, authoritative Schedule repository에 non-null id를 받는 Kotlin-first scalar lookup `findPerformanceIdById(Long): Long?`를 추가한다.
- scalar는 lock-order hint이지 Command decision state가 아니다. 가격은 locked Performance, inventory/close/membership은 locked Schedule에서 재확정한다.
- 대안인 `Schedule → Performance` lock order는 Performance modify/delete와 충돌하고, Application-owned Port는 Schedule domain knowledge를 중복하며, persistence-context clear/refresh는 현 transaction의 다른 pending state를 건드릴 수 있어 선택하지 않았다.

### ADR-MIG-007 — Kotlin test authoring은 Kotest FunSpec, execution은 JUnit Platform

- Kotlin production과 test의 표현을 맞추고 Domain/Application/Adapter/Acceptance를 `context`/`test` 한 mental model로 읽기 위해 FunSpec을 기본으로 선택한다.
- JUnit Platform은 runner interoperability와 Gradle/IDE execution contract로 유지한다. Kotest 채택은 JUnit Platform 폐기가 아니다.
- unit test의 핵심 규칙은 framework 선택이 아니라 Spring 제거와 behavior isolation이다. real object→simple fake→MockK 순서로 사용하고 interaction verification은 boundary behavior에 필요할 때만 둔다.
- Spring integration/acceptance도 FunSpec을 사용한다. Lifecycle mode 지정은 기술적 필수사항이 아니지만, BEAT 정책은 nested leaf와 Spring test-method lifecycle이 1:1이 아님을 전제로 mode를 명시적으로 고정한다.
- Testcontainers는 real MySQL/Redis를 사용한다. Spring Context와 lifecycle을 공유하는 integration/acceptance container만 context-managed bean/import 방식으로 정렬하며 독립 repository/adapter test에 이를 일괄 강제하지 않는다.
- acceptance meta-annotation은 context configuration을 안정화하는 수단이며 중앙 test-common dumping ground가 아니다. app lane별 필요가 입증될 때만 둔다.
- concurrency/lock spec은 test-level transaction을 사용하지 않고 production Application Command transaction을 실행한다.
- Toss 사례는 선택 가능성을 뒷받침하는 industry evidence이지 BEAT 결정의 권위가 아니다. BEAT의 authoritative 근거는 Kotlin-first 목표, 실제 MySQL/Redis semantics, test ownership과 feedback 요구다.

### ADR-MIG-008 — Runtime/BOM upgrade와 test rewrite를 분리

- Constitution은 Spring Boot 4.1 채택을 조건부로 열어 두었지만 기존 Execution Record는 이를 PR-9 확정안으로 잘못 닫았다.
- 공식 Spring Cloud 지원표는 `2025.1.x`를 Boot `4.0.x`에만 대응시키고, Maven Central의 최신 `spring-cloud-dependencies:2025.1.3`도 `spring-boot.version=4.0.8`을 선언한다. 현재 Kakao/Slack adapter가 OpenFeign을 사용하므로 Cloud를 무시한 Boot 4.1 강행은 지원되지 않는 runtime 조합이다.
- PR-9는 지원되는 Boot `4.0.8`/Cloud `2025.1.3` patch line과 Kotlin `2.3.21`, Boot BOM과 동일한 Testcontainers `2.0.5`를 정렬한다. Testcontainers 2의 artifact/package 변경만 필수 호환 수정으로 허용한다.
- 기존 Cloud Context `5.0.1`이 `spring-boot-restclient`를 우연히 transitive 제공해 `ImageCacheAdapter`의 `RestClient.Builder` runtime requirement를 숨기고 있었다. Cloud `5.0.3`에서 그 transitive가 제거되므로 API/Admin external-client composition은 `spring-boot-starter-restclient`를 명시적으로 제공한다. 이는 새 abstraction이 아니라 기존 adapter의 실제 runtime dependency 복구다.
- Boot 4.0.8 BOM이 현재 명시적 핀보다 새 Tomcat/Jackson/Netty를 관리하므로 해당 downgrade override는 제거하고, BOM이 해결하지 않는 security pin만 근거와 함께 유지한다.
- Boot 4.1은 Cloud 호환 train 도입 또는 OpenFeign adapter retirement가 별도 evidence로 완료된 뒤 다시 판단한다. 이를 달성하기 위해 임의의 HTTP client 재작성이나 새 PR을 지금 발명하지 않는다.
- runtime/BOM alignment는 PR-9, Kotest coexistence foundation은 PR-10, critical lock characterization은 PR-11, Domain/Application/adapter/Web test ownership은 PR-12~15로 분리한다. 이후 capability migration은 각 risk owner를 함께 이동하고 final legacy deletion은 PR-22에서 수행한다.
- 이 경계는 실패 원인이 runtime upgrade인지 authoring framework인지 test semantic rewrite인지 즉시 식별하고 독립 rollback하기 위함이다.

## Audit gate

이 문서 승인 전에는 새 production/test class, interface, package, module을 만들지 않는다. 승인 뒤에도 각 PR 시작 시 해당 source rows와 open question을 다시 확인하고, evidence가 달라지면 PR graph와 ADR을 먼저 수정한다.
