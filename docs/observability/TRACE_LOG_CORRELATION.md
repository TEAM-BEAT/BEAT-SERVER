# 트레이스 ↔ 로그 상관(correlation) 설정

목표: Tempo 트레이스와 Loki 로그를 **단일 `trace_id`** 로 양방향 드릴다운한다.
1:1 건수 일치가 목표가 아니다 — OTel auto-instrument 스팬은 요청당 N개, 로그는 요청당 ~1개(access log)+임의 로그라 **스팬 ≫ 로그가 정상**이다.

## ID 통일 체인 (코드로 보장됨)
```
valid inbound W3C v00 traceparent
  └─ nginx map이 원문 보존 → 앱 Micrometer/Tempo trace 계속

absent/invalid traceparent
  └─ nginx $request_id(32 hex)로 traceparent 생성 → 새 앱/Tempo trace

nginx access log
  ├─ trace_id: 실제 upstream으로 전달한 traceparent의 trace-id
  └─ request_id: nginx $request_id (edge request 식별자)
```
→ nginx `trace_id` == 앱 `trace_id` == Tempo traceId. `X-Request-ID`/`request_id`는 inbound trace context가 있을 때 trace-id와 다를 수 있다.

`default.conf.j2`는 Nginx `map`으로 소문자 exact-length W3C v00 형식을 검증하고 all-zero trace-id/parent-id를 거절한다. 유효한 값은 `$http_traceparent`를 `proxy_set_header traceparent` 로 그대로 전달하고, 대문자를 포함한 비정상 값에는 request-id 기반 fallback을 사용한다.

## 로그 스키마 (Tier 1·3 적용 후)
| 출처 | 포맷 | trace 필드 | 비고 |
|------|------|-----------|------|
| 앱(JsonTemplateLayout) | JSON | `trace_id` | prod·dev 동일(JsonConsoleAppender) |
| nginx access log | JSON(`escape=json`) | `trace_id`, `request_id` | `trace_id`는 전달된 trace context, `request_id`는 edge request 식별자 |
| 로컬 실행 | plain text | `traceId=`(MDC) | `dev` 프로파일 기본값(env 무설정), 수집 대상 아님 |

Alloy `stage.json`(`config.alloy.j2`)이 앱·nginx 양쪽 JSON에서 동일하게 파싱 가능.

## Tier 2 — Grafana Cloud 설정 (코드 아님, Cloud UI/API)
> Grafana Cloud managed 라 provisioning 파일이 repo에 없다. 아래는 데이터소스 설정 값.

### Loki datasource → Derived fields (로그 → 트레이스)
- **Name**: `trace_id`
- **Type**: Regex in log line (또는 JSON parser 사용 시 label)
- **Regex**: `"trace_id":"([a-f0-9]+)"`
- **Internal link → Data source**: Tempo
- **URL**: `${__value.raw}`

### Tempo datasource → Trace to logs (트레이스 → 로그)
- **Data source**: Loki
- **Tags**: 비움(매핑 불필요) 또는 `service.name → module`
- **Custom query** (권장, span trace-id로 직접 조회):
  ```logql
  {env="prod"} | json | trace_id = "${__trace.traceId}"
  ```
- **Filter by trace ID**: on
- **Filter by span ID**: off (로그는 span 단위가 아님)

### 검증 (완료 조건)
1. Tempo에서 트레이스 1건 열기 → "Logs for this span/trace" → 동일 `trace_id` 로그가 뜬다.
2. Loki에서 로그 1건의 `trace_id` 링크 클릭 → 해당 Tempo 트레이스로 이동한다.
3. nginx access log(JSON)도 `| json | trace_id="..."` 로 조회된다(이전엔 plain text라 제외됐음).

## 운영 주의
- inbound `trace_id`는 외부 요청이 지정할 수 있는 correlation 값이다. 인증·권한 판정이나 보안 감사의 단독 식별자로 사용하지 말고, edge가 생성한 `request_id`와 함께 확인한다.
- **로컬 개발**: `dev` 프로파일을 그대로 쓰면 **아무 설정 없이** 컬러 plain text 로그가 나온다(IntelliJ Run 그대로). deploy 되는 `dev`/`prod` 컨테이너는 ansible `app_container_env` 가 `BEAT_LOG_FORMAT=json` 을 주입해 JSON 으로 전환된다 — 이 한 줄(`infra/ansible/roles/app_container_runtime/tasks/env.yml`)이 유일한 배포 신호이므로 제거 시 deploy `dev` 로그가 plain text 로 떨어져 Loki 파싱이 깨진다.
- nginx `log_format` 변경은 host 전체 access log에 영향 → `ansible-playbook` 재적용 후 즉시 Grafana에서 JSON 파싱 확인.
