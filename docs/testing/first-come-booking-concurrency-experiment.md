# 선착순 예매 재고 경쟁 실험 운영 기록

> 상태: dev 배포 대기
>
> 기준 PR: `feat/stock-contention-experiment` → `develop`
>
> 최종 수정: 2026-09-08

## 1. 목적과 범위

이 실험은 동일한 예매 생성 경로에서 재고 차감 방식만 바꿔, 정합성을 지키는 네 전략의 **상대 성능**을 비교한다. dev 애플리케이션과 prod가 같은 서버 사양을 사용하고, RDS는 공유한다. 현재 prod 트래픽은 매우 낮다는 운영 전제를 두되, RDS 신호를 함께 기록해 외부 부하가 결과를 왜곡하지 않았는지 확인한다.

이 결과는 shared RDS 환경의 상대 비교다. 전용 RDS/전용 runner를 쓰지 않으므로 절대 최대 TPS나 안정적인 p99 capacity를 주장하는 실험은 아니다.

### 비교 전략

| 전략 | 재고 경쟁 방식 | 고정 파라미터 |
| --- | --- | --- |
| `PESSIMISTIC` | Schedule row `FOR UPDATE` 후 조건부 차감 | 공통 transaction timeout 30초 |
| `OPTIMISTIC` | `schedule.version` CAS 실패 시 전체 transaction 재시도 | 최대 50회, backoff 1ms |
| `REDIS` | Redis token lock을 획득한 뒤 DB 조건부 차감 | acquire 30초, lease 60초, poll 10ms |
| `ATOMIC` | 재고 조건을 포함한 단일 `UPDATE` | 사전 재고 read 없음 |

공통 경로는 member 조회, schedule/performance 메타데이터 검증, 예매 가능 시간 확인, 가격 계산, Booking INSERT다. 전략마다 재고 경쟁 부분 외의 비즈니스 동작을 바꾸지 않는다.

## 2. 실행 경로와 안전 경계

실험 endpoint는 다음 하나다.

```text
POST /api/internal/experiments/stock-contention/{strategy}/bookings
```

`dev & !prod` profile과 `booking.experiment.enabled=true` 조건을 모두 통과해야 bean과 controller가 생성된다. 현재 이 값은 임시 dev 실험을 위해 코드에서 고정했고, prod profile에서는 endpoint가 생성되지 않는다.

요청은 로컬 k6에서 dev API로 **한 번만** 보낸다.

```text
local k6 ── HTTPS ──> api-dev.beatlive.kr ──> dev apis ──> shared RDS
```

Grafana 지표까지 보려면 로컬 k6가 만든 지표만 SSH tunnel을 통해 dev Alloy로 전달한다. 이 연결은 부하 요청을 추가로 보내지 않는다.

```text
local k6 ── OTLP metrics ──> localhost:4327 ── SSH tunnel ──> dev Alloy ──> Grafana Cloud
```

실험은 기존 예매 endpoint, guest endpoint, 실제 payment/SMS/Slack 호출을 사용하지 않는다. 실험 service는 Booking과 Schedule만 다룬다.

## 3. 고정된 workload

`load-tests/k6/lib/budgets.js`에 버전 `v1`으로 고정되어 있어 실행 환경변수로 RPS나 duration을 바꿀 수 없다.

| profile | 대상 schedule | offered workload | 기대 결과 |
| --- | --- | --- | --- |
| `warmup` | 16 (`FIRST`) | 5 RPS × 180초 = 900건 | accepted 900, sold-out 0 |
| `flash` | 17 (`SECOND`) | 200 RPS × 1초 = 200건 | accepted 100, sold-out 100 |

두 profile 모두 동일한 local-only `cases.json`의 단일 `ROLE_MEMBER` token, 예약자 이름, 전화번호, 1매 요청을 사용한다. token 원문은 Git, metric label, JSON summary metadata에 남기지 않는다.

flash는 constant-arrival-rate이며 200 VU가 곧 200개의 동시 DB transaction이라는 뜻은 아니다. `dropped_iterations > 0`이면 그 run은 무효다.

## 4. 변인 통제 계약

### 고정하는 것

