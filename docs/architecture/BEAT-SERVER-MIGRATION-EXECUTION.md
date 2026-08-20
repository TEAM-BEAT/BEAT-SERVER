# BEAT-SERVER Migration Execution Record — develop 재감사본

Baseline: `develop` / `eb007147f6aa3824073b108407ea3ae47748aa40`
Architecture Constitution: `BEAT-SERVER-CQRS-MULTIMODULE-ARCHITECTURE-FINAL.md`
Audit date: 2026-08-20
Status: **implementation 전 재설계 보고서**. 아래 PR은 모두 계획이며 구현 완료를 뜻하지 않는다.

이 문서는 깨끗한 `develop` Git object를 별도 디렉터리에 전개해 조사했다. 현재 작업 브랜치의 target-module skeleton, `performance/api`, Booking offer 실험 코드는 evidence에서 제외했다. Constitution은 판단 기준이고, 이 문서는 현재 파일을 그 기준으로 옮기기 위한 실행 가설이다.

## 1. Constitution에서 변경할 수 없는 invariant

1. 최종 physical boundary는 `apps:api/admin/batch`, `application:frontoffice/admin/system`, `domain`, `infrastructure`, `support:security/observability`, `build-logic`이다.
2. `apps`는 inbound adapter와 composition root만 소유한다. HTTP/스케줄러 입력 변환을 넘어선 workflow를 소유하지 않는다.
3. `application`은 use case, application policy, command transaction, consumer-owned output port/query reader를 소유한다. Web/JPA/Redis/JDSL/Feign/S3 구현 타입을 참조하지 않는다.
4. `domain`은 aggregate, value object, business invariant와 필요한 domain service를 소유하며 framework-free다.
5. `infrastructure`는 Domain/Application abstraction의 driven adapter다. 구현 클래스는 기본 `internal`이다. 외부 상태를 숨기지 않는 credential verification 같은 공통 기술 기능은 `support:security`가 좁은 public technical API와 구현을 함께 소유할 수 있다.
6. logical/change ownership의 1차 축은 Business Capability다. Frontoffice에서 실제 actor가 둘 이상이면 Actor, 그 아래에서 Command/Query를 구분한다.
7. `Capability → Actor → Command/Query`는 분류 순서다. 빈 package 생성 규칙도, class naming 규칙도 아니다.
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
| `application/command/{GuestBookingCommandService,MemberBookingCommandService}.kt`, `BookingCommands.kt` | Booking 생성, Schedule inventory, Performance summary 조회, persistence | Booker Command | Booking, Schedule, Performance price | `application:frontoffice/booking/command` | MOVE 후 correctness 수정. 현재 `@ReadModel` money dependency 제거 |
| `application/command/BookingCancellationCommandService.kt` | refund/cancel, Booking/Schedule lock과 allocation release | Booker Command | Booking status/refund, Schedule inventory | same | MOVE. lock semantics 유지/검증 |
| `application/command/{GuestBookingAuthenticationCommandService,GuestBookingSessionCommandService}.kt`, `application/credential/GuestBookingCredentialAuthenticator.kt`, `application/GuestBookingIdentityValidation.kt` | guest credential, throttle, session actor resolution | Booker/anonymous Command | authoritative guest credential/session | `application:frontoffice/booking/command` 및 internal | MOVE. 중복 identity 결정 선행 |
| `application/query/{GuestBookingQueryService,MemberBookingQueryService}.kt` | Booking retrieval; legacy null amount fallback; Performance/Schedule 조합 | Booker Query | Booking snapshot + display projection | `application:frontoffice/booking/query` | MOVE. consumer reader 분리; fallback behavior 결정 필요 |
| `application/BookingPaymentAmount.kt` | price×quantity overflow-safe 계산; command/read error가 혼재 | internal policy | TicketPrice, Booking total | `application:frontoffice/booking` internal | MOVE. 별도 Port 불필요 |
| `application/event/{BookingCreatedEvent,BookingCreatedEventListener}.kt` | post-commit notification | Booker internal event/output | committed Booking snapshot | `application:frontoffice/booking` | MOVE. notification output은 Booking 소유 |
| `application/result/BookingResults.kt` | use-case output | Booker Command/Query output | snapshot/display 혼합 | command/query owner 인접 | SPLIT/MOVE. HTTP type과 분리 |
| `exception/BookingApplicationErrorCode.kt` 및 공용 `ApiApplicationException` 사용 | HTTP-coupled application failure | application failure | 없음 | `application:frontoffice/booking` | REPLACE. HTTP 독립 failure language |
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
| `application/query/ScheduleQueryService.kt`, `application/result/ScheduleResults.kt`, `application/DueDate.kt`, `facade/ScheduleFacade.kt`, `exception/ScheduleApplicationErrorCode.kt` | authoritative availability decision과 HTTP mapping | Booker Query + adapter | Schedule | app/application으로 SPLIT | facade/HTTP mapping은 apps; query/policy/failure는 `application:frontoffice/schedule/query` |
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
| `application/command/{TicketCommandService,TicketCommands}.kt` | payment confirmation/refund completion/delete over Booking/Schedule | Maker Command | Booking status, Schedule allocation, Performance owner | `application:frontoffice/ticket/command` | MOVE. Performance `@ReadModel` authorization 제거 |
| `application/query/{TicketQueryService,TicketListQuery}.kt`, `result/TicketRetrieveResult.kt` | Maker ticket list/search | Maker Query | consumer projection; separate auth | `application:frontoffice/ticket/query` | MOVE; reader vocabulary를 consumer가 소유 |
| `application/event/{TicketPaymentConfirmedEvent,TicketPaymentConfirmedEventListener}.kt` | post-commit SMS | Maker event/output | committed Booking state | `application:frontoffice/ticket` | MOVE. generic `SmsPort` 대신 consumer semantic 검토 |
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
| `application/query/HomeQueryService.kt`, `application/result/HomeResults.kt` | Performance/Schedule/Promotion consumer projection 조합 | Booker Query | 없음; read projection | `application:frontoffice/home/query` | MOVE. 하나의 Home-owned reader shape로 fold 가능 |

