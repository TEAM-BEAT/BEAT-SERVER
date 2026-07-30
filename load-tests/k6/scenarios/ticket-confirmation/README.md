# 예매 확정 부하 테스트

`PUT /api/tickets/update`의 HTTP 지연과 JVM·Hikari·RDS 지표를 같은 시간축에서 비교합니다.
DB Job Queue 도입 전 기준선과 도입 후 `batch 1대 / worker concurrency 1`을 비교하는 용도입니다.

## 안전 경계

- dev와 prod가 같은 RDS 인스턴스를 사용하므로 운영 피크 시간에는 실행하지 않습니다.
- 기본값은 `1 RPS / 1분`이며, 이 결과로 RDS 최대 처리량을 판단하지 않습니다.
- 이 시나리오는 실제 SMS provider 호출을 포함합니다. 승인된 합성 수신자만 사용합니다.
- `TEST_RECIPIENT`와 모든 Booking의 `bookerPhoneNumber`가 다르면 실행 전에 중단합니다.
- 각 iteration은 서로 다른 Booking을 사용해야 합니다. 같은 Booking을 재사용하면 실제 갱신 성능을 측정할 수 없습니다.
- 합성 Booking만 사용하고 DB의 전화번호도 테스트 전용 값으로 준비합니다.
- 실제 전화번호와 이름은 데이터 파일에 저장하지 않습니다.
- `cases.json`은 Git에서 제외됩니다.

## 준비

`cases.example.json`을 참고해 `cases.json`을 생성합니다. 모든 요청의 `bookingList` 길이는
`ITEM_COUNT`와 같아야 하며 Booking은 `CHECKING_PAYMENT` 상태여야 합니다. 데이터 세트에는
최소 `TARGET_RPS × DURATION(초)`개의 서로 겹치지 않는 요청이 있어야 합니다.
각 Booking이 요청의 `performanceId`에 속하고 `CHECKING_PAYMENT` 상태인지 DB에서 사전 확인합니다.

Alloy OTLP 포트는 서버 loopback에만 공개됩니다. 로컬 k6에서 dev 서버로 SSH tunnel을 엽니다.

```bash
ssh -N -L 4327:127.0.0.1:4327 ubuntu@DEV_HOST
```

## 실행

최신 k6 설치 후 별도 터미널에서 실행합니다.

```bash
cd load-tests/k6/scenarios/ticket-confirmation

TEST_ID="booking-$(date +%Y%m%d-%H%M%S)"

K6_OTEL_SERVICE_NAME="beat-k6" \
K6_OTEL_METRIC_PREFIX="k6_" \
K6_OTEL_GRPC_EXPORTER_ENDPOINT="127.0.0.1:4327" \
K6_OTEL_GRPC_EXPORTER_INSECURE="true" \
K6_OTEL_EXPORT_INTERVAL="5s" \
LOAD_TEST_ACK="shared-rds-dev" \
TARGET_ENV="dev" \
BASE_URL="https://DEV_API_HOST" \
ALLOWED_DEV_ORIGIN="https://DEV_API_HOST" \
PREFLIGHT_URL="https://DEV_API_HOST/api/main" \
ACCESS_TOKEN="${ACCESS_TOKEN}" \
SMS_SIDE_EFFECT_ACK="synthetic-recipient-approved" \
TEST_RECIPIENT="${TEST_RECIPIENT}" \
DATA_FILE="./cases.json" \
ITEM_COUNT="78" \
TARGET_RPS="1" \
DURATION="1m" \
k6 run \
  --out opentelemetry \
  --tag test_id="${TEST_ID}" \
  --tag environment="dev" \
  ticket-confirmation.js
```

`1/10/78/100`건은 각각 별도 데이터 세트와 `ITEM_COUNT`로 실행합니다. RPS를 1보다 높이는 실험은
사전에 RDS `DatabaseConnections`, Hikari pending, 오류율을 확인한 뒤 `MAX_SAFE_RPS`를 명시적으로
올려야 합니다. 공유 RDS 보호를 위해 코드의 절대 상한은 `2 RPS / 5분`이며 우회할 수 없습니다.

Latency 합격 기준은 기준선 측정 전 임의로 고정하지 않습니다. 합의된 SLO가 생기면
`MAX_P95_MS`를 설정해 k6 threshold로 강제합니다.

## 확인 지표

- k6: 실제 RPS, p95/p99, 오류율, dropped iterations
- Spring Boot: `http_server_requests_seconds_*`, JVM CPU/GC/memory
- Hikari: active, idle, pending, timeout
- RDS: DatabaseConnections, CPUUtilization, CPUCreditBalance, FreeableMemory, latency, DiskQueueDepth

Worker 구현 후 동일 조건에서 다음 두 결과를 비교합니다.

1. Worker OFF 기준선
2. `batch 1대 / worker concurrency 1`

두 실험 사이에는 Booking 데이터를 초기 상태로 다시 준비하고 backlog가 비었는지 확인합니다.
