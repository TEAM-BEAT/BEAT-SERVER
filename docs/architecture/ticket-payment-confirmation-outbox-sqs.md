# 예매자 입금 확인과 SMS 발송 비동기 처리 아키텍처

> 예매 확정 트랜잭션과 SMS 발송을 분리하고, DB Job Queue와 Transactional Outbox + Amazon SQS 중 전달 경로를 결정하기 위한 기술 기획

- 상태: 제안 — 원자적 SMS 작업 생성은 확정, 전달 경로는 Phase 0의 `DB Job Queue vs Outbox + SQS` ADR 승인 전까지 미확정
- 기준일: 2026-07-30
- 대상 API: `PUT /api/tickets/update`
- 대상 모듈: `apis`, `batch`, `domain`, `module-contracts`, `infra`
- 확정 결정: **예매 변경·요청 멱등성·SMS delivery를 하나의 MySQL transaction에 기록하고 fencing 기반으로 멱등 처리한다**
- 조건부 결정:
    - **DB Job Queue**: `sms_delivery` 자체를 durable queue로 polling한다.
    - **Outbox + SQS**: 같은 transaction에 `outbox_event`를 추가하고 Amazon SQS Standard/DLQ와 SQS Consumer를 사용한다.

## 1. 결론

예매 확정은 요청 안에서 DB 트랜잭션으로 끝내고, SMS는 내구성 있는 비동기 작업으로 분리한다.

```text
Admin
  │ PUT /api/tickets/update
  ▼
apis
  │ 1. commandId 멱등성·권한·입력·상태 전이 검증
  │ 2. booking_confirmation_idempotency 기록
  │ 3. booking 일괄 변경
  │ 4. sms_delivery PENDING + 암호화 snapshot 일괄 저장
  │ 5. SQS 선택 시 outbox_event 일괄 저장
  │    └─ 2~4 또는 2~5는 같은 MySQL transaction
  ▼
HTTP 200
  │ booking 확정 완료, SMS는 QUEUED
  ▼
batch polling
  ├─ DB Job Queue 선택 ──▶ batch: SMS polling worker
  │                         └─ sms_delivery 직접 claim
  │
  └─ SQS 선택 ──▶ Outbox Relay ──▶ Amazon SQS Standard ── 실패 누적 ──▶ DLQ
                                      │
                                      ▼
                                batch: SQS Listener
  │
  │ processing_token으로 delivery claim
  │ SOLAPI 호출
  │ ACCEPTED/RETRY_WAIT/FAILED/ACCEPTANCE_UNKNOWN 기록 후 조건부 완료
  ▼
SOLAPI
```

선택의 핵심은 다음과 같다.

- 공통 불변식은 요청 멱등성 원장, booking 변경, SMS delivery 생성을 하나의 로컬 MySQL 트랜잭션으로 묶는 것이다.
- DB Job Queue를 선택하면 `sms_delivery`가 유일한 작업 원장이다. 별도 Outbox를 만들지 않는다.
- SQS를 선택하면 Outbox 기록까지 같은 트랜잭션에 추가하고 application service가 `OutboxEventRecordPort.recordAll()`을 직접 호출한다.
- SQS 경로의 1차 발행 방식은 Polling Publisher 하나만 운영한다. `AFTER_COMMIT` fast path는 실제 publish latency가 SLO를 위반할 때만 ADR을 거쳐 추가한다.
- DB와 SQS를 동시에 쓰는 분산 트랜잭션을 시도하지 않는다.
- API 성공은 `DB commit + SMS 작업 등록`을 뜻한다. SMS 도착까지 뜻하지 않는다.
- SQS는 중복 전달될 수 있다고 전제하고 소비자를 멱등하게 만든다.
- SMS 사업자의 수락 여부가 불명확하면 `ACCEPTANCE_UNKNOWN`, 수락 후 단말 전달 여부가 불명확하면 `DELIVERY_UNKNOWN`으로 분리한다.
- provider의 발송 요청 수락과 단말 전달 완료를 `ACCEPTED`, `DELIVERED`로 구분한다.
- 현재 단일 Redis 인스턴스를 메시지 내구성 또는 relay lock에 사용하지 않는다.
- SQS는 처리량 때문에 선제 채택하지 않는다. 관리형 buffering·DLQ·독립 worker 확장의 가치가 운영비보다 큰지 Phase 0 ADR에서 결정한다.
- 초기에는 별도 worker 모듈을 만들지 않고 기존 `batch` 프로세스를 사용한다. 독립 배포·확장이 실제로 필요할 때 분리한다.
- 현재 단일 host의 `batch`는 장애 후 복구 가능한 durable delivery를 제공하지만 무중단 고가용성을 제공하지는 않는다.

## 2. 문제 상황

2026년 5월 29일경 약 78명의 예매 상태를 한 요청에서 입금 확인 처리했다. 클라이언트는 약 0.5초 뒤 연결을 끊어 nginx가 `499`를 기록했지만, 서버는 처리를 멈추지 않고 약 14.153초 뒤
`200` 처리를 완료했다.

nginx의 다음 경고는 긴 처리 시간의 주원인이 아니다.

```text
request body buffered to a temporary file
```

요청 본문이 `client_body_buffer_size`보다 커서 메모리 대신 임시 파일에 저장됐다는 뜻이다. 수십 KB 수준의 본문에서는 보통 밀리초 단위 부수 비용이다. 14초의 핵심은 애플리케이션 내부의 반복
DB 접근과 SMS 외부 I/O였다.

### 2.1 현재 코드에서 재발할 수 있는 이유

현재 브랜치는 과거의 완전 동기식 SMS 발송보다 개선됐지만, 문제를 근본적으로 제거하지 못했다.

