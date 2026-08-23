# 예매 마감은 누가 책임져야 할까

> 인메모리 스케줄러에서 계산형 예매 상태로 전환하기 위한 ADR

- 상태: 채택·구현 완료
- 관련 이슈: [#428 예매 가능 상태를 DB 시각 기반 계산형 구조로 전환](https://github.com/TEAM-BEAT/BEAT-SERVER/issues/428)
- 범위: 예매 가능 여부 판정과 공연 마감 처리

먼저 결론부터 말하면, **예매 가능 여부는 저장해 두었다가 갱신할 상태가 아니라 현재 데이터로 계산할 값이다.** 각 스케줄에 `booking_close_at`을 저장하고, 다음 조건을 만족할 때만 예매를 허용한다.

```text
예매 가능 = DB 현재 시각 < booking_close_at
           AND 판매 수량 < 전체 수량
```

예매 API는 스케줄 락을 얻은 뒤 이 조건을 검사한다. 조회 API도 같은 규칙으로 기존 `isBooking` 응답을 계산한다. 따라서 공연별 마감 타이머와 마감 상태를 보정하는 batch가 필요하지 않다. 이 문서에서는 이 방식을 **Option A3: 계산형 예매 상태(Derived Availability)**라고 부른다.

바뀌는 핵심을 먼저 비교하면 다음과 같다.

| 상황 | 기존(As-Is) | 변경 후(To-Be) |
|---|---|---|
| 공연 마감 | 정해진 시각에 batch가 `is_booking=false`로 변경해야 한다. | 별도 작업 없이 현재 시각과 `booking_close_at`을 비교한다. |
| 공연 시간 연장 | 기존 작업을 취소·재등록하고 닫힌 상태를 다시 열어야 한다. | `booking_close_at`만 바꾸면 다음 조회와 예매부터 바로 반영된다. |
| batch 장애 | 유실된 작업을 찾아 재등록하거나 상태를 보정해야 한다. | 예매 판정에 영향이 없으며 DB 복구 후 즉시 같은 규칙으로 계산한다. |

## 1. 결정 당시 문제 상황

결정 당시 예매 마감 작업은 batch 서버의 메모리에 예약됐다.

```text
공연 생성·수정
    ↓
batch가 공연 종료 시각 계산
    ↓
ScheduledFuture를 메모리에 등록
    ↓
종료 시각에 is_booking=false
```

구현은 단순하지만 batch 서버가 정상적으로 계속 실행된다는 전제에 기대고 있다.

- 작업이 `ConcurrentHashMap<Long, ScheduledFuture<?>>`에만 남았다. Batch가 재시작되면 예약 정보도 사라졌다.
- 같은 스케줄 ID가 이미 등록돼 있으면 다시 예약하지 않았다. 공연 일시나 러닝타임이 바뀌어도 예전 종료 시각이 남을 수 있었다.
- 기존 `findPendingSchedules()`는 `isBooking=true`인 전체 공연을 읽어 메모리 상태와 비교했다.
- 기존 `closeBooking()`은 실제 마감 시각이 지났는지 다시 확인하지 않고 상태를 바꿨다.
- API 서버는 실제 스케줄러를 실행하지 않는데도 no-op 구현을 통해 스케줄러 인터페이스에 의존했다.

더 큰 문제는 `is_booking` 하나에 서로 다른 두 의미가 섞여 있다는 점이다.

```text
시간상 예매 기간인가?
재고가 남아 있는가?
```

결정 당시 회원·비회원 예매는 스케줄 행을 잠그고 재고를 확인했지만 마감 시각은 직접 확인하지 않았다. 현재 구현은 lock 획득 후 DB 시각 기준 마감과 재고를 모두 검사한다.

- [MemberBookingService](../../apis/src/main/java/com/beat/apis/booking/application/MemberBookingService.java#L43)
- [GuestBookingService](../../apis/src/main/java/com/beat/apis/booking/application/GuestBookingService.java#L40)
- [`Schedule.canPurchase()`](../../domain/src/main/kotlin/com/beat/domain/schedule/model/Schedule.kt)

그 결과 batch가 늦거나 멈추면 공연이 끝난 뒤에도 예매가 성립할 수 있다. 반대로 매진으로 `is_booking=false`가 된 공연은 티켓이 취소되거나 수량이 늘 때 다시 열어야 하는데, 이때 마감 시각을 함께 확인하지 않으면 이미 끝난 공연이 다시 열릴 수 있다.

BEAT처럼 특정 공연에 트래픽이 집중되는 서비스에서는 “마감 작업이 실행됐는가”보다 “어떤 장애 중에도 마감과 재고 규칙이 지켜지는가”가 중요하다.

- **Mission Critical:** 마감 이후 판매와 초과 판매를 예매 트랜잭션에서 막아야 한다.
- **Fault Tolerance:** batch 재시작이나 작업 유실이 판매 정합성에 영향을 주면 안 된다.
- **Resilience:** 장애 복구를 위해 마감 작업을 재등록하거나 밀린 상태를 보정할 필요가 없어야 한다.
- **트래픽 확장성:** 같은 시각에 많은 공연이 끝나도 대량 상태 변경이 DB에 몰리면 안 된다.

이번 결정의 기준은 다음과 같다.

> 저장된 사실만 신뢰하고, 그 사실로 계산할 수 있는 상태는 중복 저장하지 않는다.

## 2. 대안 검토

| 대안 | 장점 | 판단 |
|---|---|---|
| 기존 인메모리 스케줄러 | 구현이 단순하고 정해진 시각에 바로 실행된다. | 서버 재시작, 중복 작업, 공연 시간 변경에 취약해 제외한다. |
| DB polling batch(A2) | 현재 MySQL과 batch만으로 유실된 마감 상태를 복구할 수 있다. | 정확성은 높일 수 있지만 계산 가능한 `is_booking`을 계속 저장하고 보정해야 한다. 조회 지연, polling 부하, 잠금 설계도 남아 있어 최종안에서 제외한다. |
| DB Job Queue 또는 Outbox | 실행 이력, 재시도, 감사 기록이 필요한 작업에 적합하다. | 예매 마감 자체는 외부 부수 효과가 없는 계산이므로 작업 레코드를 만들 이유가 없다. |
| SQS 또는 EventBridge Scheduler | 메시지 보존, 재시도, 소비자 확장에 유리하다. | DB 저장과 메시지 발행 사이의 정합성을 위해 Outbox가 추가로 필요하고, 공연 수정·취소 때 외부 예약도 맞춰야 한다. |
| Quartz, MySQL Event Scheduler, Redis 지연 큐 | 예약 실행을 영속화할 수 있다. | 계산으로 해결할 문제에 별도 스케줄링 시스템과 운영 책임을 추가한다. |
| **계산형 예매 상태(A3)** | 마감 이벤트 없이 현재 시각, 마감 시각, 재고만으로 언제나 같은 답을 얻는다. | 예매 명령과 조회가 같은 계산 규칙을 사용하도록 구현하고 검증해야 한다. **선택한다.** |

A3를 선택한 이유는 단순하다. 예매 마감 시각이 지났다고 해서 반드시 실행해야 할 외부 작업은 없다. 판매 가능 여부만 달라지며, 이 값은 현재 시각과 저장된 데이터로 계산할 수 있다.

SQS의 내구성이 부족해서 제외한 것은 아니다. SQS Standard Queue는 메시지를 한 번 이상 전달할 수 있으므로 소비자의 멱등성이 필요하고, Delay Queue의 지연 시간은 최대 15분이다. 며칠 뒤의 공연 마감을 예약하려면 EventBridge Scheduler 같은 추가 구성이 필요하다. [AWS SQS at-least-once delivery](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/standard-queues-at-least-once-delivery.html), [AWS SQS Delay Queue](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-delay-queues.html)

반대로 외부 결제, 은행망, 알림, 정산처럼 반드시 실행해야 하는 부수 효과에는 Outbox와 SQS가 잘 맞는다. 이런 작업은 멱등키, 재시도, DLQ, 사후 대조까지 포함해 별도 ADR에서 다룬다.

## 3. 해결 방법 및 이유

### 하나의 규칙으로 명령과 조회를 처리한다

`schedule.booking_close_at DATETIME(6)`을 예매 마감 기준값으로 저장한다.

```text
booking_close_at = performance_date + running_time

bookable = (DB 현재 시각 < booking_close_at)
           AND (sold_ticket_count < total_ticket_count)
```

BEAT에서는 공연이 시작된 뒤에도 예매하는 사용자가 적지 않다. 따라서 예매 마감은 공연 시작 시각이 아니라 **각 회차의 공연 종료 시각**으로 정했다. `performance_date`는 회차별 시작 시각이고 `running_time`은 공연이 공유하는 러닝타임이므로, 같은 공연이라도 회차별 `booking_close_at`은 다르다.

`booking_close_at`은 다른 테이블의 값을 매번 조인해 계산하지 않기 위한 도메인 값이다. MySQL generated column은 다른 테이블의 `running_time`을 참조할 수 없으므로 사용할 수 없다.

기존 API의 `isBooking` 필드는 없애지 않는다. 다만 DB의 `is_booking` 저장값을 그대로 반환하지 않고, 조회 SQL이 읽은 `booking_close_at`과 재고, 쿼리 시작 시점의 DB 현재 시각으로 계산한다.

조회 구현도 한 가지로 고정한다. DB 시각을 먼저 조회해 애플리케이션으로 가져오거나 `LocalDateTime.now()`를 쿼리 파라미터로 전달하지 않는다. `ScheduleAvailabilityReadPort`의 단일 native SQL이 `CURRENT_TIMESTAMP(6)`을 한 번 평가하고 `isBooking`을 계산한 `ScheduleAvailabilityReadModel`을 반환한다.

```sql
WITH query_clock AS (
    SELECT CURRENT_TIMESTAMP(6) AS evaluated_at
)
SELECT
    s.id,
    s.performance_date,
    s.schedule_number,
    s.total_ticket_count - s.sold_ticket_count AS available_ticket_count,
    (
        c.evaluated_at < s.booking_close_at
        AND s.sold_ticket_count < s.total_ticket_count
    ) AS is_booking,
    c.evaluated_at
FROM schedule s
CROSS JOIN query_clock c
WHERE s.performance_id = :performance_id;
```

MySQL은 현재 시각 함수를 SQL 문장 시작 시점에 한 번 평가한다. CTE의 `evaluated_at`을 모든 행이 공유하므로 목록 중 일부만 마감 전으로 판정되는 일이 없다. 별도의 시각 조회도 없어 네트워크 왕복과 두 SQL 사이의 시간차가 생기지 않는다.

현재 공연 상세 조회는 read model의 `evaluated_at.toLocalDate()`와 공연 일자를 `apis.schedule.application`의 `calculateDueDate`(`DueDate.kt`)에 전달한다. 따라서 `dueDate`와 `isBooking`은 같은 DB 시각을 기준으로 한다. 조회 API는 상태를 변경하지 않으므로 이 SQL에는 `FOR UPDATE`를 사용하지 않는다.

JPQL의 `CURRENT_TIMESTAMP`로도 조건식을 만들 수 있지만 소수점 이하 6자리 정밀도를 쿼리에 명시하기 어렵고 JPA provider와 MySQL dialect의 변환에 의존한다. 이 조회는 MySQL의 `CURRENT_TIMESTAMP(6)` 계약을 그대로 사용하는 native SQL로 구현한다.

이렇게 하면 별도 상태 변경 없이 결과가 바로 달라진다.

- 공연 시간이 연장돼 새 마감 시각이 미래이고 재고가 남아 있으면 다시 예매할 수 있다.
- 마감 시각을 앞당겨 새 시각이 이미 지났다면 즉시 예매할 수 없게 된다.
- 매진된 공연은 시간이 연장돼도 재고가 없으므로 열리지 않는다.
- 마감 이후 티켓이 취소돼도 시간이 지났으므로 열리지 않는다.

향후 운영자가 판매를 강제로 중지하는 기능이 생기면 `sales_enabled` 같은 별도 상태를 추가한다. 시간과 재고로 계산할 수 없는 운영 정책까지 `is_booking`에 다시 섞지 않는다.

### 예매 API는 락을 얻은 뒤 마감 시각을 다시 판정한다

회원·비회원 예매는 다음 순서로 처리한다.

```text
락과 무관한 회원 정보 선검증
    ↓
스케줄 행을 SELECT ... FOR UPDATE로 잠금
    ↓
같은 트랜잭션·물리 커넥션에서 DB 시각으로 마감 여부 재검사
    ↓
재고 확인 및 차감
    ↓
예매 저장 후 커밋
```

핵심 쿼리는 다음과 같다.

```sql
SELECT CURRENT_TIMESTAMP(6) < booking_close_at AS bookable
FROM schedule
WHERE id = :schedule_id
FOR UPDATE;
```

MySQL의 `CURRENT_TIMESTAMP(6)`은 SQL 문장이 시작될 때 결정된다. 첫 번째 locking read가 다른 트랜잭션을 기다리는 동안 마감 시각이 지날 수 있으므로, **락을 얻은 뒤 두 번째 SQL을 시작한 시점**을 예매 판정 시점으로 정의한다. 이때 마감 전이면 예매를 허용하고, 마감 시각과 같거나 지난 경우에는 거절한다. [MySQL Date and Time Functions](https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html)

두 쿼리는 반드시 같은 트랜잭션과 물리 커넥션에서 실행한다. 중간에 비동기 실행이나 `REQUIRES_NEW` 트랜잭션을 끼우지 않는다.

API는 MySQL 기본 격리 수준인 `REPEATABLE READ`를 유지한다. 일반 SELECT는 첫 consistent read의 스냅샷을 계속 볼 수 있지만, `SELECT ... FOR UPDATE`는 locking read이므로 최신 커밋 행을 기준으로 잠근다. 두 번째 쿼리도 locking read로 실행해 이전 스냅샷의 `booking_close_at`을 사용하지 않는다. [MySQL Consistent Nonlocking Reads](https://dev.mysql.com/doc/refman/8.0/en/innodb-consistent-read.html), [MySQL Locking Reads](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html)

`DATETIME(6)`과 `CURRENT_TIMESTAMP(6)`은 소수점 이하 6자리까지 비교하므로 밀리초 경계를 포함한다. 이는 저장과 비교의 정밀도이지 DB 서버 시계 자체의 정확도를 보장한다는 뜻은 아니다. DB 시계는 NTP 동기화와 운영 모니터링으로 관리한다. [MySQL Fractional Seconds](https://dev.mysql.com/doc/refman/8.0/en/fractional-seconds.html)

현재 예매 서비스는 락과 무관한 회원 조회를 스케줄 락 전에 수행한다. 공연 정보는 잠긴 스케줄의 `performance_id`를 기준으로 락 후에 조회한다. 재고와 마감 판정은 반드시 같은 스케줄 락 안에 둔다.

### 공연 수정은 `booking_close_at`까지 하나의 트랜잭션으로 바꾼다

공연 일시나 러닝타임을 수정할 때는 영향받는 스케줄을 ID 오름차순으로 잠근다. 같은 트랜잭션에서 공연 정보와 `booking_close_at`을 함께 갱신한다.

예매와 공연 수정이 동시에 실행되면 스케줄 락을 먼저 얻은 트랜잭션부터 처리된다.

- 공연 수정이 먼저라면 예매 API는 새 `booking_close_at`을 본다.
- 예매가 먼저라면 기존 마감 시각을 기준으로 예매를 확정한 뒤 공연 수정이 진행된다.

별도의 `is_booking` 갱신은 없다. 따라서 공연 시간이 연장되면 재고가 남아 있는 한 조회와 다음 예매 요청에서 자동으로 다시 열린다.

### 마감 batch를 제거한다

다음 구성은 더 이상 필요하지 않다.

- 공연별 `ScheduledFuture` 맵과 재등록 로직
- `ScheduleBookingCloseJobPort`
- `NoOpScheduleBookingCloseJobConfig`
- 마감 스케줄러와 마감 job
- `findPendingSchedules()`와 `is_booking` 기반 마감 대상 조회

batch 애플리케이션 자체를 없애는 것은 아니다. 프로모션 관리와 티켓 정리 같은 기존 작업은 그대로 남기고, **예매 마감 작업만 제거한다.** 마감 대상 polling, `FOR UPDATE SKIP LOCKED`, 5초 주기, batch 크기, 마감용 복합 인덱스도 필요하지 않다.

### KST를 명시적인 시간 계약으로 사용한다

BEAT는 한국 공연을 다루고 기존 시간 데이터도 `LocalDateTime`으로 저장한다. 이번 작업에서는 전체 시간을 UTC로 바꾸지 않고 KST를 유지한다.

운영 RDS는 `Asia/Seoul`, dev와 Testcontainers는 `+09:00`으로 설정했다. 표기만 다르고 모두 KST다. 기존 커넥션은 연결될 때 받은 시간대를 계속 사용할 수 있으므로 API, batch, admin을 재시작한 뒤 각 애플리케이션의 실제 DB 연결로 `@@session.time_zone`을 확인한다.

<details>
<summary>KST 적용과 검증 기록</summary>

- 운영 RDS의 `time_zone`은 `Asia/Seoul`로 설정했다. 민감한 접속 정보는 비공개 runbook에서 관리한다.
- dev MySQL 컨테이너와 API, admin, batch의 Testcontainers에는 `--default-time-zone=+09:00`을 적용했다.
- `DatabaseSessionTimeZoneTest`는 JVM 기본 시간대와 관계없이 DB가 KST를 반환하는지 확인한다.
- `hibernate.jdbc.time_zone=Asia/Seoul`은 MySQL 세션 변수 자체를 바꾸지 않아 사용하지 않는다.
- 운영 스키마는 `ddl-auto=none`이므로 실제 컬럼 타입을 `SHOW CREATE TABLE`로 확인한다. `booking_close_at`은 `DATETIME(6)`으로 통일한다.
- 배포 전후 다음 쿼리로 시간대를 검증한다.

```sql
SELECT
    @@session.time_zone,
    @@global.time_zone,
    CURRENT_TIMESTAMP(6),
    UTC_TIMESTAMP(6);
```

다중 지역이나 해외 공연을 지원하게 되면 `Instant`와 UTC 저장을 별도 ADR에서 검토한다.

</details>

### 단계적으로 전환한다

1. `booking_close_at`을 `NULL` 허용 `DATETIME(6)` 컬럼으로 추가한다.
2. 공연·회차 생성/수정 요청만 잠시 막는다. 티켓 조회와 예매 트래픽은 계속 받는다. 구버전은 새 컬럼을 쓰지 않으므로 이 write quiescence 없이 rolling 배포하면 `NULL` 회차가 생길 수 있다.
3. 기존 데이터를 채우고 누락값과 계산 불일치가 0건인지 확인한 뒤 `NOT NULL`을 적용한다.
4. 새 애플리케이션의 INSERT가 구형 `is_booking` 컬럼 때문에 실패하지 않도록 임시 기본값 `TRUE`를 설정한다.
5. `booking_close_at`을 기록하고, 락 획득 후 DB 시각으로 재검사하며, 조회 시 계산형 `isBooking`을 반환하는 애플리케이션을 배포한다.
6. 모든 인스턴스가 새 버전이면 공연·회차 쓰기를 다시 연다. 롤백할 때는 쓰기를 다시 막고 `booking_close_at`을 `NULL` 허용으로 되돌린 뒤 구버전을 기동한다.
7. 롤백 기간이 끝나면 `is_booking` 컬럼을 제거한다.

### 테스트와 운영 지표로 경계를 확인한다

현재 자동화 테스트로 다음을 검증했다.

- 회원·비회원 예매가 DB 마감 판정을 통과하지 못하면 거절되는지 확인
- 락을 기다리는 동안 마감 시각이 지난 요청
- 마지막 한 장에 여러 요청이 동시에 들어오는 경우
- `REPEATABLE READ`에서 locking read가 최신 `booking_close_at`을 보는지 확인
- 마감 전·마감 후·매진 회차의 `isBooking` 계산과 마감 시각 연장 후 재개방
- 한 목록의 모든 행이 같은 `evaluated_at`을 사용하는지 확인
- DB 세션 시간대와 `CURRENT_TIMESTAMP(6)`이 KST 기준인지 확인
- 마감 스케줄러 의존성이 제거돼도 API와 batch 모듈이 정상 기동하는지 확인

배포 전에는 다음 경계 조건을 추가로 검증한다.

- 마감 직전, 정확한 마감 시각, 마감 직후의 `DATETIME(6)` 비교
- 공연 시간 단축 후 새 마감 시각이 지났다면 즉시 닫히는지 확인
- 매진 공연의 시간 연장과 마감 이후 티켓 취소가 닫힌 상태를 바꾸지 않는지 확인
- 조회 시각을 위한 별도 SQL 없이 한 번의 schedule read query로 결과를 만드는지 확인

운영에서는 다음 지표를 본다.

- 예매 성공·마감 거절·재고 부족 건수
- 스케줄 락 대기 시간, lock timeout, deadlock
- 한 스케줄에 요청이 몰릴 때 처리량과 p95·p99 응답시간
- 커넥션 풀 대기와 DB CPU
- `booking_close_at` 누락 및 원본 공연 정보와의 불일치 건수

배포 전 hot-row 부하 테스트로 변경 전후 수치를 비교한다. 정확성은 유지되더라도 락 점유 시간이 목표를 넘으면 먼저 락 안의 불필요한 조회를 줄인다. 그래도 부족할 때 원자적 조건부 재고 차감이나 reservation 모델을 다음 ADR에서 검토한다.

## 4. 기대 효과

| 관점 | 달라지는 점 |
|---|---|
| Mission Critical | 마감과 재고 규칙을 예매 트랜잭션에서 검사한다. DB가 응답하지 않으면 예매를 승인하지 않는 fail-closed 방식으로 정합성을 우선한다. |
| Fault Tolerance | 마감 이벤트나 `is_booking` 갱신 작업이 없어 유실·중복 실행·재시작으로 상태가 어긋날 경로가 사라진다. |
| Resilience | batch 복구와 밀린 작업 재처리 없이 DB가 정상화되는 즉시 현재 데이터로 올바른 예매 가능 여부를 다시 계산한다. |
| 트래픽 확장성 | 공연별 장기 타이머와 마감 시각의 대량 UPDATE가 사라진다. 조회 트래픽은 읽기 확장 전략을 적용할 수 있고, 쓰기 경합은 실제 예매가 몰리는 스케줄 행에만 제한된다. |
| 운영 복잡도 | 마감 polling 주기, batch 크기, `SKIP LOCKED`, 재시도와 마감 지연 지표를 운영하지 않아도 된다. 현재 MySQL만으로 규칙을 완결한다. |
| 사용자 경험 | 마감과 연장이 다음 batch 주기를 기다리지 않고 조회 결과에 즉시 반영된다. 기존 `isBooking` 응답 형식은 유지된다. |

이 결정이 티켓팅의 모든 확장 문제를 해결하는 것은 아니다. 같은 공연의 예매 요청은 현재 비관적 락으로 직렬화되므로 인기 공연에서는 해당 행이 병목이 될 수 있다. A3는 마감 상태 동기화 문제를 제거하는 결정이며, 재고 처리량은 부하 테스트 결과에 따라 별도로 개선한다.

또한 이 구조만으로 전체 서비스가 Mission Critical 수준에 도달하는 것은 아니다. 운영 전 다음 조건이 함께 필요하다.

- RDS Multi-AZ 또는 같은 수준의 자동 장애 조치
- 자동 백업, PITR, 정기 복구 훈련과 명확한 RPO/RTO
- DB 시계 동기화와 KST 세션 검증
- hot-row 처리량, p95·p99, lock timeout, deadlock 통과 기준
- 기존 데이터의 `booking_close_at` 채우기와 `NOT NULL` 적용 검증
- 외부 결제·은행망·알림에 대한 Outbox, 메시지 큐, 멱등 처리와 사후 대조

최종 선택은 **Option A3: 계산형 예매 상태**다. `booking_close_at`과 재고는 저장하되 `is_booking`은 계산한다. 실행할 필요가 없는 마감 이벤트를 없애면 장애 복구 대상도 함께 사라진다. 현재 인프라와 서비스 규모에서 정합성, 복원력, 확장성, 운영 비용을 가장 균형 있게 만족하는 방식이다.