Home은 aggregate가 아니라 consumer query capability다. cross-capability join/projection을 허용하되 Command seam으로 재사용하지 않는다.

### 3.7 File / Storage

| Current file(s) | Current responsibility / callers | Actor / semantic | Authoritative state | Target | Action / reason |
|---|---|---|---|---|---|
| `apis/.../file/api/{FileApi,FileController}.kt`, `api/response/*`, `facade/FileFacade.kt` | `/file/presigned-url` HTTP surface | Performance Maker adapter | 없음 | `apps:api/file` | MOVE. route 보존; 앱 package 이름이 Domain owner를 의미하지 않음 |
| `application/command/FileCommandService.kt` | Performance maker image names validate + presigned upload request | Performance Maker Command | external object contract | `application:frontoffice/performance/maker/command` | MOVE; class rename은 caller/result 이동 후 책임이 실제로 더 선명해질 때만 수행. 현재 `File`은 business capability evidence가 없음 |
| `module-contracts/storage/**`, `core/infra/external/storage/s3/**` | Performance와 Admin Promotion storage methods/DTO를 한 interface에 결합 | multiple consumers | S3 object metadata | Performance Maker-owned storage output + temporary Admin legacy contract implemented by one infra adapter | SPLIT. PR-5에서 Performance presign/metadata vocabulary만 이동하고 Admin methods/DTO는 PR-10까지 compatibility로 남긴다 |

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
| Frontoffice | Booking | Booker/anonymous guest | create, authenticate/session, refund/cancel | member/guest retrieval | Actor 생략 가능. 현재 다른 actor use case 없음 |
| Frontoffice | Performance | Booker | 없음 | public detail/booking form | `performance/booker/query` 필요 |
| Frontoffice | Performance | Maker | create/modify/delete, Schedule orchestration, upload preparation | list/edit form | `performance/maker/{command,query}` 필요 |
| Frontoffice | Schedule | Booker | 없음 | availability | 단일 actor이므로 `schedule/query` 가능 |
| Frontoffice | Ticket | Maker | payment/refund/delete workflow | list/search | 단일 actor이므로 `ticket/{command,query}` 가능 |
| Frontoffice | Member | anonymous/Booker | social login/registration | 없음 | Actor package 불필요 |
| Frontoffice | Auth | anonymous/Booker | token refresh/signout/issuance | 없음 | Actor package 불필요 |
| Frontoffice | Home | Booker | 없음 | home projection | `home/query` |
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