- [
  `TicketCommandService.updateTickets()`](../../apis/src/main/kotlin/com/beat/apis/ticket/application/command/TicketCommandService.kt#L34)
  는 booking마다 저장하고 이벤트를 발행한다.
- [
  `lockSchedulesThenBookings()`](../../apis/src/main/kotlin/com/beat/apis/ticket/application/command/TicketCommandService.kt#L102)
  는 먼저 일괄 조회한 뒤 booking마다 `lockById()`를 호출한다. 약 78건이면 booking lock query도 약 78번 발생한다.
- [
  `TicketPaymentConfirmedEventListener`](../../apis/src/main/kotlin/com/beat/apis/ticket/application/event/TicketPaymentConfirmedEventListener.kt#L15)
  는 `AFTER_COMMIT + @Async`로 SMS를 호출한다.
- [`beatAsyncExecutor`](../../infra/src/main/java/com/beat/infra/config/TaskExecutorConfig.java#L22)는 포화 시
  `CallerRunsPolicy`를 사용한다.
- 현재 thread pool은 [`core=2`, `max=4`, `queue=50`](../../infra/src/main/resources/application-thread-pool.yml#L1)이다. 78개
  작업이 한꺼번에 제출되면 수용량을 넘은 일부 SMS 작업이 요청 thread에서 실행될 수 있다.
- listener가 SMS 오류를 log만 남기고 종료하므로 프로세스 종료나 일시 장애 뒤 복구할 작업 원장이 없다.
- [`CoolSmsAdapter`](../../infra/src/main/java/com/beat/infra/external/notification/sms/CoolSmsAdapter.java#L25)는 외부
  API를 동기 호출한다.

따라서 현재 코드에서도 다음 문제가 남는다.

1. executor 포화 시 요청 지연이 다시 발생할 수 있다.
2. commit 이후 프로세스가 종료되면 SMS 이벤트가 유실될 수 있다.
3. CoolSMS 일시 장애를 자동 복구할 수 없다.
4. SMS 성공 여부를 운영자가 조회하거나 재처리할 근거가 없다.
5. 요청의 78개 상세 DTO 중 서버가 실제 명령에 쓰는 값은 `bookingId`, `bookingStatus`뿐이다. 불필요한 이름·전화번호·스케줄 정보가 nginx까지 전달된다.

## 3. 요구사항과 명시적 가정

### 3.1 확정한 요구사항

- 예매 상태 확정은 금전·결제 후속 처리와 연결될 수 있는 핵심 트랜잭션이다.
- SMS는 예매 확정의 결과를 알리는 부수 효과다. SMS 실패 때문에 이미 유효한 예매 확정을 되돌리면 안 된다.
- 한 번에 약 80건을 처리한 실제 사례가 있다.
- 현재 SMS 소비자는 CoolSMS 한 곳이다.
- 현재 Redis는 단일 Docker 인스턴스이며 인증·세션·향후 cache와 장애 영역을 공유한다.
- 현재 Spring Boot는 `4.0.6`, Spring Cloud는 `2025.1.1`이다.

### 3.2 구현 전에 확인할 값

다음 값은 코드만으로 확정할 수 없으므로 운영 측정 또는 사업자 계약 확인이 필요하다.

- CoolSMS 계정의 초당·분당 발송 제한
- SOLAPI가 사용자 지정 request idempotency key를 공식 계약으로 제공하는지, group/message ID로 발송 결과를 어디까지 조회할 수 있는지
- inventory의 MySQL 8.4.5와 실제 운영 version이 일치하는지, `SKIP LOCKED` 실행 계획이 의도대로인지
- 운영 DB의 Multi-AZ, backup, PITR, RTO/RPO
- 허용 가능한 SMS 도착 지연 SLO
- 암호화한 notification snapshot의 보안·보존·파기 정책
- `ddl-auto=none` 환경에서 schema migration을 실행할 owner, 도구, 검증·forward-fix 절차
- 현재 `net.nurigo:javaSDK:2.2`에서 공식 `com.solapi:sdk`로 전환할 때의 timeout, typed error, 결과 조회 contract

문서의 timeout, concurrency, retry 횟수는 초기 제안값이며 load test와 사업자 제한을 확인한 뒤 확정한다.

## 4. 트랜잭션과 오류 정책

### 4.1 1차는 기존 all-or-nothing 계약을 유지한다

부분 성공은 운영 편의를 높일 수 있지만 현재 API는 `SuccessResponse<Void>`이고, 취소·환불 상태가 하나라도 섞이면 전체 transaction이 rollback되는 계약이다. Outbox 도입과
동시에 부분 성공까지 적용하면 client가 일부 실패를 표시하지 못한 채 전체 성공으로 오인할 수 있다.

따라서 1차 변경은 다음 정책을 유지한다.

| 오류 종류              | 예시                                                            | 1차 정책                                 |
|--------------------|---------------------------------------------------------------|---------------------------------------|
| 요청 오류              | 공연 소유권 없음, 중복 bookingId, 지원하지 않는 명령, 다른 공연 booking 혼입         | 변경 전 전체 거절                            |
| 상태 충돌              | 취소·환불 상태라 확정 불가                                               | 전체 rollback, 기존 4xx 유지                |
| 동일 명령 재요청          | 이미 `BOOKING_CONFIRMED`                                        | 멱등 성공, 신규 SMS event 생성 안 함            |
| 인프라·예상 밖 오류        | DB timeout, deadlock 재시도 소진, delivery 또는 조건부 Outbox insert 실패 | booking 포함 전체 rollback                |
| SQS 선택 시 broker 장애 | timeout, throttling, 권한 오류                                    | booking commit 유지, Outbox 재시도         |
| SMS 장애             | 사업자 5xx/timeout/잘못된 번호                                        | booking commit 유지, retry·경로별 격리·운영 확인 |

항목별 부분 성공은 별도 command endpoint 또는 API version에서 client와 함께 도입한다. 그때에도 요청 전체 오류는 전체 거절하고, 예상 가능한 상태 충돌만 항목별 결과로 분리하며 항목마다
`REQUIRES_NEW`를 만들지 않는다.

### 4.2 API 성공의 의미

1차 HTTP wire contract는 기존 `SuccessResponse<Void>`를 유지한다.

- `200 OK`: booking과 SMS delivery가 commit됐다. SQS 경로에서는 Outbox도 commit됐다. SMS는 아직 발송 중일 수 있다.
- `4xx`: 요청 전체 오류로 아무 booking도 변경하지 않았다.
- `5xx`: DB transaction이 commit되지 않았다.
- SMS 사업자 장애는 이 API를 `5xx`로 바꾸지 않는다.

동일 명령의 timeout·connection close 재시도를 안전하게 처리하기 위해 client가 `Idempotency-Key` UUID를 전달하고 서버는 request hash를 저장한다. 동일 key와
동일 command의 committed row가 있으면 기존 `200 OK`를 반환하고, 동일 key에 다른 command가 오면 `409 Conflict`를 반환한다.

기존 client 호환을 위해 전환 기간에는 header가 없으면 서버가 key를 생성하되, 이 요청은 client 재시도 멱등성을 보장하지 못한다고 metric으로 남긴다. client가 같은 key를 재사용하도록
배포된 뒤 header를 필수화한다. 따라서 “499 뒤 안전한 재시도” 보장은 client 전환 전에는 성립하지 않는다.

이후 별도 API version에서 서버가 쓰지 않는 client 전달 필드를 제거하고, “상태 일반 수정” 대신 “입금 확인”을 명시하는 command endpoint와 항목별 결과 DTO를 함께 도입한다.

요청 건수는 무제한으로 받지 않는다. Phase 0에서 운영 payload와 transaction lock 시간을 측정해 `@Size` 상한과 HTTP body 상한을 정하고, API·nginx 값이 서로 모순되지
않게 contract test로 고정한다. `200건`은 부하 테스트 후보이지 확정 운영 한도가 아니다.

## 5. 확정 설계와 전달 경로 ADR

### 5.1 원자적 SMS 작업 생성

booking 변경과 “SMS를 보내야 한다”는 사실을 같은 MySQL 트랜잭션에 저장한다.

```text
BEGIN
  INSERT booking_confirmation_idempotency ...
  UPDATE booking ...
  INSERT sms_delivery ...
  IF SQS 경로 THEN INSERT outbox_event ...
COMMIT
```

- DB commit 실패: idempotency record, booking, delivery와 조건부 Outbox가 함께 사라진다.
- idempotency/delivery insert 실패: booking도 rollback된다.
- SQS 경로에서 Outbox insert 실패: booking과 delivery도 rollback된다.
- SQS 경로에서 DB commit 성공 후 broker 장애: event가 DB에 남아 relay가 다시 시도한다.

DB commit 뒤 애플리케이션에서 SQS를 바로 호출하는 dual write는 사용하지 않는다. DB commit 성공과 SQS publish 사이에 프로세스가 죽으면 이벤트가 영구 유실되기 때문이다.

### 5.2 동일 transaction 직접 기록 + Polling Worker

SMS delivery는 booking과 함께 반드시 저장돼야 하는 공통 불변식이다. Spring event listener bean/import가 누락돼도 booking만 commit되는 구조를 만들지 않는다.
SQS 경로에서는 Outbox도 동일한 필수 기록에 포함한다.

```text
TicketCommandService.updateTickets()
  ├─ bookingConfirmationIdempotencyPort.record(...)
  ├─ bookingRepository.saveAll(changedBookings)
  ├─ smsDeliveryRecordPort.recordAll(PENDING deliveries)
  └─ SQS 선택 시 outboxEventRecordPort.recordAll(events)
       └─ 선택된 기록 모두 같은 @Transactional 경계

batch polling
  ├─ DB Job Queue: PENDING/RETRY_WAIT delivery를 BookingConfirmationSmsJob이 직접 claim
  └─ SQS: READY 또는 lease 만료 event를 OutboxRelay가 claim해 SQS로 전송
```

- application service는 멱등성 원장을 먼저 기록하고 booking별 `notificationId`를 한 번 생성한다. SQS 경로에서는 Outbox `eventId`도 한 번 생성한다.
- delivery와, SQS 경로의 Outbox는 `saveAll()`을 사용하고 JPA flush 시 실제 batch insert 여부를 검증한다.
- recorder adapter는 `REQUIRES_NEW`를 사용하지 않는다. 하나의 transaction manager에 참여하고, transaction 밖 호출을 즉시 실패시키도록 `MANDATORY` 경계
  또는 동등한 guard를 둔다.
- 공통 transaction test는 idempotency record 1건과 변경 booking·delivery 수가 일치하는지 검증한다. SQS 경로에서는 Outbox 수도 함께 검증한다.
- 선택한 경로에는 polling worker 하나만 둔다.
- SQS 경로의 `AFTER_COMMIT` fast path는 `outbox → SQS p99`가 polling만으로 SLO를 만족하지 못한다는 운영 데이터가 있을 때 별도 ADR로 도입한다.
- fast path를 나중에 추가하더라도 polling과 동일한 claim/fencing/publish 코드를 호출하고 request thread에서 SQS를 호출하지 않는다.

### 5.3 DB Job Queue 선택 시 `sms_delivery`를 직접 polling한다

DB Job Queue는 `sms_delivery`를 유일한 작업 원장으로 사용한다. 별도 `outbox_event`, relay, broker 상태를 만들지 않는다.

```text
짧은 transaction A
  due PENDING/RETRY_WAIT ids 조회
  SELECT ... FOR UPDATE SKIP LOCKED
  UPDATE status=PROCESSING,
         processing_token=<claim마다 새 UUID>,
         processing_until=<DB current timestamp + lease>
COMMIT

transaction 밖
  SOLAPI 호출

짧은 transaction B
  processing_token 조건으로
  ACCEPTED/RETRY_WAIT/FAILED/ACCEPTANCE_UNKNOWN 반영
COMMIT
```

- provider 호출 뒤 결과 commit 전 종료는 `ACCEPTANCE_UNKNOWN`으로 격리한다.
- 명확한 일시 실패만 `RETRY_WAIT + next_attempt_at`으로 전이한다.
- 영구 실패와 재시도 소진은 `FAILED`로 남겨 운영 조회 대상으로 삼는다.
- `processing_until`이 만료된 row는 provider 조회 또는 운영 판정 없이 자동 재전송하지 않는다.
- DB backlog, lock wait, oldest due age를 측정하고 worker batch와 concurrency를 조정한다.

#### 5.3.1 DB connection budget과 API 우선순위

Connection Pool 크기에 서비스 종류만으로 정해지는 공식값은 없다. HikariCP가 소개하는
`((physical core count * 2) + effective spindle count)`는 DB 서버 전체의 동시 처리량을 탐색하는 시작점이며 모듈마다 그대로 배정하는 값이 아니다. 최종값은 실제
transaction의 connection 보유시간과 DB latency knee를 부하 테스트로 찾아야 한다.

모듈별 수요의 1차 추정에는 Little's Law를 사용한다.

```text
module concurrent connection demand ≈ peak DB transaction/sec × connection hold time(sec)
```

추정값 주변에서 Pool 크기를 바꿔 가며 API p95/p99, Hikari pending, DB `Threads_running`, lock wait와 TPS가 악화되기 시작하는 지점을 찾는다. 티켓팅 burst에서는
평균값만으로 결정하지 않고 peak window와 p95/p99 connection usage를 사용한다.

현재 inventory와 Hikari 기본 동작을 기준으로 한 최대 연결 예산은 다음과 같다. `minimum-idle`을 별도로 지정하지 않았으므로 Hikari 기본값은
`maximum-pool-size`와 같아 각 컨테이너가 fixed-size Pool처럼 동작한다.

| 환경 | apis | admin | batch | steady-state 합계 | apis Blue-Green 중 합계 |
|------|-----:|------:|------:|------------------:|------------------------:|
| dev  | 10   | 1     | 1     | 12                | 22                      |
| prod | 10   | 2     | 2     | 14                | 24                      |

2026-07-30 확인 기준 dev/prod는 동일 RDS와 DB 사용자를 공유하고 schema만 분리돼 있으며 `max_connections=60`이다. 따라서 환경별 예산을 따로 계산하지 않고 합산한다.
평상시 application ceiling은 26, dev/prod apis Blue-Green이 동시에 겹치는 최악값은 46이다. RDS 관리·운영자·migration 연결은 이 숫자 밖에서 별도 예약하므로,
DB Job Queue 실험을 이유로 Pool을 선제 증설하지 않는다.

합계는 “모듈당 설정값”이 아니라 동시에 살아 있는 모든 container replica를 포함해 계산한다. 위 표는 환경별 구성을 보여주지만, 실제 용량 판단에는 공유 RDS를 사용하는
두 환경과 migration·운영 도구의 연결을 모두 합산한다.

```text
application connection ceiling
  = Σ(module maximumPoolSize × simultaneously live instances)

safe DB connection budget
  = min(load test에서 확인한 latency knee,
        max_connections - 운영자·migration·monitoring 예약분)

application connection ceiling <= safe DB connection budget
```

현재 `foundation_mysql_enabled: false`이므로 inventory의 `mysql:8.4.5` 문자열만으로 실제 dev/prod DB version, core, memory, storage, `max_connections`를
확정하지 않는다. Phase 0에서 각 환경의 실제 값과 Blue-Green overlap을 확인한다.

DB Job Queue를 기존 batch Pool에 넣는 경우 Worker가 batch의 두 connection을 모두 점유하지 않도록 staging의 시작값은 `worker concurrency=1`로 둔다. 기존
scheduled job과의 경합을 관측한 뒤 2를 검증하며, Pool이 2인데 Worker를 4로 늘리는 구성은 처리량 확장안으로 승인하지 않는다. 별도 Worker Pool을 만들더라도 같은
MySQL 연결 예산에 합산되므로 DB 격리가 되지는 않는다.

티켓팅 API를 우선 보호하기 위한 정책은 다음과 같다.

- API transaction에서는 provider network I/O를 수행하지 않는다.
- DB Job Queue의 claim/result transaction을 짧게 유지하고 provider 호출은 transaction 밖에서 수행한다.
- 빈 queue polling은 고정 고빈도 loop 대신 backoff와 jitter를 적용한다.
- API Hikari pending 또는 DB `Threads_running`·lock wait가 임계값을 넘으면 Worker concurrency를 줄이고 backlog를 허용한다.
- SMS backlog drain보다 예매 API SLO와 DB 복구용 connection 예약을 우선한다.

### 5.4 SQS 선택 시 Outbox claim은 MySQL lease를 사용한다

리디는 초기 다중 relay에서 Redis lock과 MySQL lock을 함께 썼지만, 후속 개선에서 MySQL `NOWAIT`로 Redis 의존성을 제거했다. BEAT는 처음부터 단일 Redis 장애 영역을
relay에 추가하지 않는다.

SQS relay는 외부 I/O 동안 DB transaction과 row lock을 계속 잡지 않는다.

```text
짧은 transaction A
  SELECT candidate ids
  SELECT ... FOR UPDATE SKIP LOCKED
  UPDATE status=CLAIMED,
         claim_token=<claim마다 새 UUID>,
         claimed_by=<instance id>,
         lease_until=<DB current timestamp + lease>,
         next_attempt_at=NULL
COMMIT

transaction 밖
  SQS SendMessageBatch

짧은 transaction B
  enqueue 성공 -> DELETE
  retryable 실패 -> READY + next_attempt_at
  영구 실패/소진 -> DEAD
  모든 변경은 WHERE status=CLAIMED AND claim_token=?
```

SQS relay가 enqueue 성공 뒤 Outbox 삭제 전에 죽으면 같은 event가 다시 publish될 수 있다. 이 중복은 정상적인 at-least-once 시나리오이며 consumer가 `eventId`
로 제거한다.

- 완료 query 영향 row가 1이 아니면 lease가 다른 worker로 넘어간 stale 결과이므로 폐기한다.
- `claimed_by`는 관측용이고 정확성은 매 claim마다 새로 생성하는 `claim_token`이 보장한다.
- lease 시간은 DB clock으로 계산하고 해당 경로의 외부 client timeout보다 길게 둔다.
- 만료된 `CLAIMED` row 재claim과 늦게 복귀한 이전 worker의 성공/실패를 교차 테스트한다.
- inventory의 MySQL 8.4.5는 `SKIP LOCKED`를 지원하지만 실제 운영 version과 query plan을 배포 전에 확인한다.

### 5.5 SQS 채택 시 Amazon SQS Standard

현재 event는 booking당 “예매 확정 SMS” 한 종류이고, 동일 booking에 서로 다른 순서의 명령을 연속 처리하지 않는다. 엄격한 순서 보장이 필요하지 않으므로 Standard queue를 선택한다.

- Standard queue는 at-least-once이므로 중복 수신을 전제로 한다.
- 순서는 best effort이므로 event handler는 순서에 의존하지 않는다.
- event 하나가 SMS 하나이므로 독립 병렬 처리와 backpressure가 쉽다.
- AWS가 여러 AZ에 메시지 사본을 저장하므로 단일 Docker Redis보다 장애 영역이 작다.

FIFO는 다음 요구가 생길 때만 전환한다.

- 동일 booking에 `확정 → 취소 → 재확정` event를 반드시 순서대로 처리해야 한다.
- message group 단위 직렬화가 business invariant다.

FIFO의 5분 deduplication window만으로 end-to-end exactly-once가 되지는 않는다. consumer가 visibility timeout 안에 끝내지 못하거나 외부 SMS 호출 뒤
죽는 문제는 별도로 남는다.

### 5.6 SQS 채택 시 Spring Cloud AWS 4.0.2와 AWS SDK for Java v2

현재 프로젝트는 Spring Boot `4.0.6`과 Spring Cloud `2025.1.1`을 사용한다. Spring Cloud AWS `4.x`는 이 조합과 Spring Framework 7, AWS SDK
v2를 공식 compatibility matrix에 명시한다.

구현 역할을 나눈다.

- Outbox Relay: auto-configured `SqsAsyncClient.sendMessageBatch()`를 직접 사용해 entry별 성공·실패를 확인한다.
- Consumer: `spring-cloud-aws-starter-sqs`의 `@SqsListener`와 listener container를 사용한다.
- acknowledgement: `MANUAL`을 명시하고 ledger commit 뒤에만 ACK한다.
- Standard Queue의 기본 ACK batching에 기대지 않고 초기에는 `acknowledgementInterval=0`, `acknowledgementThreshold=0`으로 즉시 삭제한다.
- local integration test: LocalStack 또는 Testcontainers 기반 SQS를 사용한다.

현재 `infra`의 S3는 AWS SDK v1을 사용한다. AWS SDK for Java v1은 2025-12-31 support가 종료됐으므로 신규 SQS 코드는 v2로만 작성한다. S3 v1→v2
migration은 별도 변경으로 진행해 이번 기능의 회귀 범위를 키우지 않는다.

## 6. 데이터 모델

### 6.1 SQS 채택 시 Outbox

아래는 논리 schema다. 실제 DDL type과 naming은 현재 DB convention에 맞춘다.

```sql
CREATE TABLE outbox_event
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    event_id        CHAR(36)     NOT NULL,
    event_key       VARCHAR(160) NOT NULL,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    BIGINT       NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    schema_version  INT          NOT NULL,
    payload         JSON         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'READY',
    attempt_count   INT          NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    claim_token     CHAR(36) NULL,
    claimed_by      VARCHAR(100) NULL,
    lease_until     DATETIME(6) NULL,
    last_error_code VARCHAR(100) NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    UNIQUE KEY uk_outbox_event_key (event_key),
    KEY             ix_outbox_claim (status, next_attempt_at, id),
    KEY             ix_outbox_lease (status, lease_until),
    CONSTRAINT ck_outbox_status CHECK (
        status IN ('READY', 'CLAIMED', 'DEAD')
        ),
    CONSTRAINT ck_outbox_next_attempt CHECK (
        (status = 'READY' AND next_attempt_at IS NOT NULL)
            OR (status <> 'READY' AND next_attempt_at IS NULL)
        ),
    CONSTRAINT ck_outbox_claim CHECK (
        (
            status = 'CLAIMED'
                AND claim_token IS NOT NULL
                AND claimed_by IS NOT NULL
                AND lease_until IS NOT NULL
            )
            OR (
            status <> 'CLAIMED'
                AND claim_token IS NULL
                AND claimed_by IS NULL
                AND lease_until IS NULL
            )
        )
);
```

상태는 최소한으로 둔다.

```text
READY ──claim──▶ CLAIMED ──SQS enqueue 성공──▶ DELETE
 ▲                 │
 └──── retryable ──┘

READY/CLAIMED ──non-retryable or retry exhausted──▶ DEAD
```

- `event_key`: `booking-confirmed:{commandId}:{bookingId}`. 같은 요청 재실행의 event 중복을 막되, 향후 합법적인 재확정 요청까지 영구 차단하지 않는다.
- `READY` 생성·재시도 시 `next_attempt_at`을 설정하고 claim 또는 `DEAD` 전이 시 `NULL`로 비운다.
- `CLAIMED`를 벗어나는 모든 전이는 `claim_token`, `claimed_by`, `lease_until`을 함께 비운다.
- 성공 row는 즉시 삭제해 hot table을 작게 유지한다.
- `DEAD` row는 운영자가 원인을 확인한 뒤 재시도 또는 별도 archive한다.
- 삭제된 Outbox의 감사 역할은 `sms_delivery`가 담당한다. Outbox를 장기 감사 테이블로 사용하지 않는다.
- `last_error_code`에는 전화번호나 payload를 저장하지 않는다.
- Outbox 완료는 broker enqueue 성공이다. provider 호출과 delivery 상태는 SQS consumer가 별도로 관리한다.

#### 리디 table 설계에서 반영한 점

리디의 초기 구조는 다음 두 table을 사용했다.

```text
message
  └─ 발행할 event

processed_message
  └─ 발행 완료된 message id
```

relay는 두 table을 `LEFT JOIN`해 아직 처리되지 않은 message를 찾았다. 처리 완료 row 삭제가 늦어지면 anti join 대상이 커지고 조회 latency가 악화됐다. 후속 개선에서는
`processed_message`와 JOIN을 제거하고, 성공한 `message` row를 같은 처리 흐름에서 바로 삭제하는 single hot table 구조로 변경했다.

BEAT는 후속 구조를 기준으로 한다.

- 처리 완료 여부를 별도 `processed_outbox` table에 저장하지 않는다.
- 경로별 완료 조건을 충족한 Outbox row는 즉시 삭제한다.
- 처리 이력은 Outbox가 아니라 `sms_delivery`에 남긴다.
- `status`, `claim_token`, `lease_until`은 다중 worker claim과 crash recovery를 위한 일시 상태다.
- claim query는 `(status, next_attempt_at, id)` index만 사용하며 다른 table과 JOIN하지 않는다.
- `DEAD`가 누적돼 claim index를 오염시키면 별도 archive table로 이동한다.

리디의 최종 구현은 broker publish 중 MySQL transaction과 row lock을 유지했다. BEAT는 SQS network latency가 booking DB connection을 점유하지
않도록 claim transaction과 publish를 분리한다. 그 결과 중복 publish 가능성을 허용하는 대신 consumer의 `eventId` 멱등성을 필수로 둔다.

| 리디 `message`               | BEAT `outbox_event`                                  | 판단                                                                 |
|----------------------------|------------------------------------------------------|--------------------------------------------------------------------|
| `id`                       | `id`, `event_id`                                     | DB paging key와 외부 멱등 key를 분리                                       |
| `topic`                    | 없음                                                   | 현재 SQS queue 하나라 config로 결정; 다중 destination이 생길 때 추가               |
| `type`                     | `event_type`, `schema_version`                       | consumer routing과 schema 진화를 명시                                    |
| `key`                      | `event_key`, `aggregate_id`                          | booking 확정 event 중복 생성을 unique key로 차단                             |
| `payload`                  | `payload`                                            | 개인정보 없이 최소 event contract 저장                                       |
| `source`                   | 없음                                                   | producer가 `apis` 하나라 상수 column을 만들지 않음                             |
| 없음                         | `status`, `claim_token`, `claimed_by`, `lease_until` | network call 밖 DB transaction과 다중 worker crash recovery·fencing 지원 |
| `created_at`, `updated_at` | 동일                                                   | backlog age와 retry 관측                                              |

### 6.2 SMS delivery ledger

```sql
CREATE TABLE sms_delivery
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    notification_id      CHAR(36)     NOT NULL,
    booking_id           BIGINT       NOT NULL,
    provider             VARCHAR(30)  NOT NULL,
    template_code        VARCHAR(100) NOT NULL,
    template_version     INT          NOT NULL,
    encrypted_payload    BLOB NULL,
    encryption_key_id    VARCHAR(255) NOT NULL,
    encryption_algorithm VARCHAR(50)  NOT NULL,
    payload_purged_at    DATETIME(6) NULL,
    provider_group_id    VARCHAR(100) NULL,
    provider_message_id  VARCHAR(100) NULL,
    status               VARCHAR(20)  NOT NULL,
    attempt_count        INT          NOT NULL DEFAULT 0,
    last_error_code      VARCHAR(100) NULL,
    processing_token     CHAR(36) NULL,
    processing_until     DATETIME(6) NULL,
    next_attempt_at      DATETIME(6) NULL,
    next_status_check_at DATETIME(6) NULL,
    status_check_count   INT          NOT NULL DEFAULT 0,
    accepted_at          DATETIME(6) NULL,
    delivered_at         DATETIME(6) NULL,
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sms_delivery_notification (notification_id),
    KEY                  ix_sms_delivery_retry (status, next_attempt_at, id),
    KEY                  ix_sms_delivery_processing (status, processing_until),
    KEY                  ix_sms_delivery_status_check (status, next_status_check_at, id),
    CONSTRAINT ck_sms_delivery_status CHECK (
        status IN (
                   'PENDING',
                   'PROCESSING',
                   'RETRY_WAIT',
                   'ACCEPTED',
                   'DELIVERED',
                   'UNDELIVERABLE',
                   'DELIVERY_UNKNOWN',
                   'FAILED',
                   'ACCEPTANCE_UNKNOWN'
            )
        ),
    CONSTRAINT ck_sms_delivery_payload_lifecycle CHECK (
        (encrypted_payload IS NOT NULL AND payload_purged_at IS NULL)
            OR (encrypted_payload IS NULL AND payload_purged_at IS NOT NULL)
        ),
    CONSTRAINT ck_sms_delivery_retry_payload CHECK (
        status NOT IN ('PENDING', 'PROCESSING', 'RETRY_WAIT')
            OR encrypted_payload IS NOT NULL
        ),
    CONSTRAINT ck_sms_delivery_next_attempt CHECK (
        (status IN ('PENDING', 'RETRY_WAIT') AND next_attempt_at IS NOT NULL)
            OR (status NOT IN ('PENDING', 'RETRY_WAIT') AND next_attempt_at IS NULL)
        ),
    CONSTRAINT ck_sms_delivery_processing CHECK (
        (
            status = 'PROCESSING'
                AND processing_token IS NOT NULL
                AND processing_until IS NOT NULL
            )
            OR (
            status <> 'PROCESSING'
                AND processing_token IS NULL
                AND processing_until IS NULL
            )
        ),
    CONSTRAINT ck_sms_delivery_status_check CHECK (
        status = 'ACCEPTED'
            OR next_status_check_at IS NULL
        )
);
```

```text
PENDING/RETRY_WAIT ──atomic claim──▶ PROCESSING
                                      ├→ ACCEPTED ──delivery report──▶ DELIVERED
                                      │                            └→ UNDELIVERABLE
                                      │                            └→ DELIVERY_UNKNOWN
                                      ├→ RETRY_WAIT
                                      ├→ FAILED
                                      └→ ACCEPTANCE_UNKNOWN

ACCEPTANCE_UNKNOWN ──provider/운영 판정──▶ ACCEPTED/DELIVERED/UNDELIVERABLE
                   ├─payload 보존 중 + 미수락 확정 + 승인──▶ RETRY_WAIT
                   └─payload 파기 후 미수락 확정──▶ 기존 row는 종료 상태 유지
                                                    └─필요 시 새 notification 생성
```

- delivery row와 암호화된 확정 시점 snapshot은 booking과 같은 transaction에서 `PENDING`으로 생성하고 `next_attempt_at`을 현재 DB 시각으로 설정한다. SQS
  경로에서는 Outbox도 이 transaction에 참여한다.
- `PROCESSING`: `processing_token`을 가진 consumer 하나만 provider를 호출할 수 있다. claim할 때 `next_attempt_at`을 `NULL`로 비운다.
- `PROCESSING`을 벗어나는 모든 전이는 `processing_token`, `processing_until`을 함께 비운다.
- `ACCEPTED`: provider가 요청을 수락하고 group/message id를 반환했다. 단말 전달 완료를 뜻하지 않는다.
- `DELIVERED`/`UNDELIVERABLE`: provider 조회 또는 delivery report로 최종 결과를 확인했다.
- `accepted_at`: BEAT가 provider 수락을 확인한 DB 시각이다. `ACCEPTED`로 처음 전이하거나 reconciliation이 곧바로 `DELIVERED`/`UNDELIVERABLE`을
  확정할 때 같은 조건부 update에서 기록하고 이후 변경하지 않는다.
- `delivered_at`: BEAT가 단말 전달을 확인한 DB 시각이다. `DELIVERED`로 전이할 때 같은 조건부 update에서 기록한다. provider 원본 시각이 필요해지면 별도 필드로 보존하며 이
  필드에 혼합하지 않는다.
- 최초 provider 호출 결과는 `processing_token`으로 fence한다. 이후 결과 조회는 token이 이미 제거된 `ACCEPTED` row를 `WHERE status = 'ACCEPTED'`
  조건으로 갱신하고, `ACCEPTANCE_UNKNOWN` 판정도 기대한 현재 상태를 조건으로 갱신한다. 영향 row가 0이면 다른 worker가 먼저 전이한 것으로 보고 결과를 다시 읽으며 terminal 상태를
  덮지 않는다.
- `PENDING`/`RETRY_WAIT`에서만 `next_attempt_at`이 존재한다. 명확한 일시 오류만 다음 재시도 시각을 기록하며, SQS 경로에서는 visibility timeout도 같은
  backoff 정책으로 맞춘다.
- `FAILED`: 잘못된 번호 등 명확한 영구 오류다.
- `ACCEPTANCE_UNKNOWN`: timeout 또는 process crash로 provider가 실제 수락했는지 알 수 없다.
- `DELIVERY_UNKNOWN`: provider 수락은 확인됐지만 최대 조회 기간 안에 단말 전달 결과를 확인하지 못했다.
- `next_status_check_at`은 provider 결과 조회가 가능한 `ACCEPTED`에서만 사용할 수 있다. provider가 최종 결과를 제공하지 않으면 `NULL`로 두고 보장 수준을
  `ACCEPTED`로 제한한다.

`ACCEPTANCE_UNKNOWN`은 자동 재전송하지 않는다. provider group/message 조회 API로 확인하거나 운영자가 수동 판단한다. `DELIVERY_UNKNOWN`은 이미 수락된 요청이므로
재전송 대상이 아니다. 사용자 지정 idempotency key 지원은 현재 확정하지 않는다. Phase 0에서 공식 문서·계약과 staging으로 지원을 확인한 경우에만 실제 SMS 효과의 식별자인
`notificationId`를 provider에 전달하고 자동 재시도를 허용한다. `eventId`는 Outbox/SQS 전달 추적에만 사용한다. 지원이 확인되지 않으면 timeout·응답 단절은 계속
`ACCEPTANCE_UNKNOWN`으로 격리한다.

암호화 payload는 versioned ciphertext envelope로 저장한다. envelope에 nonce/auth tag를 포함하고 `encryption_key_id`,
`encryption_algorithm`으로 rotation 후에도 복호화할 수 있게 한다. `PENDING`, `PROCESSING`, `RETRY_WAIT`에는 payload가 반드시 존재해야 한다. KMS 일시
장애는 provider 호출 전 재시도하고, key 영구 소실·ciphertext 훼손은 `FAILED`와 보안 alert로 격리한다.

payload를 파기해도 `encryption_key_id`와 `encryption_algorithm`은 key material이 아닌 감사 metadata로 보존한다. 계정·region을 포함할 수 있는 운영
정보이므로 공개 응답과 일반 로그에는 노출하지 않고 개인정보·운영 권한이 있는 주체만 조회한다. 실제 암호화 key, nonce/auth tag, 평문 복원 정보는 ciphertext와 함께 제거하며 로그에 남기지
않는다.

상태값 추가·변경은 mixed-version reader까지 포함한 expand-contract migration으로 진행한다.

1. DB `CHECK`에 신규 상태를 먼저 허용한다.
2. 기존 상태와 신규 상태를 모두 읽을 수 있는 reader를 `apis`, `admin`, `batch` 전체 node에 배포한다.
3. 구 reader가 모두 drain됐는지 확인한다. Kotlin/JPA enum 변환이 알 수 없는 문자열에서 실패하지 않는지도 contract test로 검증한다.
4. feature flag로 신규 상태 writer를 활성화한다.
5. 기존 상태를 migration하고 legacy reader/writer를 제거한다.
6. 더 이상 사용하지 않는 상태를 DB `CHECK`에서 제거한다.

`ACCEPTANCE_UNKNOWN` payload는 provider 조회와 운영 판정에 필요한 최대 조사 기간까지만 보존한다. 출시 전에 개인정보 owner·운영 owner가 조사 담당자와 최대 기간을 승인한다.
provider 수락 여부가 판정되거나 최대 기간이 끝나면 payload를 파기하고 `payload_purged_at`을 남긴다. 파기된 snapshot으로는 재발송하지 않으며, 미수락이 뒤늦게 확정돼 재발송이
필요하면 운영자가 수신자 정보를 다시 검증한 새 notification을 감사 로그와 함께 생성한다.

### 6.3 수동 재발행 감사 원장

payload가 파기된 `ACCEPTANCE_UNKNOWN`은 기존 row를 재활성화하지 않는다. 권한 있는 운영자가 수신자와 메시지를 다시 검증한 뒤 새 `sms_delivery`를 만들고 원본과의 관계를
append-only 감사 원장에 기록한다.

```sql
CREATE TABLE notification_reissue_audit
(
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    original_notification_id CHAR(36)    NOT NULL,
    new_notification_id      CHAR(36)    NOT NULL,
    operator_id              BIGINT      NOT NULL,
    command_id               CHAR(36)    NOT NULL,
    reason_code              VARCHAR(50) NOT NULL,
    reason_detail            VARCHAR(500) NULL,
    created_at               DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_reissue_new (new_notification_id),
    UNIQUE KEY uk_notification_reissue_command (operator_id, command_id),
    KEY                      ix_notification_reissue_original (original_notification_id)
);
```

- `reason_detail`에는 전화번호·메시지 본문 등 개인정보를 기록하지 않는다.
- 운영자 식별자는 request body가 아니라 인증 context에서 가져오고, 수동 재발행 권한을 server-side에서 검증한다.
- 같은 운영 명령의 timeout·double click은 `(operator_id, command_id)`로 한 번만 처리한다.
- 같은 원본에 대한 동시 재발행은 원본 `sms_delivery`를 `FOR UPDATE`로 잠근 뒤 활성 successor가 없는지 확인해 직렬화한다. 후속 재발행이 필요하면 새 승인 명령으로 감사 chain을
  추가하므로 `original_notification_id`에는 unique를 두지 않는다.
- 새 `sms_delivery`, 감사 row, SQS 경로의 조건부 Outbox는 하나의 MySQL transaction으로 commit하거나 함께 rollback한다.
- 감사 row는 수정하지 않는다. 정정이 필요하면 별도 감사 row를 추가한다.

### 6.4 요청 멱등성 원장

client가 응답을 받기 전에 연결을 끊으면 DB commit 여부를 알 수 없다. booking 상태만 보고 재시도를 판단하면 SMS event 중복 생성 여부까지 복원할 수 없으므로 요청 멱등성 원장을 둔다.

```sql
CREATE TABLE booking_confirmation_idempotency
(
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    command_id     CHAR(36) NOT NULL,
    actor_id       BIGINT   NOT NULL,
    performance_id BIGINT   NOT NULL,
    request_hash   CHAR(64) NOT NULL,
    expires_at     DATETIME(6)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_command_actor (actor_id, command_id),
    KEY            idx_command_expiry (expires_at)
);
```

- hash 입력은 원본 JSON이 아니라 서버가 실제 실행하는 command로 제한한다. `performanceId + (bookingId, requestedStatus)`를 bookingId 순으로 정렬해
  canonical serialization하고 이름·전화번호·schedule 등 미사용 client field와 JSON property/list 순서는 제외한다.
- command row, booking 변경, `sms_delivery`를 같은 transaction에 기록하고 SQS 경로에서만 Outbox를 추가한다.
- 비트랜잭션 idempotency coordinator가 transaction을 호출하고, transaction 시작 직후 command row를 insert·flush한 다음 booking/delivery와
  조건부 Outbox를 처리한다.
- 동시 unique 충돌을 같은 transaction 안에서 catch하지 않는다. 실패 transaction이 완전히 rollback된 뒤 coordinator가 새 read-only transaction으로
  committed row를 읽어 hash와 결과를 비교한다. 선행 transaction이 rollback됐다면 command 전체를 다시 시작한다.
- 현재 응답은 `SuccessResponse<Void>`이므로 결과 payload를 저장하지 않는다. 응답을 잃은 동일 요청은 committed row를 근거로 기존 `200 OK`를 반환하며 delivery와
  조건부 Outbox를 다시 만들지 않는다.
- 별도 상태 column은 두지 않는다. 같은 transaction이 실패하면 row도 rollback되므로 committed row 자체가 성공을 뜻한다.
- 항목별 결과가 필요한 API v2를 도입할 때만 versioned result snapshot 또는 별도 결과 모델을 ADR로 추가한다.
- 만료 기간과 삭제 batch 크기는 운영 재시도 기간·감사 요구를 확인한 뒤 정한다.

## 7. Event contract와 개인정보

SQS payload는 가능하면 개인정보 없이 식별자와 event metadata만 전달한다.

```json
{
  "eventId": "0198...",
  "eventType": "BOOKING_PAYMENT_CONFIRMED",
  "schemaVersion": 1,
  "occurredAt": "2026-07-29T12:34:56.123Z",
  "notificationId": "0198...",
  "bookingId": 101
}
```

consumer는 `notificationId`로 booking 확정 transaction에서 저장한 `sms_delivery`와 암호화 snapshot을 조회한다. `eventId`는 Outbox/SQS 전달
멱등성에만 사용하고 delivery 멱등성은 `notificationId`가 담당한다. 이름·전화번호·공연명·template version은 확정 시점 값을 사용한다.

장점:

- 전화번호·이름이 SQS, DLQ, trace, redrive 화면에 복제되지 않는다.
- 현재 client DTO가 보낸 값을 신뢰하지 않는다.
- message size와 schema 변경 범위가 작다.
- 발송이 지연되는 동안 booking이나 공연 정보가 수정·삭제돼도 확정 시점의 알림 의미를 보존한다.

단점:

- SMS worker가 DB와 snapshot 암복호화 key에 의존한다.
- DB 또는 KMS가 장애면 SMS가 지연된다.

현재는 하나의 modular monolith와 공용 DB를 사용하는 구조이므로 이 trade-off를 수용한다. 암호화 key 접근은 consumer role로 제한하고 snapshot retention 만료 시
payload를 파기한다.

보안 원칙:

- SQS encryption at rest를 활성화한다.
- 애플리케이션에는 instance/task role을 사용하고 static access key를 배포하지 않는다.
- queue URL을 configuration에 직접 주입해 `GetQueueUrl`은 사용하지 않는다. relay role은 source queue의 `sqs:SendMessage`, consumer role은
  `ReceiveMessage/DeleteMessage/ChangeMessageVisibility/GetQueueAttributes`만 허용하고 staging CloudTrail로 실제 호출 action을
  검증한다.
- 전화번호, 이름, SMS 본문을 application log와 error field에 남기지 않는다.
- DLQ redrive는 source/DLQ의 `StartMessageMoveTask/ListMessageMoveTasks/CancelMessageMoveTask`와 필요한 receive/send 권한을 가진 운영
  role로 제한하고 `RedriveAllowPolicy`로 허용 source queue를 고정한다.

## 8. SQS 채택 시 초기 설정

다음은 load test 전 초기값이다.

| 항목                   |                   초기값 | 이유                                                 |
|----------------------|----------------------:|----------------------------------------------------|
| Queue type           |              Standard | booking별 독립 event, 순서 불필요                          |
| Long polling         |                   20초 | 빈 polling 비용 감소                                    |
| Visibility timeout   |              120초(가설) | provider client timeout·처리 lease보다 길게 시작하고 p99로 조정 |
| Source retention     |                    4일 | 장애 복구 여유                                           |
| DLQ retention        |                   14일 | source보다 길게 보존                                     |
| `maxReceiveCount`    |                     5 | transient retry 후 poison message 격리                |
| Encryption           | SSE-SQS, 필요 시 SSE-KMS | 저장 데이터 암호화                                         |
| Relay batch          |        최대 10건/request | SQS API 제한                                         |
| Consumer concurrency |                2부터 시작 | SOLAPI 계약 제한 확인 전 보수적 시작                           |

`SendMessageBatch`는 HTTP `200`이어도 일부 entry가 실패할 수 있다. relay는 `Successful[]`, `Failed[]`를 각각 처리하고 성공 row만 삭제한다. 한 요청에는 최대
10개 message만 넣는다. 78개 event라면 최소 8개 batch request가 필요하다.

listener는 ack mode를 `MANUAL`로 명시하고 delivery ledger commit 뒤에만 ACK한다. 초기에는 `acknowledgementInterval=0`,
`acknowledgementThreshold=0`으로 즉시 삭제하며, batching은 측정 후 별도 선택한다. `queueNotFoundStrategy=FAIL`, long polling 20초,
`maxConcurrentMessages=2`, 직접 주입한 queue URL을 configuration에 고정한다. SSE-KMS를 선택하면 producer/consumer role과 key policy의 KMS
권한도 별도로 검증한다.

초기 구현은 무제한 heartbeat 대신 `provider connect+read hard timeout < processing lease < SQS visibility timeout`을 강제한다. claim 시
두 deadline을 같은 DB 기준 시각으로 계산하고 provider call이 processing lease 안에 끝나도록 client hard timeout을 둔다. 예상 밖 장기 작업이 필요해지면
`processing_token` 조건으로 `processing_until`과 SQS visibility를 함께 heartbeat하는 기능을 별도 추가한다. 둘 중 하나만 연장되면 안전하게 처리를 중단하고
`ACCEPTANCE_UNKNOWN`으로 격리한다.

명시적으로 재시도 가능한 실패는 exponential backoff + jitter에 따라 visibility를 늦춘다. SQS receive 횟수만 retry 시계로 사용하지 않고
`sms_delivery.next_attempt_at`과 함께 관측한다.

단일 consumer instance에서는 provider 계약량에 맞춘 local rate limiter와 listener concurrency를 함께 둔다. provider 전체 장애 때 메시지를 빠르게 소진해
DLQ로 몰아넣지 않도록 circuit open 동안 receive를 줄이거나 visibility를 늘린다. 다중 instance로 확장할 때는 instance별 quota를 합산해 provider 계정 한도를 넘지
않게 재설계한다. 이를 위해 현재 단일 Redis에 분산 rate limiter를 추가하지 않는다.

## 9. SQS 채택 시 소비자 멱등성과 재시도

### 9.1 정상 처리

```text
1. SQS message schema 검증
2. `PENDING` 또는 due인 `RETRY_WAIT`만 조건부 UPDATE하여 processing_token claim
3. ACCEPTED/DELIVERED/UNDELIVERABLE/DELIVERY_UNKNOWN/FAILED/ACCEPTANCE_UNKNOWN이면 외부 호출 없이 ACK
4. 유효한 다른 `PROCESSING` claim 또는 아직 due가 아닌 `RETRY_WAIT`이면 visibility를 조정하고 종료
5. 암호화 snapshot을 복호화하고 template version으로 요청 생성
6. transaction 밖에서 SOLAPI 호출
7. processing_token이 일치할 때만 결과를 ACCEPTED/RETRY_WAIT/FAILED/ACCEPTANCE_UNKNOWN으로 반영
8. 결과 transaction commit
9. terminal 또는 ACCEPTED면 SQS ACK
```

조회 후 상태 변경 방식은 두 consumer가 동시에 외부 API를 호출할 수 있는 TOCTOU가 생기므로 사용하지 않는다. claim은 단일 conditional update 또는 insert conflict로
원자화하고 매 시도마다 새 `processing_token`을 발급한다. 늦게 돌아온 이전 worker의 결과는 token 불일치로 폐기한다.

### 9.2 오류 분류

| 상황                                                                                                   | 처리                                                  |
|------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| DB 조회 timeout                                                                                        | 예외 발생, ACK하지 않음, visibility 후 재수신                   |
| provider가 수락 전 실패임을 명시한 429/5xx                                                                      | `RETRY_WAIT`, backoff 후 재수신                         |
| 잘못된 전화번호·템플릿                                                                                         | `FAILED` 기록 후 ACK, 운영 알림                            |
| provider timeout·응답 단절                                                                               | `ACCEPTANCE_UNKNOWN` 기록 후 ACK, provider 조회 또는 수동 확인 |
| 만료된 `PROCESSING`                                                                                     | 호출 여부를 증명할 수 없으면 `ACCEPTANCE_UNKNOWN`, 자동 재전송 금지    |
| schema version 미지원                                                                                   | DLQ로 보내기 위해 실패 처리, 즉시 알림                            |
| delivery `ACCEPTED`/`DELIVERED`/`UNDELIVERABLE`/`DELIVERY_UNKNOWN`/`FAILED`/`ACCEPTANCE_UNKNOWN` 재수신 | 외부 호출 없이 ACK                                        |

외부 SMS 호출에는 일반 DB transaction을 길게 유지하지 않는다. provider network I/O 동안 DB connection과 row lock을 잡으면 consumer 확장성과 장애 격리가
나빠진다.

### 9.3 정확히 한 번에 대한 한계

다음 순간에 process가 죽을 수 있다.

```text
SOLAPI가 메시지를 수락함
        │
        X process crash
        │
sms_delivery ACCEPTED 기록 전
```

provider idempotency 또는 조회 기능이 없으면 “발송됨”과 “발송되지 않음”을 시스템이 구분할 수 없다. 이 구간의 exactly-once SMS는 SQS FIFO, DB lock, Outbox만으로
보장할 수 없다.

따라서 보장 수준은 다음과 같이 표현한다.

- booking과 SMS 작업 생성: atomic
- SQS 전달: at-least-once
- consumer 내부 SMS 작업: notificationId와 processing token 기준 idempotent
- Outbox/SQS 전달 추적: eventId 기준 deduplication
- 외부 SMS 최종 효과: provider 기능이 없으면 ambiguous outcome 존재

## 10. 모듈 책임

새 Gradle 모듈을 바로 추가하지 않는다.

```text
domain
  └─ booking 상태 전이 규칙

module-contracts
  ├─ booking/
  │    ├─ BookingConfirmationIdempotencyPort
  │    └─ BookingConfirmationIdempotencyRecord
  ├─ outbox/                                    # SQS ADR 채택 시
  │    ├─ OutboxEventRecordPort
  │    ├─ OutboxEventClaimPort
  │    ├─ OutboxEventPublisherPort              # SQS ADR 채택 시
  │    ├─ OutboxEventRecord
  │    └─ OutboxEventClaim
  └─ sms/
       ├─ SmsPort
       ├─ SmsMessage
       ├─ SmsSendResult
       ├─ SmsDeliveryRecordPort
       ├─ SmsDeliveryStorePort
       ├─ NotificationReissueAuditPort
       ├─ NotificationReissueAuditRecord
       ├─ SmsDeliveryStatus
       └─ BookingConfirmationSmsRequested       # SQS ADR 채택 시

apis
  └─ ticket/
       ├─ facade/TicketFacade
       └─ application/command/
            ├─ TicketUpdateCommand
            ├─ TicketConfirmationIdempotencyCoordinator
            └─ TicketCommandService
                 └─ idempotency/booking/delivery와 조건부 Outbox 직접 기록

infra
  ├─ persistence/
  │    ├─ booking/
  │    │    ├─ entity/BookingConfirmationIdempotencyJpaEntity
  │    │    └─ repository/
  │    │         ├─ BookingConfirmationIdempotencyJpaRepository
  │    │         └─ BookingConfirmationIdempotencyAdapter
  │    ├─ outbox/                                # SQS ADR 채택 시
  │    │    ├─ entity/OutboxEventJpaEntity
  │    │    └─ repository/
  │    │         ├─ OutboxEventJpaRepository
  │    │         ├─ OutboxEventRecordAdapter
  │    │         └─ OutboxEventClaimAdapter
  │    └─ notification/
  │         ├─ entity/
  │         │    ├─ SmsDeliveryJpaEntity
  │         │    └─ NotificationReissueAuditJpaEntity
  │         └─ repository/
  │              ├─ SmsDeliveryJpaRepository
  │              ├─ SmsDeliveryRecordAdapter
  │              ├─ SmsDeliveryStoreAdapter
  │              ├─ NotificationReissueAuditJpaRepository
  │              └─ NotificationReissueAuditAdapter
  └─ external/
       ├─ messaging/sqs/                         # SQS ADR 채택 시
       │    ├─ SqsEventPublisherAdapter
       │    ├─ SqsMessagingConfig
       │    └─ SqsProperties
       └─ notification/sms/solapi/
            ├─ SolapiSmsAdapter
            ├─ SolapiSmsConfig
            ├─ SolapiProperties
            └─ SolapiExceptionMapper

batch
  ├─ outbox/                                      # SQS ADR 채택 시
  │    ├─ job/OutboxRelayJob
  │    ├─ facade/OutboxRelayFacade
  │    └─ application/OutboxRelayService
  └─ notification/
       ├─ listener/BookingConfirmationSmsListener # SQS ADR 채택 시
       ├─ job/BookingConfirmationSmsJob           # DB Job Queue ADR 채택 시
       ├─ facade/SmsDeliveryFacade
       ├─ application/SmsDeliveryService
       └─ job/SmsDeliveryReconciliationJob

admin
  └─ 2차: notification/
       ├─ facade/NotificationReissueFacade
       └─ application/NotificationReissueService
```

원칙:

- `command` 패키지는 실행 모듈의 `application/command`에만 사용한다. `module-contracts`와 `infra`의 최상위 패키지는 공유 capability 또는 business
  context를 따른다.
- `apis`는 SQS SDK와 SOLAPI를 모른다.
- `batch`는 JPA/SQS/SOLAPI 구현 class가 아니라 contract에 의존한다.
- `infra`는 JPA, SQS, SOLAPI adapter와 기술 설정만 소유한다. relay·retry·상태 전이 같은 application orchestration은 소유하지 않는다.
- 수동 재발행의 권한 확인·원본 lock·수신자 재검증·새 작업 조율은 `admin` application이 소유하고, `infra`는 감사 원장과 delivery 저장 Port만 구현한다.
- idempotency/delivery 저장은 `apis` application service가 항상 요청한다. SQS 경로에서만 Outbox 저장을 추가하며 infra adapter가 각 port를 구현한다.
- SQS를 채택하면 `OutboxRelayService`는 `batch`가 소유하고 `OutboxEventClaimPort`와 `OutboxEventPublisherPort`에 의존한다.
  `OutboxRelayAdapter`는 만들지 않는다.
- DB Job Queue를 채택하면 `BookingConfirmationSmsJob`이 `sms_delivery`를 직접 claim해 같은 `SmsDeliveryService`를 호출한다. Outbox/SQS
  port·relay·listener는 만들지 않는다.
- `Job`과 `Listener`는 Facade만 호출하고 application, domain, infra, module-contracts를 직접 참조하지 않는다.
- Facade는 실행 진입점의 입력 변환·관측·여러 application 호출 조합을 담당한다. 단순 위임만 남는다면 별도 계층으로 만들지 않는다.
- 기존 `batch → infra` 의존성을 재사용한다.
- 실행 모듈의 `InfraConfig`만 infra 공개 bootstrap config를 import한다. application/facade/job/listener 코드에서 `infra.*` 구현을 직접
  import하지 않는다.

현재 `ExternalClientConfig`는 `CoolSmsAdapter`를 apis runtime에 함께 등록한다. migration 이후에는 기존 SMS adapter를 broad
`EXTERNAL_CLIENTS` scan에서 제거하고, batch의 composition root가 `SqsMessagingConfig`와 `SolapiSmsConfig`를 명시적으로 선택한다. SQS가 ADR에서
탈락하면 SQS port/package/config를 생성하지 않는다.

SQS consumer는 엄밀히 말하면 정기 batch job이 아니라 상시 worker다. 지금은 모듈 수와 배포 복잡도를 늘리지 않기 위해 `batch`에 둔다. 다음 조건 중 하나가 실제로 생기면
`notification-worker` 실행 모듈을 분리한다.

- SMS만 독립 배포·autoscaling해야 한다.
- batch 정기 작업과 consumer의 CPU/memory/SLO가 충돌한다.
- 두 번째 notification consumer가 추가된다.
- 장애 격리를 위해 별도 release lifecycle이 필요하다.

## 11. DB 접근 개선

메시징 전환만으로 request thread의 SMS I/O는 제거되지만, booking별 lock query는 남는다. 같은 기능 변경에서 측정 가능한 범위로 개선한다.

현재:

```text
findAllById(78)
lockById(1) × 78
save(1) × 78
```

목표:

```text
lockAllByIdOrderById(78)  1 query
상태 전이 분류
saveAll(changedBookings)
smsDelivery saveAll(deliveries)
SQS 경로에서만 outbox saveAll(events)
```

- booking id를 정렬해 일관된 lock 순서를 유지한다.
- schedule도 distinct id를 정렬해 일괄 lock한다.
- `hibernate.jdbc.batch_size`는 실제 SQL과 driver rewrite 동작을 확인한 뒤 설정한다.
- query count와 transaction time을 integration test 또는 datasource proxy로 검증한다.
- DB query 최적화와 SMS 비동기화를 trace에서 별도 span으로 계측한다.

## 12. 장애 시나리오

| 장애 지점                                     | 결과                                               | 자동 복구                                   |
|-------------------------------------------|--------------------------------------------------|-----------------------------------------|
| booking update 전 DB 장애                    | 전체 rollback, API 5xx                             | client command 재시도                      |
| SQS 경로의 Outbox insert 실패                  | booking 포함 전체 rollback                           | client command 재시도                      |
| commit 응답 전 connection 종료                 | commit 여부 불명확                                    | idempotent command/result 조회 필요         |
| SQS 경로의 broker 전체 장애                      | booking commit, Outbox READY 유지                  | relay backoff 후 재시도                     |
| SQS 경로의 batch 일부 실패                       | 성공 row만 삭제                                       | 실패 entry만 재시도                           |
| SQS enqueue 성공 후 Outbox 삭제 전 relay 종료     | event 중복 publish 가능                              | consumer eventId dedup                  |
| consumer가 provider 호출 전후 불명확한 지점에서 종료     | delivery `ACCEPTANCE_UNKNOWN` 격리                 | provider 조회/수동 판단                       |
| SMS 명시적 일시 장애                             | 작업 재처리                                           | retry 소진 후 delivery `FAILED` 또는 SQS DLQ |
| SMS 명시적 영구 장애                             | delivery FAILED                                  | 운영 수정 후 수동 재처리                          |
| SMS 응답 timeout                            | delivery `ACCEPTANCE_UNKNOWN`                    | provider 조회/수동 판단                       |
| SQS 경로에서 SMS `ACCEPTED` commit 후 ACK 전 종료 | message 재수신                                      | delivery 상태 확인 후 skip                   |
| SQS relay lease 만료 뒤 이전 worker 결과 도착      | stale 결과 폐기                                      | claim token 조건부 반영                      |
| consumer claim 만료 뒤 이전 worker 결과 도착       | stale 결과 폐기                                      | processing token 조건부 반영                 |
| 단일 host 또는 batch process 장애               | DB 경로는 delivery, SQS 경로는 Outbox/SQS에 보존되지만 발송 지연 | host 복구 후 drain; 현재는 무중단 HA 아님          |
| SQS 경로의 DB와 broker 장기 복합 장애               | booking API도 실패하거나 Outbox backlog 증가             | DB 복구, relay drain, 운영 알림               |

DB가 source of truth이므로 DB 장애 중 booking command를 SQS에 먼저 적재하지 않는다. 사용자에게 성공처럼 보이는 “접수” 상태를 새로 만들면 payment confirmation의
일관성 모델이 바뀐다.

## 13. 관측성과 운영

### 13.1 필수 metric

```text
API
- ticket.confirmation.request.count
- ticket.confirmation.item.count{result}
- ticket.confirmation.transaction.duration
- ticket.confirmation.command.replay.count{result}

SQS 채택 시 Outbox
- outbox.pending.count
- outbox.oldest.age
- outbox.publish.count{result}
- outbox.publish.duration
- outbox.dead.count
- outbox.claim.stale-result.count

SQS 채택 시 CloudWatch
- ApproximateNumberOfMessagesVisible
- ApproximateAgeOfOldestMessage
- NumberOfMessagesSent
- NumberOfMessagesReceived
- NumberOfMessagesDeleted
- DLQ ApproximateNumberOfMessagesVisible

SMS
- sms.delivery.count{status,provider}
- sms.provider.duration
- sms.accepted.duration
- sms.delivered.duration
- sms.delivery-status-check.count{result}
- sms.acceptance-unknown.count
- sms.delivery-unknown.count
- sms.retry.count
- sms.processing.expired.count
- sms.claim.stale-result.count
```

### 13.2 초기 SLO와 alert 제안

측정 전 목표이며 운영 합의 후 확정한다.

| 항목                                | 목표                                      |
|-----------------------------------|-----------------------------------------|
| 100건 booking 확정 API p95           | 1초 이하                                   |
| 요청 trace의 SMS 외부 호출               | 0건                                      |
| SQS 채택 시 Outbox → SQS publish p99 | 5초 이하                                   |
| provider 요청 수락 p95                | 60초 이하                                  |
| 단말 전달 확인 p95                      | provider delivery report SLO 확인 후 별도 확정 |
| booking 확정 대비 delivery 불일치        | 0건                                      |
| SQS 채택 시 delivery/Outbox 불일치      | 0건                                      |
| SQS 채택 시 DLQ message              | 1건부터 경고                                 |
| `ACCEPTANCE_UNKNOWN` delivery     | 1건부터 경고                                 |
| `DELIVERY_UNKNOWN` delivery       | 1건부터 경고                                 |
| SQS 채택 시 Outbox oldest age        | 60초 초과 경고                               |

로그에는 `notificationId`, `bookingId`, `attempt`, `errorCode`, `traceId`와 SQS 경로의 `eventId`만 남긴다. 전화번호, 이름, SMS 본문,
provider credential은 남기지 않는다.

### 13.3 Reconciliation

별도 reconciliation job이 다음 불변식을 검사한다.

```text
BOOKING_CONFIRMED인데 sms_delivery가 없음
SQS 경로에서 Outbox CLAIMED인데 lease_until 만료
sms_delivery PROCESSING인데 processing_until 만료
SQS 채택 시 DLQ message와 delivery 상태 불일치
```

자동 보정은 명확한 경우에만 한다. `ACCEPTANCE_UNKNOWN` SMS는 중복 발송 위험 때문에 자동 재전송하지 않고, `DELIVERY_UNKNOWN`은 이미 수락된 요청이므로 재전송하지 않는다.

### 13.4 보존·정리와 운영 절차

- `outbox_event`: 경로별 완료 조건을 충족한 row는 조건부 삭제하고 `DEAD`는 운영 조사 기간보다 길게 보존한다.
- `sms_delivery`: 상태·provider 식별자는 감사/CS 정책에 맞춰 보존하되 암호화 snapshot은 더 짧은 기간 뒤 파기한다.
- `notification_reissue_audit`: 운영 감사 정책에 따라 append-only로 보존하고, 접근 권한과 조회 이력을 관리한다.
- `booking_confirmation_idempotency`: client 재시도와 감사에 필요한 기간만 보존한다.
- 삭제는 작은 batch로 실행하고 oldest age, lock wait, replica lag를 보고 크기를 조정한다.
- 보존 기간은 개인정보·법무·운영 owner가 승인하며 문서의 임의 숫자로 확정하지 않는다.

worker 활성화 전 최소 runbook을 준비한다. `FAILED`, `ACCEPTANCE_UNKNOWN`, `DELIVERY_UNKNOWN`, SQS 경로의 DLQ, backlog, provider 장애별
조회·중지·재개·수동 판정 절차와 담당자를 정의해야 한다. admin 화면은 후속이어도 runbook과 alert는 출시 gate다.

`ACCEPTED` row는 `next_status_check_at` 기준으로 provider 결과를 조회해 `DELIVERED`/`UNDELIVERABLE`로 전이하고, backoff와 최대 조회 기간을 둔다.
조회 기간이 끝나도 최종 결과가 없으면 `DELIVERY_UNKNOWN`으로 보내 운영자가 판정한다. `ACCEPTANCE_UNKNOWN` resolver와 이 조회 job은 “단말 전달”을 보장한다고 표현하기 위한
출시 gate이며, provider가 최종 delivery report를 제공하지 않으면 보장 수준을 `ACCEPTED`로 제한한다.

## 14. 배포와 rollback

blue-green deployment에서 legacy SMS와 신규 writer가 같은 booking을 중복 처리하거나 둘 다 빠뜨리지 않게 순서를 고정한다.

1. Phase 0 ADR에서 `DB Job Queue` 또는 `SQS`를 승인한다. SQS를 선택한 경우에만 SQS, DLQ, redrive policy, IAM/KMS, dashboard, alarm을 IaC로
   만든다.
2. production `ddl-auto=none`에 맞는 migration 도구·owner로 idempotency/delivery schema를 적용한다. SQS 경로에서만 Outbox schema를 추가하고
   rollback 대신 forward-fix 절차를 검증한다.
3. 공식 SOLAPI SDK adapter의 timeout, typed result/error, group/message 조회 contract test를 통과시킨다.
4. blue는 `legacy=on/job-writer=off`를 유지하고, green은 `legacy=off/job-writer=on`, relay/worker off로 띄운다. API role은 legacy와
   job writer가 정확히 하나만 켜지는 XOR startup guard를 둔다.
5. green health와 DB write contract를 확인한 뒤 트래픽을 원자적으로 전환한다. 이 전환이 불가능한 배포 환경에서는 구·신 API 혼재 중 재시도 중복 위험을 해소하기 전까지
   migration을 시작하지 않는다.
6. 모든 API traffic이 green인지 확인하고 blue in-flight 요청을 drain한다.
7. runbook과 alert 준비를 확인한 뒤 선택한 worker를 켠다.
    - SQS 경로: polling relay를 먼저 켜 Outbox를 SQS로 drain한 뒤 SQS listener를 활성화한다.
    - DB Job Queue 경로: delivery를 직접 claim하는 polling worker를 활성화하며 Outbox schema, SQS IaC와 listener는 배포하지 않는다.
8. 공통 상태 전이와 stale claim alert를 확인한다. SQS 경로에서는 partial batch failure·DLQ·redrive도 추가로 확인한다.
9. 안정화 뒤 기존 async SMS listener와 공용 SMS async 의존을 제거한다.

rollback은 결함 위치별로 나눈다.

| 결함                        | 조치                                | backlog 책임                                                               |
|---------------------------|-----------------------------------|--------------------------------------------------------------------------|
| API/job writer            | blue legacy API로 트래픽 전환, green 중지 | SQS 경로는 relay가 Outbox를, DB Job Queue 경로는 polling worker가 delivery를 drain |
| SQS relay                 | relay만 중지·rollback                | SQS 채택 시 Outbox가 DB에 누적되며 oldest age 감시                                  |
| SMS worker/SOLAPI adapter | worker만 중지·rollback               | SQS 경로는 SQS, DB Job Queue 경로는 DB에 누적                                     |
| schema/data               | write 전환 중단, forward-fix          | schema 즉시 drop 금지, DBA runbook 수행                                        |

구버전 API는 신규 멱등성 원장을 만들지 않으므로 API rollback 기간에는 내구성 보장이 낮아짐을 운영에 명시한다. SQS 경로에서는 Outbox drain과 SQS drain을 구분하고 각 flag가
독립적으로 멈추는지 배포 테스트로 검증한다.

기능 flag:

```text
beat.notification.sms-job-writer.enabled
beat.outbox.relay.enabled                      # SQS 채택 시
beat.notification.sms-consumer.enabled         # SQS 채택 시
beat.notification.sms-job-worker.enabled       # DB Job Queue 채택 시
beat.notification.legacy-sms.enabled
```

flag의 기본값은 실행 모듈별로 명시한다. API role에서 job writer와 legacy SMS가 둘 다 켜지거나 둘 다 꺼지면 application start를 실패시킨다. SQS listener와 DB
polling worker도 정확히 하나만 활성화되게 startup guard를 둔다. component scan에 우연히 기대지 않고 `apis`는 job writer configuration을, `batch`는
ADR 결과에 맞는 worker configuration만 명시적으로 import한다.

## 15. 테스트 전략

### 15.1 Domain/Application

- 1/10/78/100/200건 확정
- 중복 bookingId 전체 거절
- 다른 공연 booking 혼입 전체 rollback
- 이미 확정된 booking idempotent success
- 취소·환불 booking 포함 시 전체 rollback
- 유효 항목 DB 오류 시 전체 rollback
- idempotency/delivery insert 오류 시 booking rollback
- SQS 경로의 Outbox insert 오류 시 booking/delivery rollback
- transaction 밖 recorder 호출 실패와 `REQUIRES_NEW` 미사용 검증
- booking 변경 수와 delivery 수 일치
- SQS 경로에서 booking/delivery/Outbox 수 일치와 동일 event key 중복 방지
- 동일 commandId·동일 hash 재요청 시 기존 `200 OK` 반환과 신규 delivery 0건
- 동일 commandId·다른 hash 재요청 시 `409`
- JSON property/list 순서와 미사용 field만 다른 동치 command의 hash 일치
- 동시 동일 commandId 요청에서 선행 commit 대기·commit·rollback과 동일/다른 hash를 조합해 idempotency/delivery 한 세트만 생성하고, SQS 경로에서는 Outbox도
  한 세트만 생성
- 멱등성 원장 만료·삭제 후 동일 command를 다시 요청해도 이미 확정된 booking의 비즈니스 상태 검증으로 신규 `sms_delivery`와 조건부 Outbox를 생성하지 않음
- commit 응답 유실 뒤 재요청에서 신규 SMS event 생성 안 함
- 요청 건수·body 상한 초과 거절
- 확정 뒤 booking 연락처·공연명이 바뀌어도 snapshot이 확정 시점 값을 유지
- 허용되지 않은 delivery status와 상태에 맞지 않는 `next_attempt_at`·processing field 저장을 DB가 거절
- `ACCEPTANCE_UNKNOWN`에서 payload 보존 중 승인된 재시도와 payload 파기 후 새 notification 생성 경로를 각각 검증
- payload 파기 뒤 ciphertext는 없고 `encryption_key_id`·`encryption_algorithm` 감사 metadata만 유지
- 같은 수동 재발행 command의 timeout·double click에서 새 delivery·감사 row·조건부 Outbox가 한 세트만 생성
- 새 delivery·감사 row·조건부 Outbox 중 하나라도 실패하면 모두 rollback
- 같은 원본의 동시 수동 재발행은 한 건만 활성 successor를 만들고 원본→신규 notification 관계를 조회 가능
- payload가 파기된 원본 row는 `RETRY_WAIT`으로 되돌아가지 않음

### 15.2 SQS 채택 시 Outbox Relay

- 허용되지 않은 Outbox status와 상태에 맞지 않는 due/claim field 저장을 DB가 거절
- claim 경쟁 중 동일 row를 한 relay만 소유
- lease 만료 event 재획득
- lease 만료 뒤 이전 relay의 성공/실패 결과가 새 claim을 덮지 않음
- 78건을 10개 이하 entry로 분할
- HTTP 200 + partial failure에서 성공 row만 삭제
- SQS timeout에서 token 조건부 READY 복구와 backoff
- SQS 성공 후 DB 반영 전 process kill에서 중복 publish
- DEAD 전환과 수동 requeue

### 15.3 SMS Worker 공통·분기

공통:

- 동일 notification 작업 2회 처리 시 SMS 최대 1회 시도
- 동시 동일 notification 처리에서 conditional claim 한 건만 성공
- `ACCEPTED`/terminal event 재수신 skip
- 늦은 worker 결과가 새 processing token을 덮지 않음
- 공통 provider hard timeout·processing lease와 reconciliation 경쟁
- 만료된 `PROCESSING`은 provider 확인 없이 자동 재전송하지 않고 `ACCEPTANCE_UNKNOWN`
- 명시적 수락 전 429/5xx만 backoff 후 retry
- invalid phone `FAILED`
- provider timeout `ACCEPTANCE_UNKNOWN`
- provider 수락은 `ACCEPTED`, 결과 조회는 `DELIVERED`/`UNDELIVERABLE`
- 최초 `PROCESSING → ACCEPTED`와 `accepted_at`은 processing token으로, 후속 `ACCEPTED → DELIVERED`와 `delivered_at`은
  expected-status 조건부 update로 기록
- reconciliation이 `ACCEPTANCE_UNKNOWN`에서 곧바로 최종 상태를 확정하면 expected-status 조건으로 `accepted_at`과 필요한 `delivered_at`을 함께 기록
- 동시 결과 조회에서 한 worker만 terminal 전이에 성공하고 늦은 결과가 terminal 상태를 덮지 않음
- `ACCEPTED` 조회 backoff·종료와 `DELIVERY_UNKNOWN` 판정 전이
- provider circuit open 동안 retry 폭주와 delivery `FAILED`/SQS DLQ 집중이 발생하지 않음

SQS 채택 시:

- `provider hard timeout < processing lease < visibility timeout` 경계
- unsupported schema DLQ
- `ACCEPTED` commit 후 ACK 실패 재수신
- graceful shutdown 중 in-flight 처리

DB Job Queue 채택 시:

- polling worker 경쟁에서 동일 delivery를 한 worker만 claim
- 처리 중 process kill 뒤 lease 만료·재획득
- provider hard timeout이 processing lease보다 짧음
- provider 호출 뒤 delivery 결과 commit 전 process kill에서 `ACCEPTANCE_UNKNOWN` 격리

### 15.4 Integration/Load/Chaos

- 공통 MySQL Testcontainers로 API commit부터 worker 최종 상태까지 검증
- migration을 빈 DB와 직전 production schema 양쪽에 적용
- 상태 추가 시 DB CHECK 확장 → 구·신 상태 호환 reader 전체 배포·구 reader drain → 신규 writer 활성화 → legacy 제거 → DB CHECK 축소 순서를
  mixed-version blue-green test로 검증
- 100건 API p95와 SQL query count 측정
- SOLAPI stub의 latency/timeout/429/500/accepted response 주입
- 로그·trace·저장 payload의 개인정보 노출 검사
- blue-green 전환 중 요청과 API/worker별 rollback을 검증한다.
- SQS 채택 시 MySQL + LocalStack/Testcontainers로 commit부터 ACK까지, queue/DLQ redrive policy, SQS 차단 후 Outbox 누적·복구 drain,
  consumer 강제 종료 후 재수신을 검증한다.
- SQS 채택 시 실제 AWS staging에서 IAM/KMS, queue URL/attributes, visibility, redrive를 검증한다. LocalStack 결과만으로 AWS 권한과 운영 동작을
  승인하지 않는다.
- DB Job Queue 채택 시 DB 장애·lock 경쟁·lease 만료·backlog drain을 검증한다.
- 로컬 MySQL/Testcontainers probe는 SQL, lock, fencing과 부하 harness의 기능 검증에만 사용하며 DB Job Queue/SQS 선정 근거로 사용하지 않는다.
- 성능 ADR은 운영과 같은 MySQL major/minor, instance class, storage, parameter group을 가진 격리된 staging에서 실제 `apis`와 `batch` container를 띄우고
  k6로 HTTP API를 호출해 작성한다. production DB에는 합성 부하를 가하지 않는다.
- staging에서는 Worker OFF baseline과 Worker 1/2 및 backlog 0/100/10,000을 비교한다. Worker 4는 Pool 확장이 선행되고 DB 전체 connection budget에
  포함된 경우에만 경계 탐색용으로 실행한다.
- 애플리케이션에서는 API p95/p99, Hikari active/idle/pending/acquire time/usage time을, MySQL에서는
  `Threads_connected`, `Threads_running`, connection error, lock wait, deadlock, CPU, IOPS, buffer pool hit ratio를 같은 시간축으로 수집한다.
- 각 시나리오는 warm-up 뒤 최소 3회 반복하고 중앙값과 worst run을 함께 ADR에 기록한다.

## 16. 단계별 구현

### Phase 0 — 측정

- 현재 1/10/78/100건 SQL count와 latency 기록
- SOLAPI latency, rate limit, timeout, idempotency·결과 조회 기능 확인
- dev/prod 실제 MySQL version, physical core, memory, storage, `max_connections`, 현재 `Threads_connected/Threads_running` peak 확인
- apis/admin/batch replica 수와 Blue-Green overlap을 포함한 공유 RDS connection ceiling(dev steady 12/overlap 22, prod steady 14/overlap 24,
  양 환경 동시 overlap 46) 검증
- 모듈별 Hikari acquire/usage/pending과 transaction p95/p99를 계측하고 DB 전체 safe connection budget 안에서 Pool 배분 ADR 작성
- migration owner/tool, production HA·backup·PITR 확인
- request 건수/body 상한과 SMS 도착 SLO 확정
- 운영과 동급인 격리 staging에서 k6 + metrics로 Worker OFF/1/2, backlog 0/100/10,000 비교
- `DB Job Queue`와 `Outbox + SQS`의 장애 격리·운영비·확장 요구를 ADR로 비교하고 선택 승인

### Phase 1 — DB와 API 경계

- bulk lock query
- 기존 all-or-nothing HTTP contract 유지
- 공통 idempotency/delivery schema와 port/adapter
- SQS 경로의 Outbox schema와 port/adapter
- application service의 동일 transaction 직접 기록
- booking + idempotency + delivery transaction test
- SQS 경로의 Outbox 포함 transaction test

### Phase 2 — ADR 결과별 전달 경로

- 공통: claim token/fencing, backlog metric과 alert
- SQS 채택: SQS Standard/DLQ/IAM/IaC, Spring Cloud AWS 4.0.2와 SDK v2, Polling Publisher, partial batch response 처리
- DB Job Queue 채택: delivery DB polling worker, lease 만료 복구, DB backlog·lock wait 관측

### Phase 3 — SMS Worker

- processing token/fencing과 암호화 snapshot
- 공식 SOLAPI SDK typed adapter
- retry/FAILED/ACCEPTANCE_UNKNOWN/DELIVERY_UNKNOWN 정책
- provider 결과 조회와 `ACCEPTANCE_UNKNOWN` resolver
- SQS 채택: `@SqsListener`, manual ACK, visibility timeout
- DB Job Queue 채택: scheduled polling job, DB lease와 조건부 완료 처리
- 기존 async listener 제거
- 최소 runbook·alert와 load/chaos test

### Phase 4 — 운영 기능

- idempotency/delivery reconciliation job과 SQS 경로의 Outbox reconciliation
- admin 실패 조회·권한 검증·수동 재발행과 append-only 재발행 감사 원장
- admin에서 provider 조회 결과와 운영자 판정 제공
- SLO dashboard와 runbook
- 필요할 때만 `notification-worker` 모듈 분리

### Phase 5 — 측정 후 선택

- client와 함께 항목별 결과를 제공하는 command API v2
- polling publish latency가 SLO를 위반할 때만 fast path ADR
- 단일 host가 허용 RTO를 못 맞출 때 batch/consumer 다중 instance 또는 독립 worker 배포

## 17. 검토한 대안

| 대안                         | 장점                                            | 선택하지 않은 이유                                                                                         |
|----------------------------|-----------------------------------------------|----------------------------------------------------------------------------------------------------|
| 현재 `@Async + AFTER_COMMIT` | 구현이 가장 작고 빠름                                  | process crash 유실, retry/DLQ 없음, CallerRuns로 request thread 회귀                                      |
| 직접 DB commit 후 SQS publish | 단순하고 낮은 지연                                    | DB와 SQS dual-write gap으로 event 유실                                                                  |
| Redis Pub/Sub              | 단순, 낮은 latency                                | at-most-once, subscriber 장애 중 유실                                                                   |
| Redis Streams              | consumer group, pending/replay                | 단일 Redis 장애 영역, auth/cache와 blast radius 공유, 운영 내구성 미검증                                            |
| DB Job Queue만 사용           | 가장 작은 운영 구성, DB transaction과 작업 원장이 자연스럽게 원자적 | 현재 80건 burst만 보면 충분히 유효한 선택이다. SQS는 managed buffering, DB 장애 영역 분리, 독립 consumer 확장이 실제 요구일 때만 선택한다 |
| SQS FIFO                   | 순서와 broker dedup                              | 현재 순서 요구 없음, 외부 SMS exactly-once 문제는 해결하지 못함                                                       |
| SNS + SQS                  | 소비자별 fan-out                                  | 현재 소비자 한 개라 불필요                                                                                    |
| EventBridge                | routing/filtering, 다수 target                  | 현재 단일 task queue에 과함                                                                               |
| Kafka                      | 높은 처리량, replay, consumer group                | 약 80건 burst에 운영 비용이 과함                                                                             |
| CDC/Debezium               | application polling 제거                        | connector/schema/운영 복잡도가 현재 규모에 과함                                                                 |
| Spring Batch               | chunk/restart metadata                        | 상시 queue consumer와 짧은 relay polling에는 부적합; 대량 재처리 job에는 향후 사용 가능                                   |

두 번째 소비자가 추가되면 같은 SQS queue에 붙이지 않는다. SQS consumer 여러 개는 fan-out이 아니라 경쟁 소비다. 그 시점에는 `SNS/EventBridge → 소비자별 SQS` 구조를
검토한다.

따라서 SQS는 “처리량이 80건이라서” 선택하지 않는다. Phase 0 ADR에서 장애 중 수일 보존, DB와 별도 backpressure, 운영 DLQ, 향후 독립 worker라는 가치가 추가 운영비보다 크다는
근거가 확인돼야 최종 채택한다. 그렇지 않으면 같은 idempotency/delivery 원장과 fencing 상태 모델을 유지한 DB Job Queue가 BEAT의 더 작은 정답이다.

## 18. 예상 결과

### 기능

- booking 확정과 notification 작업 생성 사이의 유실 구간이 사라진다.
- SMS 장애가 booking transaction을 rollback하지 않는다.
- client timeout 뒤 같은 commandId 재시도가 booking과 SMS를 중복 생성하지 않는다.
- 1차 API의 기존 all-or-nothing 성공·실패 계약을 유지한다.
- 실패·재시도·delivery `FAILED`·SQS 경로의 DLQ·`ACCEPTANCE_UNKNOWN`·`DELIVERY_UNKNOWN`을 운영자가 추적할 수 있다.

### 성능

- request trace에서 SOLAPI 외부 I/O가 제거된다.
- 78건 요청은 공통 booking/idempotency/delivery와 SQS 경로의 Outbox DB 작업만 기다린다.
- booking별 lock N+1을 bulk lock으로 줄인다.
- SQS 채택 시 78개 event publish는 SQS batch 최대 8회로 처리한다.
- DB Job Queue 채택 시 측정한 DB 부하와 provider rate limit에 맞춰 polling batch 크기를 정한다.

### 복원력

- SQS 채택 시 SQS 장애 동안 Outbox가 buffer 역할을 하고 consumer 장애 동안 SQS가 message를 보존한다.
- DB Job Queue 채택 시 worker 장애 동안 delivery row가 작업을 보존하며 DB가 동일 장애 영역이 된다.
- 중복 message와 늦은 worker 결과는 delivery ledger와 fencing token으로 흡수한다.
- SQS 채택 시 poison message는 DLQ로 격리한다. DB Job Queue 채택 시 영구 실패 delivery를 `FAILED` 상태와 운영 조회 대상으로 격리한다.
- 단일 host 장애 시 데이터는 보존되지만 복구 전까지 발송은 지연된다.

이 결과는 구현만으로 선언하지 않는다. Phase 0 baseline과 Phase 3 load/chaos test 결과를 ADR에 추가한 뒤 “채택·구현 완료”로 상태를 바꾼다.

## 19. 근거 자료

### 회사 기술 사례

- [29CM — 트랜잭셔널 아웃박스 패턴의 실제 구현 사례](https://medium.com/@greg.shiny82/%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%94%EB%84%90-%EC%95%84%EC%9B%83%EB%B0%95%EC%8A%A4-%ED%8C%A8%ED%84%B4%EC%9D%98-%EC%8B%A4%EC%A0%9C-%EA%B5%AC%ED%98%84-%EC%82%AC%EB%A1%80-29cm-0f822fc23edb)
    - `BEFORE_COMMIT`에서 DB transaction과 Outbox 기록을 묶고, `AFTER_COMMIT` 즉시 발행과 실패 event polling을 결합했다.
    - 글의 제목과 본문에 명시된 운영 대상은 29CM이다.
    - CDC/Debezium보다 팀의 당시 운영 역량에 맞는 직접 구현을 선택했다.
- [리디 — Transactional Outbox 패턴으로 메시지 발행 보장하기](https://ridicorp.com/story/transactional-outbox-pattern-ridi/)
    - Polling Publisher를 선택하고 다중 relay, DB lock, 지연·처리량 monitoring을 운영했다.
- [리디 — Transactional Outbox message relay 개선하기](https://ridicorp.com/story/transactional-outbox-message-relay-ridi/)
    - `processed_message` join과 Redis lock을 제거하고 MySQL lock/query plan을 개선했다.
    - Outbox는 패턴 도입보다 lock, cleanup, query plan, backlog monitoring이 운영 품질을 좌우함을 보여준다.

### AWS와 Spring 공식 문서

- [AWS Prescriptive Guidance — Transactional outbox pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)
- [Amazon SQS Standard queues](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/standard-queues.html)
- [Amazon SQS at-least-once delivery](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/standard-queues-at-least-once-delivery.html)
- [Amazon SQS visibility timeout](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html)
- [Amazon SQS dead-letter queues](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html)
- [Amazon SQS SendMessageBatch API](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_SendMessageBatch.html)
- [Amazon SQS FIFO exactly-once processing과 5분 deduplication](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues-exactly-once-processing.html)
- [AWS Decision Guide — SQS, SNS, EventBridge](https://docs.aws.amazon.com/decision-guides/latest/sns-or-sqs-or-eventbridge/sns-or-sqs-or-eventbridge.html)
- [Spring Cloud AWS 4.0.2 Reference — SQS Integration](https://docs.awspring.io/spring-cloud-aws/docs/4.0.2/reference/html/index.html#sqs-integration)
- [Spring Cloud AWS compatibility matrix](https://github.com/awspring/spring-cloud-aws#compatibility-with-spring-project-versions)
- [Spring Framework — Transaction-bound Events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- [Spring Framework —
  `@TransactionalEventListener` Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html)
- [AWS SDK for Java v1 support 종료 공지](https://aws.amazon.com/blogs/developer/announcing-end-of-support-for-aws-sdk-for-java-v1-x-on-december-31-2025/)
- [SOLAPI 공식 Kotlin/Java SDK](https://github.com/solapi/solapi-kotlin)

### DB와 Connection Pool 공식 자료

- [HikariCP — About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [HikariCP — `minimumIdle`, `maximumPoolSize` 설정](https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby)
- [MySQL 8.4 — `max_connections`](https://dev.mysql.com/doc/refman/8.4/en/server-system-variables.html#sysvar_max_connections)
- [MySQL 8.4 — `SHOW STATUS`, `Threads_connected`, `Threads_running`](https://dev.mysql.com/doc/refman/8.4/en/show-status.html)

## 20. 이력서·포트폴리오 표현

SQS가 ADR에서 채택되고 부하·장애 테스트까지 완료된 뒤에만 다음처럼 작성한다.

> 약 80건의 예매 입금 확인 요청에서 동기 SMS와 booking별 잠금 조회로 14초가 소요되고 client timeout이 발생하던 흐름을 분석했다. 예매 확정·멱등성 원장·SMS
> delivery·Outbox를 하나의 MySQL transaction으로 묶고, SQS Standard/DLQ와 fencing 기반 멱등 consumer로 외부 SMS I/O를 request path에서 분리했다.
> partial batch failure, visibility timeout, duplicate delivery, provider ambiguous timeout을 상태 모델과 reconciliation으로
> 처리했으며, 부하·장애 주입 테스트로 API p95와 불변식 위반 여부를 검증했다.

측정하지 않은 성능 수치나 “exactly-once SMS”는 쓰지 않는다.
