# 재고 경쟁 예매 성능 테스트

`POST /internal/experiments/stock-contention/{STRATEGY}/bookings`에 합성 회원의 고유 access token을
사용해 동일 회차의 재고 경쟁을 재현합니다. 목적은 Pessimistic, Optimistic, Redis Lock,
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
- 공통 `ACCESS_TOKEN`은 사용하지 않습니다. 모든 case가 서로 다른 회원의 access token을 가집니다.
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

`cases.example.json`은 필드 예시만 담고 있으므로 그대로 실행할 수 없습니다. 실제
`cases.json`은 다음 조건을 모두 만족해야 합니다.

- `schema_version`은 `v1`
- 전체 1,100개 case 중 `warmup` 900개, `flash` 200개
- 두 phase 전체에서 access token 중복 금지 및 빈 값 금지
- warmup case는 하나의 warmup schedule ID만 사용
- flash case는 하나의 flash schedule ID만 사용
- warmup과 flash schedule ID는 서로 다름
- 모든 case의 `purchaseTicketCount`는 1
- warmup schedule stock은 최소 900
- flash schedule stock은 정확히 100
- 각 case의 `bookerName`과 `bookerPhoneNumber`는 API validation을 통과해야 함

토큰은 파일 안에만 존재하고 metric tag, 로그, summary metadata에 기록하지 않습니다.
`DATASET_HASH`에는 원본을 역으로 복원할 수 없는 SHA-256 digest만 기록합니다.

## 고정 profile

profile은 실행 인자로 RPS를 바꾸지 못하도록 `lib/budgets.js`에서 versioned budget으로
관리합니다.

| profile | workload | case 수 | 용도 |
| --- | --- | ---: | --- |
| `warmup` | 5 RPS × 180초 | 900 | JVM, Hikari, DB page warm-up |
| `flash` | 200 RPS × 1초 | 200 | stock 100에 대한 경쟁 측정 |

flash는 k6 open arrival-rate 모델이므로 200 VU가 DB transaction 200개와 같은 의미는
아닙니다. dropped iteration이 발생하면 결과를 사용하지 않습니다.

## 실행

dev 서버를 한 번 배포한 뒤, 각 strategy마다 합성 warmup/flash schedule을 같은 초기 상태로
복원하고 아래 두 profile을 순서대로 실행합니다. reset API는 추가하지 않으므로 fixture 복원은
실험 운영 절차에서 수행합니다. 각 warmup 뒤 flash 전, 그리고 strategy 사이에는 60초 quiet
period를 둡니다. 다섯 repetition block은 서로 다른 strategy 순서를 사용합니다.

```bash
cd load-tests/k6/scenarios/stock-contention
set -euo pipefail

GIT_SHA="$(git rev-parse HEAD)"
DATASET_HASH="$(sha256sum cases.json | awk '{print $1}')"

run_profile() {
  local strategy="$1"
  local profile="$2"
  TARGET_ENV="dev" \
  BASE_URL="https://api-dev.beatlive.kr" \
  STRATEGY="${strategy}" \
  DATA_FILE="./cases.json" \
  TEST_ID="${TEST_ID}" \
  GIT_SHA="${GIT_SHA}" \
  DATASET_HASH="${DATASET_HASH}" \
  LOAD_PROFILE="${profile}" \
  K6_OTEL_SERVICE_NAME="beat-k6" \
  K6_OTEL_METRIC_PREFIX="k6_" \
  K6_OTEL_GRPC_EXPORTER_ENDPOINT="127.0.0.1:4327" \
  K6_OTEL_GRPC_EXPORTER_INSECURE="true" \
  k6 run --out opentelemetry stock-contention.js
}

STRATEGY_ORDERS=(
  "PESSIMISTIC OPTIMISTIC REDIS ATOMIC"
  "OPTIMISTIC REDIS ATOMIC PESSIMISTIC"
  "REDIS ATOMIC PESSIMISTIC OPTIMISTIC"
  "ATOMIC PESSIMISTIC OPTIMISTIC REDIS"
  "PESSIMISTIC REDIS ATOMIC OPTIMISTIC"
)

for REPETITION in 1 2 3 4 5; do
  read -r -a STRATEGIES <<< "${STRATEGY_ORDERS[$((REPETITION - 1))]}"
  for STRATEGY in "${STRATEGIES[@]}"; do
    # 매 repetition/strategy마다 동일한 fixture와 dataset을 복원한다.
    TEST_ID="stock-contention-${STRATEGY}-r${REPETITION}-$(date +%Y%m%d-%H%M%S)"
    run_profile "${STRATEGY}" warmup
    sleep 60
    # flash schedule을 stock 100으로 복원한 뒤 flash를 제공한다.
    run_profile "${STRATEGY}" flash
    sleep 60
  done
done
```

기본 결과 파일은 다음처럼 profile별로 생성됩니다.

```text
summary-<test_id>-warmup.json
summary-<test_id>-flash.json
```

summary에는 strategy, profile, budget version, case count, accepted/sold_out/
conflict_exhausted/lock_timeout/unexpected 수, accepted TPS, accepted latency p50/p95/p99,
attempts, timeouts, dropped, drain time 추정값이 포함됩니다. 최종 schedule stock,
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
stock_contention_completion_elapsed_ms
stock_contention_attempt_count
```

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