현재 Performance modify/delete는 `Performance → sorted Schedule` 순서다. 현재 Booking create는 `Schedule`을 먼저 lock한 뒤 Performance summary를 읽는다. 이 상태에서 Booking에 뒤늦게 Performance lock을 추가하면 `Schedule → Performance`가 되어 반대 순서와 deadlock 가능성이 생긴다.

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
| `MakerTicketReadPort`, `MakerTicketListItemReadModel`, `MakerTicketBookingStatus`, `MakerTicketScheduleNumber` | Ticket Maker list/search / JDSL | MOVE-TO-QUERY-READER: `application:frontoffice/ticket/query` |
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
| `ImageCachePort` | Performance poster와 Admin Promotion prewarm / CDN adapter | PR-5에서 Performance 사용 제거: committed poster event를 infrastructure가 구독. Admin legacy contract는 PR-10까지 유지한 뒤 central contract 삭제 |
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
5. **Actor package 과잉:** Booking을 무조건 `booking/booker`로 정렬하려 했다. 실제 단일 actor capability에는 불필요하다.
6. **Schedule ownership 표현 혼동:** reference slice sequencing을 “Booking enabling seam”으로 표현해 Schedule의 독립 aggregate ownership을 흐렸다.
7. **Contract relocation 중심 분류:** mixed summary/storage/JWT contracts를 consumer semantics보다 새 package 위치로 먼저 분류했다.
8. **Kotlin-first 후순위화:** 별도 말기 PR로 대부분 미뤘다. 실제로는 Java caller를 각 slice에서 옮긴 직후 compatibility surface를 제거해야 한다.
9. **Guard 후순위화:** invented `booking→performance.api` allowlist를 먼저 상정하고 compiler-level macro graph와 apps→domain policy를 충분히 앞세우지 않았다.
10. **PR 완료 상태 오기:** 작업 브랜치 실험 결과를 baseline PR 완료처럼 기록했다. 이 재감사본에서 모든 migration PR은 미착수다.

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
│       │   ├── command
│       │   └── query
│       ├── performance
│       │   ├── booker
│       │   │   └── query
│       │   └── maker
│       │       ├── command
│       │       └── query
│       ├── schedule
│       │   └── query
│       ├── ticket
│       │   ├── command
│       │   └── query
│       ├── member
│       │   └── command
│       ├── auth
│       │   └── command
│       └── home
│           └── query
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

## 9. Revised PR graph

```text
PR-1 ─┐
PR-2 ─┼→ PR-4 → PR-5 → PR-6 ───────────────┐
PR-3 ─┘    │      └→ PR-8                   │
           └→ PR-7 → PR-9 → PR-10 ← PR-5   ├→ PR-13 → PR-14
PR-3 ─────────────────→ PR-11               │
PR-3 ─────────────────→ PR-12 ──────────────┘
```

`PR-13`은 PR-4부터 PR-12까지의 모든 실제 consumer migration 완료에 의존한다.