| 구분 | 계약 |
| --- | --- |
| application | 동일 Git SHA, 동일 dev image, 동일 JVM 및 `DB_HIKARI_MAX_POOL_SIZE=10` |
| server | dev/prod와 동일 사양의 서버, 실험 중 배포 금지 |
| request | 동일 endpoint, token, body, `purchaseTicketCount=1`, request timeout 35초 |
| fixture | schedule 16 warmup stock 900, schedule 17 flash stock 100, `sold_ticket_count=0`, `version=0`에서 시작 |
| ordering | 네 전략을 5개 randomized complete block 순서로 회전 |
| cache | 모든 strategy에서 동일한 warmup을 먼저 실행하고 60초 quiet period 후 flash 실행 |
| cleanup | strategy run 종료 후 실험 booking만 삭제하고 두 schedule의 stock/version을 초기 상태로 복원 |
| observability | 같은 30초 Alloy scrape, 동일 Grafana dashboard/selector, k6 `test_id`로 run 분리 |

### 관측하고 무효화하는 것

prod 트래픽이 낮다는 전제는 실험을 허용하는 운영 판단이며, “RDS가 완전히 격리됐다”는 뜻은 아니다. 각 run 전후의 RDS CPU, FreeableMemory, connections, read/write latency, IOPS와 DiskQueueDepth를 기록한다. baseline에서 유의하게 벗어나거나 batch·배포·backup이 겹친 run은 결과표에서 제외한다.

RDS memory는 고정 `128MiB`를 하한으로 사용하지 않는다. `db.t3.micro`의 2026-09-10 직전 7일 관측치는 FreeableMemory p1 약 96.8MiB, p5 약 102.6MiB, 중앙값 약 118.3MiB였으므로 `128MiB`는 정상 상태까지 중단시키는 기준이다. 이 값은 AWS 공통 사양이 아니라 현재 instance·parameter group·상시 연결을 포함한 운영 baseline이며, instance class나 parameter group이 바뀌면 다시 산정한다.

- 시작 조건: quiet window 10분의 FreeableMemory 중앙값이 100MiB 이상이고 지속 하락하지 않으며, SwapUsage가 안정적이어야 한다.
- 중단 조건: FreeableMemory가 5분 연속 96MiB 미만이거나, 실험 직전 10분 중앙값보다 20% 이상 낮은 상태가 5분 지속된다.
- 중단 조건: SwapUsage가 실험 직전 baseline보다 32MiB 이상 증가한 뒤 계속 상승한다.
- 판정 방식: FreeableMemory 또는 SwapUsage의 순간값 하나만으로 run을 폐기하지 않고 CPU, Hikari pending/timeout, latency와 함께 본다.

dev cAdvisor는 활성화한다. Docker Compose service label만 보존해 `apis`, `admin`, `batch`, `redis`, `nginx`, `alloy`의 CPU·memory를 dev/prod 모두 같은 기준으로 본다. 모든 Docker label을 보내지 않아 series cardinality를 제한한다.

### 완전히 초기화하지 않고 관측하는 것

- shared RDS의 아주 작은 외부 read/write
- shared RDS의 MySQL buffer pool page 배치와 전역 counter에 섞이는 외부 접근
- 로컬 Mac runner의 CPU·network jitter
- 장시간 실행 중 변하는 JVM JIT/GC와 OS page cache

이들은 강제로 초기화하지 않는다. 애플리케이션을 매 run 재시작하면 비교 대상이 steady-state lock 전략이 아니라 cold-start가 되고, 애플리케이션 재시작으로 shared RDS buffer pool도 초기화되지 않는다. 동일 프로세스에서 고정 warmup, randomized block, 5회 반복을 사용하고 run별 Grafana/JSON artifact를 함께 남긴다. 전용 RDS와 전용 runner를 만들기 전에는 절대 capacity 결론을 내리지 않는다.

## 5. fixture reset 계약

reset은 실험의 일부다. `stock-contention` reset API는 만들지 않는다.

각 strategy run 전에 DB 작업자는 다음 allowlist만 대상으로 reset한다.

1. schedule `16`, `17`에 속하면서 실험의 token/user와 예약자 식별값으로 생성된 booking 및 종속 row를 삭제한다.
2. schedule `16`을 `total_ticket_count=900`, `sold_ticket_count=0`, `version=0`으로 복원한다.
3. schedule `17`을 `total_ticket_count=100`, `sold_ticket_count=0`, `version=0`으로 복원한다.
4. schedule `16`, `17` Redis lock key가 남아 있지 않은지 확인한다.
5. 두 schedule의 stock/version과 실험 booking count를 read-back한다.

