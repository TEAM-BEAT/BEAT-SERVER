# 재고 경쟁 예매 성능 테스트

`POST /api/internal/experiments/stock-contention/{STRATEGY}/bookings`에 합성 회원 1명의 access token 하나를
사용해 동일 회차의 재고 경쟁을 재현합니다. 모든 warmup/flash booking이 같은 회원으로 인증됩니다. 목적은 Pessimistic, Optimistic, Redis Lock,
Conditional Atomic UPDATE 구현을 한 번 배포한 서버에서 비교하는 것입니다. 기존
[`ticket-confirmation`](../ticket-confirmation/README.md) 시나리오는 변경하지 않습니다.

## 실행 경계

- `TARGET_ENV=dev`만 허용합니다. prod 대상은 코드에서 preflight 이전에 거부됩니다.
- `BASE_URL=https://api-dev.beatlive.kr`만 허용하며, 기존 공통 HTTPS allowlist 검증을 사용합니다.
- `STRATEGY`는 `PESSIMISTIC`, `OPTIMISTIC`, `REDIS`, `ATOMIC` 중 하나이며 필수입니다.
- 요청 timeout은 `35s`로 고정하며 실행 환경에서 덮어쓸 수 없습니다.
- 서버는 dev profile의 실험 flag 활성화 설정으로 한 번만 배포하고, strategy는 URL 경로로 선택합니다.
- endpoint는 기존 회원 Bearer 인증과 요청 validation을 그대로 사용합니다.
- 공통 회원·schedule·performance 조회는 요청당 한 번의 짧은 read-only transaction으로 실행하고
  connection을 반환합니다. OSIV는 비활성화하며 request 전체에 EntityManager나 connection을 유지하지 않습니다.
- 각 strategy의 재고 변경과 booking 저장은 하나의 reservation transaction에서 함께 commit 또는 rollback됩니다.
  Optimistic retry는 공통 조회를 반복하지 않고 reservation transaction만 새로 실행합니다.
- 실험 endpoint는 `BookingCreatedEvent`를 발행하지 않아 Slack 전송 없이 부하를 측정합니다.
- 공통 `ACCESS_TOKEN` 환경 변수는 사용하지 않습니다. `cases.json` 최상위의 accessToken 하나를 모든 booking이 공유합니다.
- `cases.json`은 1,100개 행을 저장하지 않고, k6가 profile별 동일 request를 생성·재사용합니다.
- `cases.json`과 summary 파일은 로컬에서만 사용하며 Git에 추가하지 않습니다.
- 외부 adapter, 배포, 배치 작업이 없는 dev 시간대에 실행합니다.

### Dev migration prerequisite

flag 활성화 또는 dev 배포 전에 SSH tunnel로 로컬 MySQL에 연결한 뒤,
`scripts/apply-local-dev-booking-migration.sh`를 실행해 `beatDev.schedule.version`
(`BIGINT NOT NULL DEFAULT 0`)을 적용·검증합니다. migration과 version 검증이 끝난 뒤에만
dev app을 시작합니다. 앱도 시작 시 같은 version 계약을 재검증하며, 누락되거나 호환되지
않으면 fail-fast 합니다.

응답 계약은 다음과 같습니다.

```text
201 + outcome=ACCEPTED → accepted
200 + outcome=SOLD_OUT → sold_out
200 + outcome=CONFLICT_EXHAUSTED 또는 LOCK_TIMEOUT → 해당 결과를 기록하지만 판정은 실패
그 외 status/body 또는 bookingId/attemptCount가 없는 응답 → unexpected
```

`bookingId`와 `attemptCount`는 응답에서 기록하며, 토큰·회원·예약 식별자는 metric tag에 넣지
않습니다.

## 데이터 준비

`cases.example.json`을 복사해 실제 dev fixture의 값으로 바꿔 `cases.json`을 만듭니다. 설정 파일은
다음 여섯 필드만 가질 수 있습니다.

