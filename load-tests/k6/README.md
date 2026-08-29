# k6

k6는 애플리케이션 밖에서 부하를 생성하는 독립 테스트 도구입니다. 런타임 계측을 담당하는
`observability` Gradle 모듈에 포함하지 않습니다.

## 구조

```text
k6/
├─ lib/                     # 공통 안전 설정과 HTTP preflight
└─ scenarios/
   ├─ http/                 # 일반 HTTP API
   └─ ticket-confirmation/  # 예매 확정 DB queue 전환 성능 실험
```

새 API는 먼저 범용 HTTP 시나리오의 요청 JSON으로 측정합니다. 여러 요청의 순서, 고유 mutation
데이터, queue job 수처럼 별도 불변식과 지표가 필요한 경우에만 독립 시나리오를 추가합니다.

개발자 PC 또는 외부 ephemeral runner에서 dev/prod API를 대상으로 실행합니다. `TARGET_ENV`와
`BASE_URL`은 필수이며, 환경별 allowlist와 HTTPS origin을 함께 검증합니다. 부하 모양은 실행 인자가
아닌 버전이 붙은 `LOAD_PROFILE` budget으로 고정합니다.

모든 시나리오의 k6 지표는 OTLP로 기존 Alloy에 전달됩니다. Spring Boot의 HTTP·JVM·Hikari
지표와 RDS 지표는 기존 수집 경로를 사용하므로 API별 Alloy 설정은 추가하지 않습니다.

비즈니스 처리량처럼 기본 지표에 없는 값만 해당 기능의 애플리케이션 코드에서 Micrometer로
계측합니다. 계측은 부하 테스트 설정에 따라 켜고 끄지 않고 항상 동일하게 동작해야 하며,
Booking ID·사용자 ID 같은 고카디널리티 값은 metric tag로 사용하지 않습니다.

## 설정 기준

기본 실행이 정상인지 먼저 확인한 뒤 한 번에 하나의 값만 변경합니다. 구현 전후를 비교할 때는
아래 값과 데이터 구성을 동일하게 유지해야 합니다.

### 실행 대상과 데이터

| 설정 | 기본값 | 언제 사용하는가 | 왜 필요한가 |
| --- | --- | --- | --- |
| `TARGET_ENV` | 필수, `dev` 또는 `prod` | 측정 환경을 선택할 때 | 환경별 hostname allowlist와 server type을 선택합니다. |
| `BASE_URL` | 필수 | 측정 대상 API를 지정할 때 | allowlist된 HTTPS origin만 받고, 요청 파일에는 상대 경로만 두어 다른 호스트로 요청이 새지 않게 합니다. |
| `TEST_ID` | 자동 생성, 지정 권장 | Grafana와 결과 파일을 연결할 때 | 실행 간 지표와 JSON summary를 구분합니다. |
| `GIT_SHA` | `GITHUB_SHA` 또는 `unknown` | 배포 코드와 결과를 연결할 때 | 모든 k6 지표와 summary에 기록합니다. |
| `SERVER_TYPE` | 환경 기본값 | 실제 서버 사양이 기본값과 다를 때 | `dev=t3.micro`, `prod=t4g.small` 기본값을 덮어씁니다. |
| `DATASET_HASH` | 자동 계산, 지정 선택 | 입력 데이터 무결성을 확인할 때 | 지정하면 파일의 SHA-256과 일치하는지 검증합니다. |
| `ACCESS_TOKEN` | 없음 | 인증 API를 측정할 때 | Bearer 토큰으로 사용됩니다. 예매 확정 시나리오에는 필수입니다. |
| `REQUEST_FILE` | `./requests.json` | 범용 HTTP 요청 구성을 바꿀 때 | method, path, body, 예상 상태 코드를 코드 수정 없이 교체합니다. |
| `DATA_FILE` | `./cases.json` | 예매 확정 데이터 세트를 바꿀 때 | 서로 다른 Booking으로 상태 변경 API를 반복하기 위해 사용합니다. 요청당 Booking 수는 파일에서 자동 계산합니다. |
| `REUSE_REQUESTS` | `false` | 읽기 전용 요청을 반복할 때만 `true` | 상태 변경 요청의 중복 실행을 기본적으로 막습니다. 쓰기 API에는 사용하지 않습니다. |
| `LOAD_PROFILE` | `smoke` | 고정된 부하 profile을 선택할 때 | `smoke`, `baseline`, `step`만 허용하며 RPS·duration runtime override는 거부합니다. |
| `SUMMARY_FILE` | `summary-<test_id>.json` | JSON 결과 위치를 바꿀 때 | k6 metric과 실행 metadata를 로컬 artifact로 남깁니다. |

### 부하 모양과 판정 기준

현재 시나리오는 arrival-rate executor를 사용합니다. 서버 응답이 느려져도 정해진 비율로 새
iteration을 시작하므로, 동시 사용자 수가 아니라 **초당 유입량**을 고정해 비교할 때 적합합니다.

