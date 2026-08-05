# 예매 확정 DB queue 전환 성능 테스트

`PUT /api/tickets/update`에 동일한 예매 확정 workload를 입력해 DB queue 도입 전 기준선과 도입 후
producer 성능을 비교하고, worker의 backlog 처리 성능을 별도로 측정합니다. SQS 비교나 SMS
provider 처리량 측정이 목적이 아닙니다.

## 측정 경계

측정을 세 단계로 나눕니다.

1. **도입 전 기준선**: 동일한 Booking 수로 기존 예매 확정 경로를 실행합니다.
   - 1/10/78/100건 절대 처리시간과 SELECT 수
   - transaction·connection 점유시간
   - provider 지연 변화가 API 처리시간에 미치는 영향
2. **도입 후 producer**: worker를 중지하고 같은 k6 요청을 보냅니다.
   - HTTP RPS·p95/p99·오류율
   - `ticket_confirmation_items_submitted`, `ticket_confirmation_items_accepted`
   - DB queue insert 수와 적재 실패 수
3. **도입 후 worker**: k6를 종료한 뒤 worker concurrency를 고정하고 backlog를 처리합니다.
   - 시작 backlog와 완료·실패·재시도 job 수
   - backlog가 0이 될 때까지 걸린 시간과 초당 처리량
   - queue wait time과 processing time

도입 후 HTTP 200은 job 처리가 끝났다는 뜻이 아니라 예매 확정 트랜잭션과 queue 적재가
수락됐다는 뜻입니다. `ticket_confirmation_items_accepted`를 worker 처리 성공 수로 해석하면
안 됩니다.

## 안전 경계

- 실제 예매를 차단한 prod `t4g.small` 서버와 prod RDS에서 구현 전후를 비교합니다.
- 기본값은 `1 RPS / 1분`이며 `TARGET_RPS`와 `DURATION`으로 원하는 부하를 지정합니다.
- 각 iteration은 서로 다른 `CHECKING_PAYMENT` Booking을 사용합니다.
- `cases.json`에는 합성 Booking ID만 저장하며 Git에 커밋하지 않습니다.
- worker 측정 중 외부 SMS adapter는 비활성화하거나 테스트 대역으로 교체합니다.
- 실험 전후 queue 상태를 초기화하고 다른 배포·배치 작업이 없는 시간에 실행합니다.

## 데이터 준비

`cases.example.json`을 복사해 `cases.json`을 만듭니다. API가 실제로 사용하는 필드만 작성합니다.

```json
{
  "performanceId": 1,
  "bookingList": [
    {
      "bookingId": 1,
      "bookingStatus": "BOOKING_CONFIRMED"
    }
  ]
}
```

- 모든 Booking은 요청의 `performanceId`에 속해야 합니다.
- 모든 Booking은 실행 전에 `CHECKING_PAYMENT` 상태여야 합니다.
- 데이터 전체에서 `bookingId`를 중복 사용하면 안 됩니다.
- 모든 요청의 `bookingList` 길이는 첫 요청과 같아야 합니다.
- case 수는 최소 `TARGET_RPS × DURATION(초)`개여야 합니다.

## 기준선·Producer 실행

먼저 worker를 중지하고 queue가 비어 있는지 확인합니다. Alloy OTLP 포트는 loopback에만
공개되므로 로컬에서 SSH tunnel을 엽니다.

```bash
ssh -N -L 4327:127.0.0.1:4327 ubuntu@PROD_HOST
```

```bash
cd load-tests/k6/scenarios/ticket-confirmation

TEST_ID="ticket-confirmation-$(date +%Y%m%d-%H%M%S)"

K6_OTEL_SERVICE_NAME="beat-k6" \
K6_OTEL_METRIC_PREFIX="k6_" \
K6_OTEL_GRPC_EXPORTER_ENDPOINT="127.0.0.1:4327" \
K6_OTEL_GRPC_EXPORTER_INSECURE="true" \
LOAD_TEST_ACK="rds" \
BASE_URL="https://PROD_API_HOST" \
ACCESS_TOKEN="${ACCESS_TOKEN}" \
DATA_FILE="./cases.json" \
k6 run \
  --out opentelemetry \
  --tag test_id="${TEST_ID}" \
  --tag environment="prod" \
  --tag server_instance_type="t4g.small" \
  ticket-confirmation.js
```

`1/10/78/100`건은 각각 해당 크기의 별도 데이터 세트로 실행합니다. warm-up을 한다면
측정 데이터와 겹치지 않는 별도 Booking을 사용하고, 기준선과 도입 후 실험에 동일하게 적용합니다.

DB queue 도입 후 producer 실행에서는 다음 값이 일치하는지 확인합니다.

```text
ticket_confirmation_items_accepted
= CHECKING_PAYMENT → BOOKING_CONFIRMED 전이 수
= 새로 생성된 DB queue job 수
= worker 시작 전 pending backlog
```

일치하지 않으면 worker 성능을 측정하지 말고 누락·중복 적재부터 조사합니다.

## Worker 실행 및 비교

동일한 backlog에 대해 worker 인스턴스 수와 concurrency를 기록한 후 시작합니다. 처리량은
`완료 job 수 / backlog drain 시간`으로 계산하고, 실패·재시도 job은 별도로 기록합니다.

구현이나 worker 설정을 비교할 때는 다음을 고정합니다.

- 동일한 Booking 수와 요청당 Booking 수
- 동일한 초기 queue 상태
- 동일한 worker 인스턴스 수·concurrency
- 동일한 DB와 애플리케이션 사양
- 동일한 warm-up 여부와 측정 시간

각 비교 실행 전 Booking과 queue 데이터를 초기 상태로 다시 준비합니다. SLO가 합의된 경우에만
`MAX_P95_MS`를 지정해 producer HTTP p95 threshold를 강제합니다.