- `schema_version`은 `v3`
- `accessToken`은 비어 있지 않고 whitespace를 포함하지 않아야 하며 모든 booking이 이 하나의 token을 사용
- `warmupScheduleId`와 `flashScheduleId`는 양의 정수이고 서로 달라야 함
- `bookerName`과 `bookerPhoneNumber`는 모든 booking에 재사용되며 API validation을 통과해야 함
- warmup schedule stock은 최소 900
- flash schedule stock은 정확히 100

`purchaseTicketCount`는 설정 파일에 넣지 않으며 runner가 항상 1로 고정합니다. warmup profile은
동일 warmup schedule request를 900번, flash profile은 동일 flash schedule request를 200번 생성·재사용합니다.

최상위 accessToken은 파일 안에만 존재하고 metric tag, 로그, summary metadata에 기록하지 않습니다.
`DATASET_HASH`에는 원본을 역으로 복원할 수 없는 SHA-256 digest만 기록합니다.

## 고정 profile

profile은 실행 인자로 RPS를 바꾸지 못하도록 `lib/budgets.js`에서 versioned budget으로
관리합니다.

| profile | workload | iteration 수 | 용도 |
| --- | --- | ---: | --- |
| `warmup` | 5 RPS × 180초 | 900 | JVM, Hikari, DB page warm-up |
| `flash` | 200 RPS × 1초 | 200 | stock 100에 대한 경쟁 측정 |

flash는 k6 open arrival-rate 모델이므로 200 VU가 DB transaction 200개와 같은 의미는
아닙니다. dropped iteration이 발생하면 결과를 사용하지 않습니다.

## 현재 환경의 변인 통제

이 실험은 cache와 JVM을 매번 강제로 초기화하는 cold-start benchmark가 아니라, 현재 dev의
steady-state에서 lock strategy만 바꾸는 비교 실험입니다. 통제할 수 있는 것은 고정하고,
shared RDS·OS cache처럼 안전하게 초기화할 수 없는 것은 같은 방식으로 예열한 뒤 run 전후 값을
기록합니다.

### 고정하는 조건

- 한 측정 block 동안 동일한 dev Git SHA·Docker image·JVM option·Hikari pool을 유지하고 배포하지 않습니다.
- 동일한 `cases.json`, endpoint, token, request body, timeout과 versioned workload budget을 사용합니다.
- warmup/flash schedule은 매 strategy 전에 동일한 stock, sold count, version으로 reset하고 read-back합니다.
- 동일한 Mac, k6 version, 전원과 네트워크를 사용합니다. Time Machine·대용량 동기화·회의 앱은 중지합니다.
- strategy별 유효 flash run을 5회 수집하고 문서에 정한 rotation 순서로 실행해 시간·순서 효과를 분산합니다.

### 강제로 초기화하지 않는 상태

- InnoDB buffer pool과 RDS OS page cache
- JVM JIT, heap과 GC 상태
- Hikari connection pool
- Redis 내부 cache와 shared RDS의 소량 외부 트래픽

RDS 재시작, cache flush, `drop_caches`, `TRUNCATE`와 광범위한 `DELETE`는 금지합니다. 이런 조작은
운영 환경과 다른 상태를 만들거나 다른 서비스에 영향을 줍니다. 대신 모든 strategy에서 동일하게
warmup한 뒤 60초 quiet period를 두고, 종료 후 최소 90초 동안 baseline 복귀를 확인합니다.

### run 전후 필수 snapshot

DB에서는 동일 connection으로 flash 직전과 drain 직후 아래 누적값을 같은 순서로 기록하고
`post - pre` delta를 결과에 남깁니다.

```sql
SHOW GLOBAL STATUS WHERE Variable_name IN (
  'Innodb_buffer_pool_read_requests',
  'Innodb_buffer_pool_reads',
  'Innodb_buffer_pool_read_ahead',
  'Innodb_buffer_pool_read_ahead_evicted',
  'Innodb_buffer_pool_pages_dirty',
  'Innodb_row_lock_waits',
  'Innodb_row_lock_time',
  'Threads_connected',
  'Threads_running',
  'Questions'
);
```

