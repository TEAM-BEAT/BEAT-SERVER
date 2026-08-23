# BEAT Batch 로그 Loki 미수집 — 증거 기반 진단 런북

batch(및 admin) 컨테이너 로그가 Grafana Cloud Loki에서 조회되지 않는 문제의 **증거 기반 진단 → 최소 수정 → 조회 검증** 절차다.
가설을 단정하지 말고 각 단계 결과로 원인을 좁힌다. 수정은 **원인이 확정된 단 한 지점만** 한다.

> 관련 설정: `infra/ansible/roles/observability_alloy/templates/config.alloy.j2` (섹션 7 로그 파이프라인)
> 전제: 현재 dev/prod inventory 모두 `observability_alloy_enable_logs: true`다. 환경별 inventory를 먼저 확인하고 진단한다.

## 사전 확인 (코드 레벨에서 이미 확정된 사실)
- Alloy `discovery.relabel` regex `^/.*?(apis|admin|batch|mysql|redis|nginx).*$` 는 `/batch`·`/admin`을 **정상 매칭**한다(module 라벨 부여 OK).
- relabel에는 `keep`/`drop` action이 없어, **regex가 틀려도 컨테이너가 드롭되지 않는다**. → "regex 매칭"은 완전 누락의 원인이 될 수 없다(우선순위 낮음).
- 따라서 진단은 **수집 단(stdout/driver/discovery)** 과 **파이프라인 drop** 을 중심으로 한다.

---

## 진단 절차 (prod 호스트 + Grafana 필요)

### Step 1 — 현재 label로 조회 (Grafana Explore)
```logql
{env="prod", module="batch"}
```
- 결과가 있으면 수집은 정상이며 조회 기간/필터를 점검한다.
- 결과가 없으면 Step 2로 진행한다. `container`는 현재 Loki JSON/label 계약에 없는 필드이므로 사용하지 않는다.

### Step 2 — 컨테이너가 stdout으로 로깅하며 docker가 읽을 수 있는가 (prod 호스트)
```bash
docker inspect -f '{{.HostConfig.LogConfig.Type}}' batch admin apis-blue
docker logs --tail 20 batch
```
- driver가 `json-file`/`local`/`journald`가 아니면 `loki.source.docker`가 tail 불가 → driver 정렬.
- `docker logs`에 아무것도 안 나오면 앱이 파일로만 로깅하는 것 → stdout 로깅으로 전환.
- apis-blue와 batch의 driver/출력이 다른지 비교(차이가 원인 후보).

### Step 3 — Alloy가 batch 컨테이너를 타깃으로 잡는가 (prod 호스트)
```bash
# Alloy UI(127.0.0.1:12345)의 discovery.docker / loki.source.docker 컴포넌트에서 batch 타깃 유무 확인
curl -s localhost:12345/api/v0/web/components | grep -i batch
docker logs alloy --tail 50 | grep -iE 'batch|error|docker'
```
- 타깃에 batch 없음 → discovery.docker가 해당 컨테이너를 못 봄(소켓 권한/네임스페이스).
- Alloy 로그에 docker API 에러 → 소켓 마운트/권한 점검.

### Step 4 — stale drop에 걸리는가 (Grafana, batch 특성상 1순위 후보)
```promql
sum(rate(loki_process_dropped_lines_total{reason="stale"}[1h])) by (instance)
```
- `stage.drop older_than="12h"`는 공유 processor에 적용된다. counter 증가만으로 batch 원인을 확정하지 말고, batch log 시각과 Alloy log/metric 증가 시각이 일치하는지 확인한다.

### Step 5 — 포맷/타임스탬프 파싱 (보조)
- batch 로그가 JsonTemplateLayout(`@timestamp`/`level`)이 아니면 `stage.timestamp`가 skip되어 scrape-time을 쓰지만
  **로그 자체는 들어온다**(level 라벨만 누락). 무필터 쿼리는 되는데 `{level="ERROR"}`만 빈다면 이 케이스.

---

## 최소 수정 결정트리

| 확정된 원인(Step) | 최소 수정 | 변경 위치 |
|------------------|-----------|-----------|
| Step 2: logging driver 부적합 | 컨테이너 `log_driver: json-file` 명시 | `infra/ansible/roles/app_stopstart/tasks/run_container.yml` |
| Step 2: 파일로만 로깅 | 앱 stdout 로깅 확인(이미 JsonConsoleAppender → 보통 OK) | 앱/컨테이너 설정 |
| Step 3: discovery 누락/권한 | docker.sock 마운트·권한 정렬 | `observability_alloy` role |
| Step 4: stale drop | `stage.drop older_than` 상향(예 `24h`) 또는 batch 백필 정책 조정 | `config.alloy.j2` 섹션 7 |
| Step 1: 라벨링만 문제 | discovery.relabel 규칙 점검(가능성 낮음) | `config.alloy.j2` |

> 인프라 변경은 영향 범위가 host 전체 Alloy 수집이므로, 1개 지점만 바꾸고 재적용(`ansible-playbook observability.yml`) 후 즉시 재검증한다.

---

## 완료 조건 (강화)

1. Grafana Explore에서 `{env="prod", module="batch"}` batch 로그가 조회된다.
2. staging/canary에서 synthetic 실패를 발생시켜 `"Batch task execution failed"` ERROR와 stacktrace를 확인한다. production에는 진단용 예외 코드를 배포하지 않는다.
3. production에서는 기존 실제 오류 또는 안전한 운영 event의 correlation만 확인한다.
4. 오류가 있었다면 Sentry에서도 동일 예외 event를 확인한다.

## 현재 상태

이 세션에서는 prod 호스트 / Grafana / Loki 접근이 없어 Step 1~5의 런타임 증거를 수집할 수 없다.
위 절차는 prod 접근 권한이 있는 운영자가 실행해 원인 확정 후 해당 1개 지점만 수정한다.
코드 측(중앙 ErrorHandler)은 이미 적용되어, Loki 수집이 복구되면 스케줄 실패 ERROR가 즉시 조회 가능한 상태다.
