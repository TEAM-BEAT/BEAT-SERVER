# 범용 HTTP API 부하 테스트

Alloy·Grafana 설정이나 애플리케이션 계측을 추가하지 않고 임의 API의 RPS, 지연, 오류율과
기존 JVM·Hikari·RDS 지표를 같은 시간축에서 확인합니다.

## 요청 정의

`requests.example.json`을 복사해 `requests.json`에 요청을 작성합니다.

- `name`: Grafana label로 사용하는 고정된 저카디널리티 이름
- `method`, `path`: HTTP 요청. `path`는 승인된 prod origin의 상대 경로만 허용
- `headers`, `body`: 선택값
- `expectedStatuses`: 성공으로 판단할 HTTP 상태 목록

기본값은 요청 재사용을 금지합니다. 상태를 변경하는 API는 iteration마다 서로 다른 합성 데이터를
준비해야 합니다. 읽기 전용 API만 `REUSE_REQUESTS=true`로 같은 요청을 반복할 수 있습니다.

## 실행

Alloy SSH tunnel을 연 뒤 실행합니다. 공통 안전 설정은 `k6/lib/config.js`가 검증합니다.

```bash
cd load-tests/k6/scenarios/http

TEST_ID="http-$(date +%Y%m%d-%H%M%S)"

K6_OTEL_SERVICE_NAME="beat-k6" \
K6_OTEL_METRIC_PREFIX="k6_" \
K6_OTEL_GRPC_EXPORTER_ENDPOINT="127.0.0.1:4327" \
K6_OTEL_GRPC_EXPORTER_INSECURE="true" \
LOAD_TEST_ACK="rds" \
BASE_URL="https://PROD_API_HOST" \
ACCESS_TOKEN="${ACCESS_TOKEN}" \
REQUEST_FILE="./requests.json" \
k6 run \
  --out opentelemetry \
  --tag test_id="${TEST_ID}" \
  --tag environment="prod" \
  --tag server_instance_type="t4g.small" \
  generic-http.js
```

실행 전 실제 예매가 차단됐는지 확인합니다. 읽기 전용 API 한 건을 반복할 때만
`REUSE_REQUESTS=true`를 추가합니다. 기본값은 `1 RPS / 1분`이며, 다른 부하는 `TARGET_RPS`와
`DURATION`으로 지정합니다.

Grafana에서는 `name`, `test_id`, `environment`로 API별 결과를 분리합니다.
`api_request_failed`는 요청별 `expectedStatuses`를 기준으로 계산됩니다. 서버 측 공통
`http_server_requests_seconds_*`, JVM, Hikari, RDS 지표는 API별 코드 추가 없이 함께 수집됩니다.