PR-7, PR-8, PR-11, PR-12는 선행 consumer contract 충돌이 없도록 실제 merge 시점에 병렬 가능하다. 번호는 Architecture가 아니며 source overlap이 달라지면 graph를 수정한다.

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
- Compatibility: Performance/Schedule/File routes, status/message/JSON, transaction boundary, S3 key/presigned URL semantics, after-commit best-effort CDN prewarm 유지. Admin storage/cache legacy contract는 PR-10까지 temporary compatibility로 남긴다.
- Rollback: capability slice revert; no schema change.
- Tests: Performance/Schedule domain/application, modification/delete lock and active-booking behavior, child diagnostic, JPA/JDSL/JDBC readers, File/Performance/Schedule API contract, S3 adapter, context bootstrap, capability/implementation-access guards.
- DoD: 실제 use case가 있는 `performance/booker/query`, `performance/maker/{command,query}`, `schedule/query`만 존재; no `performance/api`; Schedule ownership 독립; Performance application에서 `ApiApplicationException`, apps error, Web, infrastructure, `module-contracts` import zero; `PerformanceEditFormReadPort`의 `Optional` 제거; Performance 용 broad storage/cache methods retired; touched Java caller를 Kotlin으로 옮긴 뒤 불필요한 `@Jvm*` 제거; apps facade/controller는 application public use case/output 외 infrastructure implementation에 접근하지 않음.

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

### PR-8 — Home consumer projection

- Objective: Home projection을 consumer-owned reader로 fold하고 Performance/Schedule/Promotion central read contracts를 제거한다.
- Invariant gained: Query consumer가 vocabulary와 optimization을 소유한다.
- Dependencies: PR-5; Promotion domain/adapter는 현재 contract compatibility로 독립 이동 가능.
- Correctness risk: ordering/due-date/genre/filter and carousel composition.
- Compatibility: Home JSON/order 유지.
- Rollback: reader adapter와 use case slice revert.
- Tests: Home application, JDSL/JDBC integration, API contract.
- DoD: Home query만의 projection; Home model이 Command에 노출되지 않음.

### PR-9 — Admin User lane foundation

- Objective: apps:admin→application:admin User query를 먼저 검증해 runtime boundary를 작은 slice로 연다.
- Invariant gained: Admin lane이 Actor를 중복 package로 만들지 않고 use case를 소유한다.
- Dependencies: PR-3, PR-7 security wiring.
- Correctness risk: admin role vs member-existence policy.
- Compatibility: `/users` behavior 유지.
- Rollback: small slice revert.
- Tests: Admin user query, security/HTTP, admin boot.
- DoD: controller has mapping only; no Domain failure direct HTTP mapping.

### PR-10 — Admin Promotion

- Objective: Promotion command/query, Performance existence, S3/cache outputs를 application:admin으로 이동한다.
- Invariant gained: Admin command authoritative referential checks와 consumer-owned storage seams.
- Dependencies: PR-5, PR-7, PR-9.
- Correctness risk: carousel uniqueness/order, external/internal redirect, S3 object validation/cache failure.
- Compatibility: Admin routes/JSON/storage keys 유지.
- Rollback: Promotion slice revert.
- Tests: Promotion domain/application, concurrent Performance delete/existence Testcontainers, JPA/S3/cache adapters, Admin API/security.
- DoD: no Admin Command→`PerformanceSummaryReadModel`; broad storage/cache central contracts 제거.

### PR-11 — System/Batch workflows and Java production retirement

- Objective: two maintenance services를 application:system Kotlin use case로, jobs를 apps:batch scheduler adapter로 이동한다.
- Invariant gained: apps:batch has no maintenance workflow; production Java 제거.
- Dependencies: PR-3; PR-4/10과 source conflict가 생기면 해당 PR 후 merge.
- Correctness risk: clock/timezone, cron/error handler, large delete/reorder transaction.
- Compatibility: scheduler names/cron/job behavior/deploy runtime 유지.
- Rollback: job별 commit revert.
- Tests: Clock-controlled application tests, scheduler invocation, batch boot, repository integration.
- DoD: four Java production files gone; thin facades gone; no unjustified `@Jvm*` left for them.