`Innodb_buffer_pool_reads`는 storage까지 간 physical read이고
`Innodb_buffer_pool_read_requests`는 logical read입니다. `SwapUsage`는 별개의 RDS OS memory
pressure입니다. shared RDS의 누적값에는 외부 접근이 섞일 수 있으므로 DB delta만으로 strategy의
우열을 판정하지 않고 같은 UTC 구간의 CloudWatch와 함께 설명합니다.

Grafana Cloud MCP로 run 전 10분 baseline과 실행·cooldown 구간에서 다음을 기록합니다.

- app: process/container/node CPU·memory, JVM heap·GC·allocation, Hikari active/pending
- RDS: CPU, FreeableMemory, SwapUsage, connection, read/write latency·IOPS, DiskQueueDepth
- MySQL/Redis: QPS, thread, buffer-pool, row-lock, Redis command rate와 exporter freshness
- pipeline: Alloy pending/failed, discarded samples, 429와 필수 scrape `up`

### AWS burst credit 통제

현재 k6 발생기는 Mac이므로 발생기에는 AWS CPU credit가 없습니다. 반면 dev application EC2와
shared `db.t3.micro` RDS는 burstable 계열입니다. RDS db.t3는 AWS가 Unlimited mode로 운영하며,
EC2는 실제 instance의 credit specification을 실험 전에 AWS Console/CLI로 확인합니다.

```bash
aws ec2 describe-instance-credit-specifications \
  --region ap-northeast-2 \
  --instance-ids <DEV_EC2_INSTANCE_ID>
```

각 block 전후 동일 instance의 `CPUCreditBalance`, `CPUCreditUsage`,
`CPUSurplusCreditBalance`, `CPUSurplusCreditsCharged`를 CloudWatch/Grafana MCP로 기록합니다.
EC2 또는 RDS credit balance가 run 중 0에 도달하거나 surplus balance/charged가 새로 증가하면
다른 CPU credit regime에서 실행된 것이므로 해당 run을 성능 비교에서 제외합니다. Unlimited는
credit 소진 뒤에도 실행을 허용하는 과금 방식이지, 실험 변인이 사라진다는 뜻이 아닙니다.

### run 무효화 조건

- `dropped_iterations`, unexpected response, timeout, lock timeout 또는 DB invariant 실패
- 실행 중 배포·batch·RDS backup·network interruption 발생
- CPU credit regime 변경 또는 cooldown 뒤 CPU/JVM/Hikari가 baseline으로 복귀하지 않음
- Hikari acquire timeout이나 지속 pending 발생
- RDS FreeableMemory·SwapUsage·latency·IOPS·DiskQueueDepth가 사전 baseline에서 지속 이탈
- Alloy pending/discard/429 때문에 해당 시간대 지표가 유실됨

위 조건을 통과한 run끼리만 비교합니다. 이 방식은 현재 shared dev/RDS에서 strategy의 상대 성능을
비교하기에는 충분하지만 절대 capacity를 증명하지는 않습니다. 절대 처리 한계가 필요하면 전용 RDS,
non-burstable application server와 전용 load generator에서 별도 실험합니다.

## 실행

dev 서버를 한 번 배포한 뒤, 각 strategy마다 합성 warmup/flash schedule을 같은 초기 상태로
복원하고 아래 runner로 profile 하나씩 실행합니다. 이 runner는 macOS 기본 shell이 zsh여도 Bash로
동작하며 workload 값은 받지 않습니다. reset API는 추가하지 않으므로 fixture 복원과 DB invariant
검증은 실행자가 별도로 수행해야 합니다.

```bash
cd load-tests/k6/scenarios/stock-contention
TEST_ID="stock-contention-PESSIMISTIC-r1-$(date +%Y%m%d-%H%M%S)"
./run-stock-contention.sh PESSIMISTIC warmup "$TEST_ID"
# 60초 quiet period와 flash fixture read-back 후 실행
./run-stock-contention.sh PESSIMISTIC flash "$TEST_ID"
```