`LOAD_PROFILE`은 workload별 versioned budget을 선택합니다. generic HTTP는 `smoke=1 RPS × 1분`,
`baseline=5 RPS × 5분`, `step=1→2→5→10 RPS 각 2분`이며 hard cap은 `10 RPS / 10분`입니다.
ticket-confirmation은 `smoke=1 RPS × 1분`, `baseline=1 RPS × 1분`, `step=1 RPS × 2분`이며
hard cap은 `1 RPS / 2분`입니다. cap과 VU budget은 `load-tests/k6/lib/budgets.js`에서만 변경합니다.

| 설정 | 기본값 | 언제 사용하는가 | 왜 필요한가 |
| --- | --- | --- | --- |
| `REQUEST_TIMEOUT` | HTTP `10s`, 예매 확정 `30s` | 정상적으로 오래 걸리는 API를 측정할 때 | k6가 개별 응답을 기다리는 최대 시간입니다. 서버 timeout을 변경하는 값은 아닙니다. |
| `MAX_P95_MS` | 없음 | 합의된 응답시간 SLO를 자동 판정할 때 | 지정하면 `p(95)<값` threshold가 추가되어 기준을 넘은 실행이 실패합니다. 탐색 테스트에서는 생략할 수 있습니다. |
| `PREFLIGHT_PATH` | `/api/main` | 기본 경로가 readiness 확인에 적합하지 않을 때 | 실제 부하 전에 대상 서버가 응답하는지 확인합니다. |
| `PREFLIGHT_EXPECTED_STATUS` | `200` | preflight의 정상 상태 코드가 다를 때 | 예상하지 않은 환경이나 인증 상태에서 테스트를 시작하지 않게 합니다. |

예매 확정에서 실제 시도 처리량은 `profile RPS × 요청당 Booking 수`입니다. 예를 들어 요청당 `100건` 데이터는
1 RPS에서 초당 최대 `100건`의 상태 전이를 시도합니다. `1/10/78/100건` 비교는 각각 동일한 크기의
`bookingList`를 가진 데이터 파일로 수행합니다.

```bash
# baseline budget을 선택하고 p95 500ms를 통과 기준으로 사용
TARGET_ENV="dev" BASE_URL="https://api-dev.beatlive.kr" LOAD_PROFILE="baseline" \
MAX_P95_MS="500" k6 run ...
```

`TARGET_RPS`, `DURATION`, `PRE_ALLOCATED_VUS`, `MAX_VUS` 같은 budget override는 실행 전에 거부됩니다.
`dropped_iterations`, preflight 실패, fixture 소진, 30초 지연 후 5% 초과 expected-status 실패·timeout
급증은 threshold로 실행을 중단합니다. `TARGET_ENV=prod`이면 shared RDS 영향 경고를 먼저 출력합니다.

### OTLP 전송

OTLP 설정은 서버 부하 모양이 아니라 측정값을 Alloy로 전달하는 방법을 제어합니다. 특별한 이유가
없으면 실행 예시의 값을 그대로 사용합니다.

| 설정 | 현재 사용값 | 언제 사용하는가 | 왜 필요한가 |
| --- | --- | --- | --- |
| `K6_OTEL_SERVICE_NAME` | `beat-k6` | 여러 부하 발생기를 서비스별로 구분할 때 | OTLP resource의 service name으로 사용됩니다. k6 공식 기본값은 `k6`입니다. |
| `K6_OTEL_METRIC_PREFIX` | `k6_` | 기존 애플리케이션 지표와 이름 충돌을 피할 때 | 내보내는 k6 metric 이름 앞에 붙습니다. 공식 기본값은 빈 문자열입니다. |
| `K6_OTEL_GRPC_EXPORTER_ENDPOINT` | `127.0.0.1:4327` | Alloy tunnel의 주소나 포트가 바뀔 때 | k6 metric을 받을 OTLP gRPC endpoint를 지정합니다. |
| `K6_OTEL_GRPC_EXPORTER_INSECURE` | `true` | 현재처럼 loopback SSH tunnel로 전송할 때 | 로컬 구간에서 TLS 없이 gRPC를 사용합니다. 외부 네트워크에 직접 노출할 때는 사용하지 않습니다. |
| `K6_OTEL_EXPORT_INTERVAL` | 공식 기본값 `10s` | Grafana 반영 주기와 export 호출량을 조정할 때 | 낮추면 지표가 더 자주 보이지만 export 오버헤드가 증가합니다. 현재는 기본값을 사용합니다. |
| `K6_OTEL_FLUSH_INTERVAL` | 공식 기본값 `1s` | k6 내부 metric flush 해상도를 조정할 때 | 일반적인 부하 테스트에서는 변경할 필요가 없습니다. |

관련 공식 문서:

- [Constant arrival rate](https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/constant-arrival-rate/)
- [Arrival-rate VU allocation](https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/arrival-rate-vu-allocation/)
- [Thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/)
- [OpenTelemetry output](https://grafana.com/docs/k6/latest/results-output/real-time/opentelemetry/)