### PR-12 — Observability/global-support ownership

- Objective: observability physical target move와 global-support 분해를 수행한다.
- Invariant gained: narrow technical support; HTTP response/Jackson policy는 apps가 소유.
- Dependencies: PR-3.
- Correctness risk: logging/MDC/Sentry/serialization behavior.
- Compatibility: log fields, trace IDs, CDN serialization, error envelope 유지.
- Rollback: support concern별 revert.
- Tests: observability tests, serialization/API snapshots, three boot apps.
- DoD: `global-support` retired; no Domain dependency in observability.

### PR-13 — Infrastructure visibility and `module-contracts` retirement

- Objective: 모든 consumer migration 후 remaining contract/config bridge를 제거하고 infra implementation을 internal로 닫는다.
- Invariant gained: no central contracts, apps web→infra implementation 금지, minimal bootstrap API.
- Dependencies: PR-4 through PR-12 relevant consumers.
- Correctness risk: Spring bean discovery/configuration and hidden implementation access.
- Compatibility: explicit bootstrap configuration allowlist only.
- Rollback: adapter family별 commits; module deletion은 마지막 commit.
- Tests: full context/adapter integration, dependency graph, zero-reference scans.
- DoD: `module-contracts` project/package/reference zero; public infra implementation exceptions documented.

### PR-14 — Physical tree alignment, Kotlin/API minimization, legacy guard retirement, final verification

- Objective: legacy names/directories를 target tree에 최종 정렬하고 remaining Kotlin/public/test debt를 감사한다.
- Invariant gained: target physical graph와 semantic guards가 최종 상태를 증명한다.
- Dependencies: PR-13.
- Correctness risk: CI/deploy paths, Java ABI test callers, obsolete guard removal.
- Compatibility: deployment module alias/rollback path를 release plan에 따라 유지 또는 명시적으로 전환.
- Rollback: physical rename/CI mapping commit 단위.
- Tests: full `check`, boot jars, Testcontainers, security, batch, API, dependency/build health, deploy artifact smoke.
- DoD: final dependency graph; remaining Java/`@Jvm*`/Optional/public API/source-scan/temp adapter 전수 justification; all three apps executable.

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
4. **Capability ownership:** `performance` 안의 Booker/Maker packages가 서로의 concrete service를 호출하지 않도록 보호. Booking에는 evidence 없는 `booker` package를 강제하지 않는다.
5. **CQRS semantic tests:** Command monetary/ownership/inventory dependency가 read-model/cache/replica adapter를 사용하지 않음을 type/annotation graph로 검증한다. 단순 package-name scan으로 대체하지 않는다.
6. **Concurrency/integration:** Performance/Booking/Schedule lock outcome, bulk Ticket locks, guest identity race, DB clock을 Testcontainers로 검증한다.
7. **Legacy guard retirement:** 각 source-string assertion의 protected invariant를 위 mechanism에 매핑한 뒤 한 개씩 제거한다.

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
- 따라서 `application:frontoffice/ticket`은 유지할 수 있으나 `domain.ticket`은 만들지 않는다.

### ADR-MIG-004 — Actor package는 capability별 evidence로 생성

- Performance는 Booker와 Maker의 command/query가 모두 있어 Actor axis가 필요하다.
- Booking, Ticket, Home, Schedule은 현재 한 actor만 확인되어 Actor directory를 생략할 수 있다.
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

## Audit gate

이 문서 승인 전에는 새 production/test class, interface, package, module을 만들지 않는다. 승인 뒤에도 각 PR 시작 시 해당 source rows와 open question을 다시 확인하고, evidence가 달라지면 PR graph와 ADR을 먼저 수정한다.