전체 `booking` 삭제, `TRUNCATE`, production schema 접근은 금지한다. reset 직후에는 최소 60초 기다려 cleanup write가 flash 구간에 섞이지 않게 한다.

warmup이 만든 900 booking은 flash가 끝날 때까지 유지한다. 그래야 모든 strategy가 동일한 warm-cache/동일 write-table 상태에서 flash를 받는다. cleanup은 warmup과 flash를 모두 끝낸 뒤 수행한다.

## 6. 한 strategy run 절차

1. 배포된 `develop` SHA와 dev apis health를 확인한다.
2. Grafana `90 Load Test`에서 environment를 `dev`, RDS identifier를 `beat-prod-database`로 둔다.
3. SSH tunnel을 한 번 연다. k6 지표를 대시보드에 남기는 용도이며 HTTP 부하는 직접 dev API로 간다.
4. 최초 배포 후 고정 settle과 10분 baseline을 확보한다. 측정 block 중 재시작·배포가 발생하면 해당 run을 폐기한다.
5. fixture reset/read-back 후 60초 quiet period를 둔다. 직전 quiet baseline 10분의 RDS FreeableMemory 중앙값과 SwapUsage 추세, node/container/GC/heap/Hikari 및 InnoDB 전역 counter의 pre snapshot을 저장한다.
6. 해당 strategy로 warmup을 실행한다. accepted 900, dropped 0이어야 하며 warmup booking은 flash가 끝날 때까지 유지한다.
7. 60초 quiet period 후 flash schedule 17의 stock/version을 다시 read-back한다.
8. 동일 strategy로 flash를 실행한다. accepted 100, sold-out 100, dropped 0이어야 한다.
9. DB invariant, Redis lock, InnoDB post counter와 서버 post snapshot을 확인하고 local JSON summary 및 Grafana URL/time range를 보관한다.
10. drain 후 최소 90초 cooldown을 두고 연속 scrape에서 baseline 복귀를 확인한 다음 cleanup/reset한다.

InnoDB는 `Innodb_buffer_pool_read_requests`, `Innodb_buffer_pool_reads`, `Innodb_buffer_pool_read_ahead`, `Innodb_buffer_pool_read_ahead_evicted`, `Innodb_buffer_pool_pages_dirty`를 같은 순서로 기록한다. 이 값은 RDS 전역 누적값이므로 strategy별 물리 I/O로 귀속하지 않고 외부 개입과 cache 상태를 설명하는 정황 증거로만 사용한다. fixture 세 행의 `SELECT *`는 buffer pool 전체를 동일하게 만드는 pre-touch로 간주하지 않는다.

Mac runner에서는 `sntp time.apple.com`으로 offset만 기록한다. `sntp -sS`는 시스템 시각을 slew/set할 수 있어 사용하지 않는다. 같은 Mac·전원·네트워크에서 `caffeinate -i k6 run --out opentelemetry stock-contention.js`를 사용하고 모델, macOS, k6 version, 유·무선, 전원, offset, 전후 runner CPU, dropped, attempts를 기록한다. `dropped_iterations >= 1`, unexpected 응답, 네트워크 단절 또는 설정 arrival rate를 만들지 못한 runner saturation이 있으면 새 `TEST_ID`로 재실행한다. 전체 Mac CPU 80%만으로는 단독 폐기하지 않는다.

전략 순서는 아래 다섯 block을 사용한다.

| block | 순서 |
| ---: | --- |
| 1 | PESSIMISTIC → OPTIMISTIC → REDIS → ATOMIC |
| 2 | OPTIMISTIC → REDIS → ATOMIC → PESSIMISTIC |
| 3 | REDIS → ATOMIC → PESSIMISTIC → OPTIMISTIC |
| 4 | ATOMIC → PESSIMISTIC → OPTIMISTIC → REDIS |
| 5 | PESSIMISTIC → REDIS → ATOMIC → OPTIMISTIC |

따라서 본 측정은 strategy별 유효 flash run 5개다. 초기 smoke/warmup 검증 run은 본 결과에 넣지 않는다.

## 7. 판정 기준

### 정합성: 하나라도 실패하면 해당 run 폐기