Grafana로 OTLP metric도 보낼 때만 tunnel을 연 뒤 다음 환경변수를 추가합니다.

```bash
K6_OUTPUT=opentelemetry \
K6_OTEL_GRPC_EXPORTER_ENDPOINT=127.0.0.1:4327 \
K6_OTEL_GRPC_EXPORTER_INSECURE=true \
./run-stock-contention.sh PESSIMISTIC flash "$TEST_ID"
```

다섯 repetition의 strategy 순서는 아래 rotation 계약을 따르되 자동 반복하지 않습니다. 각
strategy 앞의 선택적 cleanup/reset/read-back과 종료 뒤 invariant/cooldown 확인이 성공한 뒤에만
다음 run을 시작해야 하기 때문입니다.

기본 결과 파일은 다음처럼 profile별로 생성됩니다.

```text
summary-<test_id>-warmup.json
summary-<test_id>-flash.json
```

summary에는 strategy, profile, budget version, case count, accepted/sold_out/
conflict_exhausted/lock_timeout/unexpected 수, accepted TPS, accepted latency p50/p95/p99,
attempts, optimistic retry 총수와 요청당 평균, timeouts, dropped, drain time 추정값이 포함됩니다. 최종 schedule stock,
overselling, duplicate booking, negative stock은 DB read-only query로 별도 검증해야 합니다.

## 판정 지표와 중단

custom metric은 HTTP status를 정상/실패로 잘못 해석하지 않도록 다음을 사용합니다.

```text
stock_contention_requests_submitted
stock_contention_bookings_accepted
stock_contention_bookings_sold_out
stock_contention_conflict_exhausted
stock_contention_lock_timeout
stock_contention_unexpected_response
stock_contention_request_timeout
stock_contention_timeouts
stock_contention_accepted_latency_ms
stock_contention_terminal_latency_ms
stock_contention_request_start_elapsed_ms
stock_contention_completion_elapsed_ms
stock_contention_drain_time_ms
stock_contention_attempt_count
stock_contention_optimistic_retries
```

각 custom metric에는 `test_id`, `git_sha`, `strategy`, `phase`가 붙고, 공통 k6
tag로 `load_profile`과 `scenario`도 전달됩니다. 현재 Alloy의
`otelcol.exporter.prometheus.k6`는 `add_metric_suffixes = false`이므로
`K6_OTEL_METRIC_PREFIX=k6_`가 붙은 뒤 Counter는 그 이름 그대로,
Rate인 `stock_contention_request_timeout`은
기본 `K6_OTEL_SINGLE_COUNTER_FOR_RATE=true` 기준
`k6_stock_contention_request_timeout_total`, Trend는
`k6_<name>_bucket/_sum/_count`로 Grafana에 나타납니다. Trend p95/p99는 OTLP
histogram bucket 기반 운영용 추정값이며, exact accepted latency·attempt·drain
판정은 local JSON summary를 기준으로 합니다.

`http_req_failed`는 응답 상태만으로 결과를 판정하므로 threshold에 사용하지 않습니다.
대신 recognized response, accepted/sold_out/conflict/lock-timeout/unexpected Counter, attempt,
timeout, dropped iteration threshold를 사용합니다. warmup은 accepted/sold_out/unexpected를
정확히 900/0/0으로, flash는 100/100/0으로 검증하고 conflict/lock-timeout과 timeout rate도
0이어야 합니다.

다음 중 하나라도 발생하면 실행을 폐기합니다.

- preflight 실패
- fixture data exhausted
- `dropped_iterations > 0`
- unexpected response, conflict/lock-timeout 또는 timeout이 1건 이상
- flash 결과가 accepted 100, sold_out 100이 아님
- DB invariant 검증 실패

실험 전후 RDS, Hikari, JVM, Redis와 shared DB의 영향을 별도로 기록합니다. 200건 flash의
p50/p95/p99는 summary에 저장하되, 작은 표본의 대표 latency는 p95로 보고하고 절대 TPS는
별도 non-burstable 환경에서 검증합니다.
