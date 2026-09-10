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

### Grafana MCP로 같은 시간대 원인 확인

세 관측 수단의 책임을 섞지 않습니다.

| 수단 | 책임 |
| --- | --- |
| local `summary-*.json` | 정확한 TPS, outcome, p95/p99, retry, timeout, dropped 판정 |
| DB read-only invariant query | 최종 재고, overselling, duplicate booking 정합성 판정 |
| Grafana Cloud MCP | 같은 UTC 구간의 app/JVM/Hikari, host/container, RDS/MySQL, Redis, Alloy 상태와 성능 차이의 원인 설명 |

Grafana MCP는 부하를 실행하거나 최종 결과를 대신 판정하지 않습니다. 실행자는 각 profile의 시작·종료
UTC와 `test_id`를 기록합니다. AI는 90 Load Test 대시보드와 원본 datasource를 read-only로 조회하고,
`git_sha`, `dataset_hash`, `budget_version`이 같은 run끼리만 비교해야 합니다. k6 tag를 갖지 않는 서버·RDS
지표는 해당 run의 UTC 구간으로 상관 분석합니다.

실행 후 AI에게 아래 형식으로 요청합니다.

```text
BEAT stock-contention 부하 실험을 read-only로 분석해줘.

- Grafana folder: BEAT Observability
- Dashboard: 90 Load Test
- test_id: <TEST_ID>
- strategy/profile: <STRATEGY>/<warmup|flash>
- UTC 시작: <YYYY-MM-DDTHH:mm:ssZ>
- UTC 종료: <YYYY-MM-DDTHH:mm:ssZ>
- local summary: <summary JSON 경로 또는 내용>
- DB invariant 결과: <최종 stock, accepted, duplicate, overselling 결과>

Grafana Cloud MCP로 같은 UTC 구간의 원본 datasource를 조회해 다음을 확인해줘.
1. run identity(test_id, git_sha, dataset_hash, budget_version)가 기대값과 일치하는지
2. experiment endpoint RPS와 server p95/p99
3. process CPU, JVM heap/GC/allocation, Hikari active/pending
4. node/container CPU·memory
5. RDS CPU·FreeableMemory·Swap·connections·latency·IOPS·DiskQueueDepth·BurstBalance
6. MySQL QPS·threads·buffer-pool reads/read requests·row-lock waits/time과 exporter freshness
7. Redis commands/sec와 redis_up
8. Alloy remote-write pending/failed, discarded samples, 429, 필수 scrape up

local JSON과 DB invariant를 최종 판정 기준으로 삼고 Grafana는 원인 설명에만 사용해줘.
No data를 0으로 해석하지 말고, 직접 확인한 사실과 추정을 분리해 표로 보고해줘.
다른 run과 비교할 때는 git_sha, dataset_hash, budget_version이 모두 같은 경우만 수치 비교해줘.
```

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