```text
warmup: accepted=900, sold_out=0
flash:  accepted=100, sold_out=100
overselling=0
negative stock=0
duplicate experiment booking=0
unexpected=0
conflict_exhausted=0
lock_timeout=0
request timeout=0
dropped_iterations=0
```

### 자원: 즉시 중단 또는 결과 제외

- 배포, batch job, backup이 run 중 시작됨
- Hikari acquire timeout이 발생함
- flash 종료 60초 뒤에도 Hikari pending이 지속되며 saturation/timeout이 동반됨
- RDS FreeableMemory가 5분 연속 96MiB 미만이거나, pre-run 10분 중앙값보다 20% 이상 낮은 상태가 5분 지속됨
- RDS SwapUsage가 pre-run baseline보다 32MiB 이상 증가한 뒤 계속 상승함
- RDS CPU, connection, latency, IOPS 또는 DiskQueueDepth가 baseline에서 비정상적으로 이탈함
- gp2 RDS에서만 BurstBalance가 사전 정의한 허용치 아래로 하락함
- dev node 또는 apis container가 지속적으로 CPU/memory pressure를 보임
- Grafana Alloy remote-write failure/pending이 지속 증가함

RDS의 CloudWatch는 60초 해상도이므로 1초 flash의 원인을 단독으로 귀속하는 지표가 아니다. run validity를 판단하는 보조 증거로 사용한다.

## 8. 수집 지표와 source of truth

| 질문 | source of truth | 보조 관측 |
| --- | --- | --- |
| 정합성/oversell/중복 | DB read-only invariant query | k6 outcome counter |
| accepted TPS, exact outcome, p50/p95/p99, drain | local k6 JSON summary | 90 Load Test k6 panels |
| 서버 HTTP RPS | Spring actuator metric | 90 Load Test panel 5 |
| JVM, Hikari | `02 JVM & Hikari`, 90 panels 6~8 | app metric raw series |
| dev container CPU/memory | `04 Infrastructure` cAdvisor panels | node CPU/memory |
| RDS CPU/memory/connection/I/O | `03 Shared RDS / MySQL`, 90 panels 9~13 | CloudWatch 60s points |
| Redis 상태 | `04 Infrastructure` Redis panels | experiment response lock timeout |

MySQL exporter는 아직 별도 read-only DSN 설정이 없으므로 buffer-pool physical-read/read-request 패널은 `No data`가 정상이다. RDS IOPS/latency/queue는 CloudWatch로 확인한다. paging을 핵심 결론으로 쓰려면 MySQL exporter를 먼저 구축해야 한다.

## 9. artifact와 결과 보고

각 run마다 다음을 보관한다.

- `summary-<test_id>-warmup.json`, `summary-<test_id>-flash.json`
- test ID, deployed Git SHA, strategy, block, 실행 시각
- fixture reset read-back 결과와 DB invariant 결과
- InnoDB buffer-pool 전역 counter의 pre/post 값
- Mac 모델, macOS·k6 version, 유·무선, 전원, clock offset, 전후 runner CPU, dropped/attempts
- settle/baseline, warmup, flash, drain, cooldown과 baseline 복귀 snapshot
- Grafana `90 Load Test`, `02 JVM & Hikari`, `03 Shared RDS / MySQL`, `04 Infrastructure`의 UTC time range
- baseline/invalid reason/cooldown recovery 기록

결과표는 strategy별 유효 5회 run의 median, min-max, invalid run 이유를 함께 적는다. 200건 flash의 p99는 참고값이며 대표 수치로 사용하지 않는다.

| strategy | valid n | accepted TPS median | accepted p95 median | drain median | invalid runs | note |
| --- | ---: | ---: | ---: | ---: | --- | --- |
| PESSIMISTIC | | | | | | |
| OPTIMISTIC | | | | | | |
| REDIS | | | | | | |
| ATOMIC | | | | | | |

## 10. 실험 종료와 원복

실험 완료 후에는 다음을 분리해 수행한다.

1. 마지막 fixture cleanup과 DB read-back
2. experiment merge commit revert 및 dev 재배포
3. dev Alloy/Grafana 상태가 revert된 Git 설정과 일치하는지 확인
4. Mimir recording rule, dashboard, DB `schedule.version`은 app rollback만으로 지워지지 않으므로 필요 여부를 별도 확인

`rollback-dev`는 앱 image rollback일 뿐 DB fixture와 Grafana Cloud의 외부 상태를 자동 정리하지 않는다.
