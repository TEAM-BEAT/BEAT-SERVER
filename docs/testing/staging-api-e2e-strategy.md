# BEAT Layer 2 Staging API E2E 전략

> **상태:** Proposed  
> **작성 기준:** 2026-08-26  
> **적용 대상:** `apps:api`를 중심으로 한 staging 배포 직후 검증  
> **관련 정본:** [`docs/architecture/architecture.md`](../architecture/architecture.md)

## 1. 결론

BEAT의 Layer 2 E2E는 모든 endpoint를 반복 검증하는 API 회귀 suite가 아니다. 실제 staging 배포물에 HTTPS로 접근해 다음 네 가지 critical user journey만 검증하는 얇은 배포 검증 계층으로 둔다.

1. Health와 공개 조회가 실제 gateway 및 runtime 설정을 통과한다.
2. 비회원이 공연을 조회하고 예매한 뒤 guest cookie로 조회·취소 또는 환불 요청한다.
3. 회원이 JWT로 예매하고 자신의 예매를 조회한 뒤 취소 또는 환불 요청한다.
4. 메이커 ticket lifecycle을 유료/무료, bulk 선택, 취소/환불/삭제, 재시도 관점에서 검증한다.

권장 구조는 `apps/api/src/e2eTest` 독립 source set과 `./gradlew e2eTest` 독립 task다. 새 Gradle product module은 만들지 않는다. 이는 11-module freeze를 유지하면서 E2E가 `apps:api`의 실제 외부 계약만 사용하게 한다.

테스트 데이터는 다음 하이브리드 전략을 사용한다.

- staging 전용 회원/메이커 identity만 사전 생성한다.
- 공연, 회차, 예매는 매 실행마다 API로 생성한다.
- 모든 생성값에 `e2e-{runId}` namespace를 넣는다.
- 정상 경로에서는 공개 API로 보상 정리하고, 누락 데이터는 TTL janitor가 제거한다.
- Slack은 capture webhook을 사용하고 CoolSMS는 주입 가능한 staging transport를 먼저 만든 뒤 capture mode를 사용한다. S3와 CloudFront는 staging 전용 실제 자원을 사용한다.

배포 직후 E2E 실패는 곧바로 자동 rollback하지 않는다. 초기에는 promotion 중단과 알림을 hard gate로 삼고, 애플리케이션 회귀가 확인된 경우 기존 수동 rollback workflow를 실행한다. flake rate와 분류 정확도가 안정화된 뒤에만 제한적 자동 rollback을 검토한다.

## 2. 현재 구조에서 확인된 제약

- 아키텍처 정본은 `apps:*`를 HTTP 진입점과 composition root로 정의하고 product module을 11개로 동결한다.
- 현재 `acceptanceTest`는 `apps:api`의 Spring context, Testcontainers, MockMvc를 사용한다.
- 현재 risk task는 `fast`, `integration`, `correctness`, `acceptance`, `openapi`를 사용한다.
- 현재 public booking API는 `/api/bookings/guest`, `/api/bookings/member`, 조회·취소·환불 endpoint를 제공한다.
- guest mutation은 cookie뿐 아니라 허용된 `Origin`도 필요하다.
- 메이커 ticket 상태 변경은 별도 admin runtime이 아니라 `apps:api`의 `/api/tickets/**`와 MEMBER 권한을 사용한다.
- management endpoint는 runtime별 별도 port/path를 사용하므로 API base URL과 health URL을 분리해야 한다.
- 공연 생성은 poster object의 S3 존재 여부를 확인하며 image key 환경 prefix는 현재 `dev`와 `prod`만 허용한다.
- repository에는 `deploy-dev.yml`, `deploy-prod.yml`만 있고 이름이 `staging`인 environment/workflow는 없다.

따라서 구현 전에 아래 한 가지를 반드시 결정한다.

> 현재 `dev`를 staging으로 운영하는가, 아니면 별도 `staging` GitHub Environment와 배포 workflow를 만들 것인가?

이 문서는 논리 환경명을 `staging`으로 표기한다. 실제 구현 시 팀 결정에 따라 `dev` secret/variable에 매핑하거나 별도 staging 환경을 만든다.

## 3. 목표와 비목표

### 3.1 목표

- 배포 artifact, reverse proxy/TLS, profile, secret, route policy가 함께 동작하는지 확인한다.
- MySQL과 Redis를 포함한 실제 staging wiring을 검증한다.
- 가장 중요한 예매 및 재고 상태 전이가 외부 API 관점에서 이어지는지 확인한다.
- 실패 시 어떤 배포를 중단하거나 되돌릴지 10분 안에 판단할 증거를 남긴다.
- 같은 suite를 반복 실행해도 결과와 데이터가 서로 충돌하지 않게 한다.

### 3.2 비목표

- 모든 endpoint, validation 조합, error code를 전수 검사하지 않는다.
- 동시성, lock ordering, overselling 한계를 staging E2E에서 부하로 증명하지 않는다.
- 외부 사업자의 전체 SLA를 배포 gate로 삼지 않는다.
- UI 브라우저 흐름이나 Kakao consent 화면을 검증하지 않는다.
- production 데이터나 production 외부 자원을 사용하지 않는다.

## 4. Layer 1 Acceptance와 Layer 2 Staging E2E 경계

| 검증 항목 | Acceptance/Testcontainers | Staging API E2E |
|---|---|---|
| domain invariant와 상태 전이의 모든 분기 | 필수 | 배포 위험을 대표하는 lifecycle cluster만 |
| overselling, lock ordering, transaction rollback | 필수 | 부하·race 재현 금지 |
| validation/error code/인가 matrix 전수 | 필수 | 401/403 대표 1건만 |
| Controller DTO serialization과 OpenAPI | 필수 | critical response field만 |
| MySQL/Redis adapter 동작 | 격리 container로 상세 검증 | 배포 환경 wiring 존재 여부 |
| JWT filter, guest cookie, Origin 정책 | MockMvc로 matrix 검증 | 실제 gateway를 통과하는 대표 요청 |
| TLS, DNS, reverse proxy, runtime profile, secret | 불가 | 필수 |
| S3 presigned URL과 CloudFront staging 전달 | adapter contract는 fake/mock | staging 자원으로 1건 |
| CoolSMS/Slack payload | capture fake contract | capture endpoint 도달 여부 |
| 재시도와 eventual consistency | 결정적 clock/fake로 상세 검증 | 제한된 polling으로 최종 상태만 |

판단 규칙은 간단하다.

> 동일 JVM 또는 Testcontainers에서 싸고 결정적으로 증명할 수 있으면 Acceptance로 보낸다. 배포된 프로세스와 실제 네트워크·설정·managed service의 결합에서만 드러나는 위험만 Staging E2E에 남긴다.

## 5. Critical User Journey 선별

### 5.1 선별 점수

후보 journey마다 각 항목을 0~2점으로 평가하고 합계 7점 이상만 Layer 2에 둔다.

| 기준 | 0점 | 1점 | 2점 |
|---|---|---|---|
| 매출·재고 영향 | 없음 | 간접 영향 | 예매/재고 직접 영향 |
| 경계 수 | 단일 process | DB 또는 인증 | gateway+인증+DB/Redis/외부 연동 |
| 배포 설정 민감도 | 낮음 | profile 영향 | secret, route, managed service 영향 |
| Acceptance 사각지대 | 거의 없음 | 일부 있음 | 실제 환경에서만 검증 가능 |
| 장애 감지 가치 | 낮음 | 부분 장애 감지 | 고객 핵심 여정 중단 감지 |

endpoint 수가 많다는 이유는 선별 기준이 아니다. 같은 위험을 공유하는 endpoint는 하나의 journey로 묶는다.

### 5.2 BEAT Top 4

| 우선순위 | Journey | 핵심 검증 | 예상 시간 |
|---|---|---|---:|
| P0 | Health + 공개 조회 smoke | management health, `/api/main`, 공연 상세, 회차 availability | 20초 |
| P0 | 비회원 예매 | public route, MySQL, Redis guest session, secure cookie, Origin, 재고 반영 | 60초 |
| P0 | 회원 예매 | 실제 MEMBER JWT, 소유권, 예매 생성·조회, 재고 반영 | 60초 |
| P0/P1 | 메이커 ticket lifecycle cluster | 유료/무료 분기, bulk 선택, `deletable`, 입금·환불·삭제, 재고·SMS 멱등성 | critical 120초, extended 추가 180초 |

#### Journey 1: Health와 공개 조회

1. `E2E_HEALTH_URL`에 접근해 HTTP 200과 필요한 health component를 확인한다.
2. `/api/main`이 200과 정상 envelope를 반환하는지 확인한다.
3. 이번 run에서 만든 공연의 `/api/performances/detail/{performanceId}`를 조회한다.
4. `/api/schedules/{scheduleId}/availability?purchaseTicketCount=1`에서 잔여 수량을 확인한다.

health는 readiness 판단, 공개 조회는 실제 API ingress 판단이다. 둘 중 하나로 다른 하나를 대체하지 않는다.
management endpoint는 인터넷에 공개하지 않는다. GitHub-hosted runner에서 접근할 수 없다면 private network의 self-hosted runner를 사용하거나, 인증된 최소 readiness endpoint만 별도로 노출한다.

#### Journey 2: 비회원 예매

1. run 전용 유료 공연과 회차를 메이커 token으로 만든다.
2. 고유한 이름·전화번호·생년월일로 `/api/bookings/guest`를 호출한다.
3. 201, `bookingId`, `CHECKING_PAYMENT`, `Set-Cookie`를 확인한다.
4. `/api/bookings/guest/retrieve`로 동일 booking을 확인한다.
5. 생성 전후 availability를 비교해 판매 수량이 정확히 1 증가했는지 확인한다.
6. `/api/bookings/refund`에 guest cookie와 staging Origin을 함께 보내 `REFUND_REQUESTED`를 확인한다.
7. 메이커가 `/api/tickets/refund`로 환불을 완료하고 availability가 복구되는지 polling한다.

#### Journey 3: 회원 예매

1. 사전 생성한 MEMBER token으로 `/api/bookings/member`를 호출한다.
2. 201과 `bookingId`를 확인한다.
3. `/api/bookings/member/retrieve`에서 방금 만든 booking만 run 식별자로 찾는다.
4. 무료 공연이면 `/api/bookings/cancel`, 유료 공연이면 `/api/bookings/refund`를 사용한다.
5. 최종 상태와 availability 복구를 확인한다.

비회원과 회원은 같은 booking use case를 공유하더라도 제거하면 안 된다. 실제 인증 수단이 Redis guest session cookie와 JWT로 다르기 때문이다.

#### Journey 4: 메이커 ticket lifecycle cluster

이 영역은 하나의 happy path로 축약하면 안 된다. BEAT의 booking은 다섯 상태를 가지며 최초 상태와 취소·삭제 가능 여부가 결제 금액에 따라 달라진다. 사용자 화면도 입금 여부 선택에 따라 즉시 취소와 환불 요청 중 다른 API를 호출하고, 메이커 화면은 입금 확인·환불 완료·삭제를 각각 bulk action으로 호출한다.

##### 4.1 상태와 재고의 정본

| 상태 | 의미 | 티켓 할당 상태 | 메이커 기본 조회 | 비고 |
|---|---|---:|---:|---|
| `CHECKING_PAYMENT` | 유료 예매 후 입금 확인 전 | 유지 | 노출 | 유료 예매의 최초 상태 |
| `BOOKING_CONFIRMED` | 무료 예매 또는 입금 확인 완료 | 유지 | 노출 | 무료 예매의 최초 상태 |
| `REFUND_REQUESTED` | 사용자가 계좌와 함께 환불 요청 | 유지 | 노출 | 메이커가 실제 송금 후 완료 처리 |
| `BOOKING_CANCELLED` | 취소 또는 환불 완료 | 비활성(이미 반환) | 노출 | 이후 메이커 삭제 가능 |
| `BOOKING_DELETED` | 메이커 목록에서 제거 | 비활성(이미 반환) | 숨김 | 명시적 상태 필터 요청도 거부 |

`BOOKING_CANCELLED`와 `BOOKING_DELETED`만 비활성 할당 상태다. 따라서 `REFUND_REQUESTED`에서 좌석을 먼저 반환하면 안 되고, 취소·환불 완료·삭제를 재시도해도 같은 수량을 두 번 반환하면 안 된다.

##### 4.2 사용자·메이커 action 허용행렬

`허용`은 상태 변경, `멱등`은 성공하되 상태·재고·SMS 추가 변경 없음, `거부`는 4xx와 무변경을 뜻한다. `비정상 도달`은 정상 API 생성 흐름에서는 만들어지지 않으므로 staging fixture로 억지 생성하지 않는다.

| 유/무료 | 현재 상태 | 사용자 취소 | 사용자 환불 요청 | 메이커 입금 확인 | 메이커 환불 완료 | 메이커 삭제 |
|---|---|---|---|---|---|---|
| 유료 | `CHECKING_PAYMENT` | 허용 → `BOOKING_CANCELLED`, 재고 반환 | 입금했다고 선택하면 허용 → `REFUND_REQUESTED`, 재고 유지 | 허용 → `BOOKING_CONFIRMED`, SMS 1회 | 거부 | 허용 → `BOOKING_DELETED`, 재고 반환 |
| 유료 | `BOOKING_CONFIRMED` | 거부 | 허용 → `REFUND_REQUESTED`, 재고 유지 | 멱등, SMS 추가 발송 없음 | 거부 | 거부, `deletable=false` |
| 유료 | `REFUND_REQUESTED` | 거부 | 같은 계좌는 멱등, 다른 계좌는 거부 | 거부 | 허용 → `BOOKING_CANCELLED`, 재고 반환 | 거부, `deletable=false` |
| 유료 | `BOOKING_CANCELLED` | 멱등 | 거부 | 거부 | 멱등 | 허용 → `BOOKING_DELETED`, 재고 추가 반환 없음 |
| 유료 | `BOOKING_DELETED` | 거부 | 거부 | 거부 | 거부 | 멱등, 목록에는 계속 숨김 |
| 무료 | `BOOKING_CONFIRMED` | 허용 → `BOOKING_CANCELLED`, 재고 반환 | 거부 | 멱등 | 거부 | 허용 → `BOOKING_DELETED`, 재고 반환 |
| 무료 | `BOOKING_CANCELLED` | 멱등 | 거부 | 거부 | 멱등 | 허용 → `BOOKING_DELETED`, 재고 추가 반환 없음 |
| 무료 | `BOOKING_DELETED` | 거부 | 거부 | 거부 | 거부 | 멱등, 목록에는 계속 숨김 |
| 무료 | `CHECKING_PAYMENT` | 비정상 도달 | 비정상 도달 | 비정상 도달 | 비정상 도달 | domain 정책상 삭제 가능하지만 Acceptance에서만 검증 |
| 무료 | `REFUND_REQUESTED` | 비정상 도달 | 비정상 도달 | 비정상 도달 | domain 정책상 완료 가능 | 메이커 삭제 거부; 모두 Acceptance에서 검증 |

행렬의 세부 오류 코드, 비정상 도달 상태, 다른 환불 계좌 재요청은 domain/Application Acceptance가 전수 검증한다. Staging E2E는 API만으로 자연스럽게 만들 수 있는 상태와 배포 결합 위험을 골라 검증한다.

##### 4.3 Layer 2에서 실행할 시나리오

배포 직후 hard gate와 수동·야간 extended suite를 분리한다.

| ID | 등급 | 시나리오 | 반드시 볼 결과 |
|---|---|---|---|
| `TL-C01` | P0, 매 배포 | 유료 booking 두 건 생성 → 한 건만 bulk 입금 확인 → 그 건을 사용자 환불 요청 → 메이커 환불 완료 → 완료 API 재시도 | 선택 건만 `BOOKING_CONFIRMED`, 미선택 건은 `CHECKING_PAYMENT`, SMS 선택 건당 1회, 환불 전 재고 유지, 완료 후 한 번만 복구 |
| `TL-C02` | P0, 매 배포 | 무료 booking 생성 → 삭제 전 일반·검색 조회에서 동일 ID와 `deletable=true` 확인 → maker 삭제 → 삭제 API 재시도 | 최초 `BOOKING_CONFIRMED`, SMS 불필요, 재고 한 번만 복구, 삭제 후 일반·검색 목록에서 계속 숨김 |
| `TL-E01` | P1, 수동/야간 | 유료 미확인 booking에서 사용자가 “입금 전” 선택 후 즉시 취소 → maker 삭제 | `BOOKING_CANCELLED → BOOKING_DELETED`, 취소 시 한 번만 재고를 반환하고 삭제 때는 추가 반환 없음 |
| `TL-E02` | P1, 수동/야간 | 유료 미확인 booking에서 사용자가 “입금함” 선택과 환불 계좌 입력 → maker 환불 완료 | `CHECKING_PAYMENT → REFUND_REQUESTED → BOOKING_CANCELLED`, 입금 확인 SMS 없음, 완료 시점에만 재고 복구 |
| `TL-E03` | P1, 수동/야간 | 유료 확정 booking 삭제 시도 및 다른 maker의 bulk action 시도 | 응답 `deletable=false`, 삭제/타인 action은 거부, 상태와 재고 무변경 |
| `TL-E04` | P1, 수동/야간 | 취소된 유료 booking이 일반·검색 조회에 노출됨을 먼저 확인 → 삭제 후 재조회와 `BOOKING_DELETED` 명시 필터 시도 | 삭제 후 일반·검색 목록에서 숨김, 명시 필터도 계약 오류, 재고 무변경 |

여기서 bulk는 대량 부하 시험이 아니다. 같은 공연에 2~3건만 만들고 “선택된 ID만 바뀌고 나머지는 유지된다”는 wire contract를 확인한다. 중복 ID, 역순 ID, 여러 회차 lock ordering, deadlock, cross-performance booking 혼입은 Testcontainers integration/Acceptance에 남긴다.

##### 4.4 클라이언트–서버 계약

메이커 UI가 `bookingStatus`와 `ticketPrice`로 삭제 허용 상태를 다시 계산하면 서버 정책과 쉽게 어긋난다. 서버 ticket 조회 응답의 `deletable`을 단일 계약으로 사용하고, 클라이언트는 다음 규칙을 지켜야 한다.

- 삭제 모드에서도 `deletable=false`인 항목은 checkbox를 노출하지 않거나 disabled 처리한다.
- action 전환, 검색, 필터 변경 때 현재 선택 ID를 초기화해 숨겨진 이전 선택이 payload에 섞이지 않게 한다.
- mutation 실패 시 성공 toast를 표시하거나 선택 상태를 초기화하지 않는다.
- `BOOKING_DELETED`는 화면 필터 값으로 제공하지 않는다.

Layer 2 API E2E는 `deletable`, 상태, 목록 제외, 오류 응답이라는 서버 wire contract를 검증한다. 실제 checkbox disabled와 toast는 BEAT-CLIENT component/integration test가 담당한다. 두 repository가 공유하는 OpenAPI schema 검증을 PR CI에 두고, `deletable` 누락이나 enum drift를 staging 이전에 차단한다.

현재 확인한 클라이언트 구현처럼 삭제 모드를 상태 배열로 필터링하면서 `deletable`을 선택 제어에 사용하지 않으면 유료 확정·환불 요청 건이 선택된 뒤 서버에서 거부될 수 있다. E2E 구축 전 이 부분을 cross-repository contract gap으로 등록하고, 서버 정책 복제 대신 `deletable` 소비로 수렴한다.

##### 4.5 Layer 1에 남길 전이 table test 예시

모든 거부 조합을 staging에서 호출하는 대신 domain FunSpec에서 전이표를 빠르게 고정한다.

```kotlin
class BookingMakerDeletionPolicySpec : FunSpec({
    context("메이커 예매 삭제 정책") {
        listOf(
            Triple(10_000, BookingStatus.CHECKING_PAYMENT, true),
            Triple(10_000, BookingStatus.BOOKING_CONFIRMED, false),
            Triple(10_000, BookingStatus.REFUND_REQUESTED, false),
            Triple(10_000, BookingStatus.BOOKING_CANCELLED, true),
            Triple(0, BookingStatus.BOOKING_CONFIRMED, true),
            Triple(0, BookingStatus.REFUND_REQUESTED, false),
        ).forEach { (amount, status, expected) ->
            test("결제금액 ${amount}원이고 상태가 ${status}이면 삭제 가능 여부는 ${expected}다") {
                Booking.canDeleteByMaker(status, amount) shouldBe expected
            }
        }
    }
})
```

실제 repository에는 이 정책을 더 상세히 검증하는 `BookingLifecycleInvariantSpec`과 application command/query spec이 있으므로, 구현 시 중복 spec을 새로 만들기보다 누락된 행만 기존 table에 보강한다.

## 6. Staging 격리와 테스트 데이터

### 6.1 옵션 비교

| 옵션 | 장점 | 단점 | 판단 |
|---|---|---|---|
| A. 모든 business 데이터를 고정 seed | 빠르고 구현이 단순함 | 이전 실행 상태에 의존, 병렬 충돌, drift와 수동 복구 발생 | 비추천 |
| B. 모든 identity와 business 데이터를 매번 생성·삭제 | 독립성이 가장 높음 | social login/회원 lifecycle까지 필요, cleanup 실패 시 더 복잡함 | 현재 API로는 과도함 |
| C. identity만 seed하고 business 데이터는 run별 생성 | 인증 안정성과 데이터 독립성의 균형 | 최소 seed 관리와 janitor 필요 | **BEAT 추천** |
| D. 매 실행 ephemeral environment/DB | 격리가 가장 강함 | 비용과 배포 시간이 크고 현재 CD 범위를 넘음 | 장기 목표 |

### 6.2 권장 데이터 수명주기

각 실행은 GitHub run ID와 attempt를 조합한 식별자를 가진다.

```text
runId = gh-{GITHUB_RUN_ID}-{GITHUB_RUN_ATTEMPT}
performanceTitle = [E2E:{runId}] 유료 예매 여정
bookerName = 이투이
bookerPhoneNumber = staging 전용 010-0000-0000 형식에서 runId를 hash한 값
S3 upload fileName = e2e-{runId}-poster.png
S3 object key = presigned URL 응답이 반환한 실제 key를 manifest에 기록
```

규칙:

- 날짜는 runner의 `now()`를 그대로 여러 번 섞지 않는다. suite 시작 시 한 번 캡처한 `RUN_NOW`에서 공연일과 마감일을 파생한다.
- 회차는 `RUN_NOW + 7일`처럼 충분한 여유를 둔다.
- random UUID만으로 데이터를 찾지 않는다. 계약상 허용되는 필드에는 사람이 검색 가능한 `[E2E:{runId}]` prefix를 함께 쓰고, 이름처럼 형식이 제한된 필드는 고정값과 고유 전화번호를 조합한다.
- 같은 run 재시도는 새 attempt namespace를 사용한다. create endpoint에 idempotency key 계약이 없으므로 같은 payload 재전송을 멱등하다고 가정하지 않는다.
- 테스트 간 state를 공유하지 않는다. fixture는 journey 단위로 생성한다.
- suite 병렬 실행을 막아 동일 staging 계정의 상태 충돌을 피한다.

### 6.3 Setup과 teardown

정상 cleanup은 역순 보상 작업으로 수행한다.

```text
refund/cancel booking
  -> delete/cancel ticket if contract allows
  -> delete performance
  -> S3 object key를 cleanup manifest에 기록
```

teardown 실패가 원래 assertion 실패를 덮지 않게 두 결과를 따로 기록한다. cleanup 실패는 warning으로 숨기지 않고 별도 `cleanup-leak` artifact와 metric으로 남긴다.

API만으로 제거할 수 없는 cancelled/deleted booking은 다음 janitor로 보완한다.

- staging 전용 least-privilege credential을 사용한다.
- 대상은 `[E2E:%]` prefix, staging seed user ID allowlist, 생성 후 24시간 경과 조건을 모두 만족해야 한다.
- 삭제 전 대상 count와 ID를 artifact에 기록한다.
- 매 배포마다 실행하지 않고 daily schedule로 실행한다.
- production credential과 endpoint에서는 실행 자체를 거부한다.

staging-only cleanup HTTP endpoint를 production artifact에 추가하는 방식은 초기에는 채택하지 않는다. 공격 표면과 운영 오구성 위험이 E2E 편의보다 크다.

### 6.4 Seed identity 정책

필수 seed는 두 개뿐이다.

| identity | 권한 | 용도 |
|---|---|---|
| `e2e-booker` | ROLE_MEMBER | 회원 예매와 환불 요청 |
| `e2e-maker` | ROLE_MEMBER | 공연 생성과 소유 ticket 관리 |

- 사람이 쓰는 staging 계정과 공유하지 않는다.
- 장기 JWT를 repository secret으로 저장하지 않는다.
- 가능하면 CI가 짧은 TTL token을 발급받는 staging 전용 auth bootstrap을 사용한다.
- 당장 token 발급 API가 없다면 GitHub Environment secret에 token을 두고 만료 전 rotation runbook과 소유자를 정한다.
- token, cookie, 전화번호, 계좌번호는 test output과 HTTP dump에서 masking한다.

## 7. 외부 종속성 정책

### 7.1 옵션 원칙

외부 연동은 “전부 mock” 또는 “전부 live”로 통일하지 않는다. 배포 gate에서 검증할 BEAT 소유 경계와 사업자 소유 경계를 분리한다.

| 종속성 | 배포 직후 E2E 정책 | 별도 canary | 이유 |
|---|---|---|---|
| CoolSMS | staging capture fake/sandbox | 일 1회 지정 번호 live | 매 배포 실문자·비용·rate limit 방지 |
| Slack Webhook | capture webhook 또는 비활성 | 일 1회 staging channel | 고객 journey 성공과 무관한 알림 장애 분리 |
| S3 | staging bucket live | 불필요 | presigned URL, IAM, CORS, object lifecycle 검증 가치가 큼 |
| CloudFront | staging distribution live | 지속 synthetic 가능 | origin/path/cache 설정은 실제 환경에서만 드러남 |
| Kakao OAuth | 기존 seed token 사용 | sandbox 계정으로 별도 scheduled test | consent/provider 장애를 배포 회귀와 분리 |
| MySQL/Redis | staging managed resource live | health/metric 병행 | Layer 2의 핵심 wiring 대상 |

현재 `CoolSmsAdapter`는 Nurigo `Message`를 method 안에서 직접 생성하고 endpoint를 주입받지 않으므로 URL 설정만으로 capture fake로 바꿀 수 없다. Phase 2 전에 infrastructure 내부에 최소 `SmsTransport` 경계를 두고 `BEAT_SMS_MODE=capture|live`로 구현을 선택하게 한다. capture 구현은 staging 전용 collector로 보내며, live 구현만 Nurigo SDK를 생성한다. 이 변경은 동일 module 내부 adapter seam이며 새 product module을 만들지 않는다. 공식 sandbox가 실제 계약과 비용 조건을 충족한다고 확인되면 capture transport 대신 sandbox 구현을 선택할 수 있다.

CoolSMS capture transport와 Slack capture webhook은 요청 method, path, template ID, 수신자 hash, correlation ID만 저장한다. message 전문이나 민감정보를 장기 보관하지 않는다. `BEAT_SMS_MODE=live`인 staging 배포는 E2E preflight에서 거부한다.

현재 storage adapter는 `/`가 없는 fileName만 받고 `${cloud.s3.key-prefix}/poster/{UUID}-{fileName}` 형태의 실제 key를 만든다. 따라서 runner가 임의의 `e2e/{runId}/` prefix를 가정하지 않는다. presigned URL 응답의 key를 fixture manifest에 저장하고, janitor가 그 exact key만 삭제한다. 별도 staging 환경을 만들면 현재 `dev|prod` image-key prefix allowlist에 `staging`을 추가할지, staging이 `dev` prefix를 사용할지 architecture/security review로 먼저 결정한다.

S3/CloudFront journey는 다음만 검증한다.

1. `e2e-{runId}-poster.png` fileName으로 presigned URL을 발급하고 응답의 실제 key를 기록한다.
2. 작은 고정 PNG를 업로드한다.
3. CloudFront URL을 polling해 200과 content type을 확인한다.
4. cleanup manifest를 exact-key janitor에 전달한다. bucket 전체가 E2E 전용으로 격리된 경우에만 lifecycle TTL로 누락을 보완한다.

`e2e-storage-janitor.yml`은 최근 24시간 E2E artifact의 manifest를 `actions:read`로 내려받고, GitHub OIDC로 staging 전용 AWS role을 assume한다. key가 staging bucket, 허용 category, `*-e2e-gh-{runId}-*` basename을 모두 만족할 때만 `s3:DeleteObject`를 exact key로 호출한다. 장기 AWS key와 prefix wildcard 삭제는 사용하지 않는다.

cache purge 전체 호출이나 production distribution 접근은 금지한다.

## 8. Kotest E2E 아키텍처

### 8.1 디렉터리

```text
apps/api/src/e2eTest/kotlin/com/beat/apps/api/e2e
├── E2eEnvironment.kt
├── client/E2eClient.kt
├── fixture/E2eFixture.kt
├── support/E2eRunContext.kt
└── journey
    ├── StagingSmokeE2eSpec.kt
    ├── GuestBookingE2eSpec.kt
    ├── MemberBookingE2eSpec.kt
    └── MakerTicketLifecycleE2eSpec.kt
```

E2E 코드는 production DTO를 import하지 않는다. JSON wire contract를 독립 data class 또는 `JsonNode`로 표현한다. production DTO를 공유하면 서버와 테스트가 동시에 잘못 바뀌어도 테스트가 통과할 수 있다.

### 8.2 Tag 표기

현재 repository는 Kotest 6.2.4와 `@Tags("acceptance")`를 사용한다. 따라서 요구사항의 개념적 `@Tag("e2e")`는 실제 코드에서 Kotest annotation인 `@Tags("e2e")`로 표기한다.

```kotlin
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec

@Tags("e2e")
class StagingSmokeE2eSpec : FunSpec({
    test("staging health와 공개 API가 실제 gateway를 통과한다") {
        // ...
    }
})

@Tags("e2e", "e2e-extended")
class TicketLifecycleExtendedE2eSpec : FunSpec({
    test("입금 확인 전 환불 요청은 완료 전까지 재고를 유지한다") {
        // TL-E02
    }
})
```

모든 spec에 `e2e`를 붙이고, 매 배포 대상이 아닌 spec에만 `e2e-extended`를 추가한다. source set 자체가 일차 격리 장치이고 tag는 분류와 방어적 필터다.

### 8.3 Gradle 독립 source set과 task

`apps/api/build.gradle.kts`에 다음 형태로 추가한다. 아래는 설계 예시이며 실제 적용 시 version catalog alias 존재 여부를 먼저 확인한다.

```kotlin
import org.gradle.api.tasks.testing.Test

val e2eTest = sourceSets.create("e2eTest")

dependencies {
    add(e2eTest.implementationConfigurationName, libs.kotest.runner.junit5)
    add(e2eTest.implementationConfigurationName, libs.kotest.assertions.core)
    add(e2eTest.implementationConfigurationName, libs.jackson.module.kotlin)
}

tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "Runs black-box API journeys against an already deployed staging environment."

    testClassesDirs = e2eTest.output.classesDirs
    classpath = e2eTest.runtimeClasspath

    val suite = providers.environmentVariable("E2E_SUITE").orElse("critical")

    useJUnitPlatform {
        includeTags("e2e")
        if (suite.get() != "extended") excludeTags("e2e-extended")
    }
    systemProperty("kotest.tags.include", "e2e")
    if (suite.get() != "extended") {
        systemProperty("kotest.tags.exclude", "e2e-extended")
    }

    maxParallelForks = 1
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }

    doFirst {
        require(suite.get() in setOf("critical", "extended")) {
            "E2E_SUITE must be critical or extended"
        }
    }

    val required = listOf(
        "E2E_BASE_URL",
        "E2E_HEALTH_URL",
        "E2E_RELEASE_URL",
        "E2E_TARGET_ENV",
    )
    onlyIf("required E2E environment variables are configured") {
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        if (missing.isNotEmpty()) {
            logger.lifecycle("Skipping e2eTest: missing ${missing.joinToString()}")
        }
        missing.isEmpty()
    }
}
```

실행 명령:

```bash
E2E_BASE_URL=https://api.staging.example.com \
E2E_HEALTH_URL=https://management.staging.example.com/health \
E2E_RELEASE_URL=https://management.staging.example.com/info \
E2E_TARGET_ENV=staging \
E2E_SUITE=critical \
E2E_ORIGIN=https://web.staging.example.com \
E2E_BOOKER_AUTH_TOKEN='***' \
E2E_MAKER_AUTH_TOKEN='***' \
./gradlew e2eTest --no-build-cache
```

`./gradlew check`에서 `e2eTest`를 의존하지 않는다. 별도 source set이므로 일반 `test` task도 E2E class를 발견하지 않는다. 이 두 조건을 architecture/Gradle test로 고정하면 PR CI 격리를 회귀로부터 보호할 수 있다.

로컬에서는 필수 URL이 없으면 task를 skip한다. CD에서는 silent green을 막기 위해 workflow preflight가 URL과 token 누락을 먼저 실패시킨다.

### 8.4 환경 설정과 skip guard

```kotlin
package com.beat.apps.api.e2e

import java.net.URI

data class E2eEnvironment(
    val baseUrl: String,
    val healthUrl: String,
    val releaseUrl: String,
    val origin: String?,
    val bookerToken: String?,
    val makerToken: String?,
) {
    companion object {
        fun loadOrNull(env: Map<String, String> = System.getenv()): E2eEnvironment? {
            val baseUrl = env["E2E_BASE_URL"]?.trimEnd('/') ?: return null
            val healthUrl = env["E2E_HEALTH_URL"] ?: return null
            val releaseUrl = env["E2E_RELEASE_URL"] ?: return null
            val target = env["E2E_TARGET_ENV"] ?: return null
            val origin = env["E2E_ORIGIN"]
            E2eTargetPolicy.requireAllowed(
                target,
                *listOfNotNull(baseUrl, healthUrl, releaseUrl, origin).toTypedArray(),
            )
            return E2eEnvironment(
                baseUrl = baseUrl,
                healthUrl = healthUrl,
                releaseUrl = releaseUrl,
                origin = origin,
                bookerToken = env["E2E_BOOKER_AUTH_TOKEN"],
                makerToken = env["E2E_MAKER_AUTH_TOKEN"],
            )
        }
    }
}

private object E2eTargetPolicy {
    // 실제 구현 PR에서 팀의 exact non-production host로 교체하고 code review로만 변경한다.
    private val allowed = mapOf(
        "staging" to setOf(
            "api.staging.example.com",
            "management.staging.example.com",
            "web.staging.example.com",
        )
    )

    fun requireAllowed(target: String, vararg urls: String) {
        val allowedHosts = requireNotNull(allowed[target]) { "Unknown E2E target: $target" }
        urls.map(URI::create).forEach { uri ->
            require(uri.scheme == "https" && uri.host in allowedHosts) {
                "E2E URL is not in the checked-in staging host allowlist"
            }
        }
    }
}
```

`E2E_EXPECTED_API_HOST` 같은 environment variable은 URL과 함께 오설정될 수 있으므로 보안 경계로 쓰지 않는다. API·health·release host는 checked-in exact allowlist로 모두 검증하고 allowlist 밖 host는 production 여부와 관계없이 거부한다. journey별 secret이 없으면 해당 test만 skip한다. URL 전체가 없으면 Gradle task가 skip된다. target/host guard가 불일치하면 cleanup을 포함한 어떤 요청도 보내기 전에 suite가 실패한다.

```kotlin
@Tags("e2e")
class MemberBookingE2eSpec : FunSpec({
    val environment = E2eEnvironment.loadOrNull()
    val enabled = environment?.bookerToken != null && environment.makerToken != null

    test("회원이 예매하고 자신의 예매에서 동일 booking을 조회한다")
        .config(enabled = enabled) {
            val env = requireNotNull(environment)
            // journey
        }
})
```

skip된 test 수는 report에 남겨야 한다. 자동 staging gate에서는 P0 journey가 하나라도 skip되면 workflow를 실패시킨다.

### 8.5 `E2eClient` 최소 설계

새 HTTP library를 도입하지 않고 Java 25 `HttpClient`와 기존 Jackson을 사용한다.

```kotlin
package com.beat.apps.api.e2e.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class E2eResponse(
    val status: Int,
    private val payload: ByteArray,
    private val headers: Map<String, List<String>>,
    private val mapper: ObjectMapper,
) {
    val contentType: String? = firstHeader("Content-Type")

    private fun firstHeader(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

    fun hasHeader(name: String): Boolean =
        headers.keys.any { it.equals(name, ignoreCase = true) }

    fun json(): JsonNode {
        check(contentType?.startsWith("application/json") == true) {
            "Expected JSON response; status=$status, contentType=$contentType"
        }
        check(payload.isNotEmpty()) { "Expected non-empty JSON response; status=$status" }
        return mapper.readTree(payload)
    }

    fun safeSummary(): String =
        "E2eResponse(status=$status, contentType=$contentType, contentLength=${payload.size})"

    override fun toString(): String = safeSummary()
}

class E2eClient(
    private val baseUrl: String,
    private val defaultToken: String? = null,
    private val origin: String? = null,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
) {
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .cookieHandler(CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    fun get(path: String, token: String? = defaultToken): E2eResponse =
        exchange("GET", path, null, token)

    fun post(path: String, body: Any, token: String? = defaultToken): E2eResponse =
        exchange("POST", path, body, token)

    fun patch(path: String, body: Any, token: String? = defaultToken): E2eResponse =
        exchange("PATCH", path, body, token)

    fun put(path: String, body: Any, token: String? = defaultToken): E2eResponse =
        exchange("PUT", path, body, token)

    fun delete(path: String, token: String? = defaultToken): E2eResponse =
        exchange("DELETE", path, null, token)

    private fun exchange(method: String, path: String, body: Any?, token: String?): E2eResponse {
        val builder = HttpRequest.newBuilder(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("X-E2E-Run-Id", E2eRunContext.runId)

        token?.let { builder.header("Authorization", "Bearer $it") }
        origin?.let { builder.header("Origin", it) }

        val publisher = body?.let {
            builder.header("Content-Type", "application/json")
            HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(it))
        } ?: HttpRequest.BodyPublishers.noBody()

        val response = http.send(
            builder.method(method, publisher).build(),
            HttpResponse.BodyHandlers.ofByteArray(),
        )
        return E2eResponse(
            status = response.statusCode(),
            payload = response.body(),
            headers = response.headers().map(),
            mapper = mapper,
        )
    }
}
```

필수 client 정책:

- 기본 timeout을 모든 요청에 적용한다.
- redirect를 따라가지 않아 잘못된 gateway/login redirect를 성공으로 오판하지 않는다.
- cookie jar를 client instance별로 격리한다.
- HTML gateway 오류, 빈 body, S3 binary도 status와 content type을 먼저 분류한 뒤 JSON일 때만 `json()`으로 파싱한다.
- `E2eResponse.toString()`은 payload와 header를 출력하지 않는다. 필요한 error field만 allowlist로 추출해 masking한다.
- assertion failure에 method, path, status, correlation ID, safe summary만 남긴다.
- Authorization, cookie, 전화번호, 계좌번호는 절대 출력하지 않는다.
- `expectStatus(200)`, `data("bookingId")` 같은 작은 helper만 추가하고 범용 API testing framework로 키우지 않는다.

### 8.6 비동기 polling helper

고정 sleep은 사용하지 않는다. deadline까지 상태를 다시 읽고 마지막 실패를 보존한다.

```kotlin
fun <T> pollUntil(
    timeout: Duration = Duration.ofSeconds(20),
    interval: Duration = Duration.ofMillis(500),
    read: () -> T,
    done: (T) -> Boolean,
    safeSummary: (T) -> String,
): T {
    val deadline = System.nanoTime() + timeout.toNanos()
    var last = read()
    while (!done(last) && System.nanoTime() < deadline) {
        Thread.sleep(interval.toMillis())
        last = read()
    }
    check(done(last)) {
        "Condition was not met within $timeout; last=${safeSummary(last)}"
    }
    return last
}
```

polling은 비동기 notification, CloudFront propagation처럼 eventual consistency가 계약인 곳에만 쓴다. `safeSummary`에는 status, 상태 enum, count처럼 비민감 필드만 전달한다. booking DB transaction 직후 상태가 즉시 보여야 하는 계약을 polling으로 감추지 않는다.

### 8.7 FunSpec journey 예시

```kotlin
@Tags("e2e")
class GuestBookingE2eSpec : FunSpec({
    val env = E2eEnvironment.loadOrNull()

    test("비회원 예매가 guest session으로 조회되고 메이커 환불 후 재고가 복구된다")
        .config(enabled = env?.makerToken != null && env.origin != null) {
            val environment = requireNotNull(env)
            val maker = E2eClient(environment.baseUrl, environment.makerToken)
            val guest = E2eClient(environment.baseUrl, origin = environment.origin)
            val fixture = E2eFixture.createPaidPerformance(maker, E2eRunContext.runId)

            try {
                val before = guest.get(
                    "/api/schedules/${fixture.scheduleId}/availability?purchaseTicketCount=1"
                ).json()["data"]["availableTicketCount"].asInt()

                val identity = mapOf<String, Any>(
                    "bookerName" to "이투이",
                    "bookerPhoneNumber" to E2eRunContext.phoneNumber,
                    "birthDate" to "900101",
                    "password" to E2eRunContext.guestPassword,
                )
                val created = guest.post(
                    "/api/bookings/guest",
                    identity + mapOf(
                        "scheduleId" to fixture.scheduleId,
                        "purchaseTicketCount" to 1,
                    ),
                )
                created.status shouldBe 201
                created.hasHeader("Set-Cookie") shouldBe true
                created.json()["data"]["bookingStatus"].asText() shouldBe "CHECKING_PAYMENT"
                val bookingId = created.json()["data"]["bookingId"].asLong()

                val retrieved = guest.post("/api/bookings/guest/retrieve", identity)
                retrieved.status shouldBe 200
                retrieved.json()["data"].any {
                    it["bookingId"].asLong() == bookingId
                } shouldBe true

                val after = guest.get(
                    "/api/schedules/${fixture.scheduleId}/availability?purchaseTicketCount=1"
                ).json()["data"]["availableTicketCount"].asInt()
                after shouldBe before - 1

                val refundRequested = guest.patch(
                    "/api/bookings/refund",
                    mapOf(
                        "bookingId" to bookingId,
                        "bankName" to "KAKAOBANK",
                        "accountNumber" to E2eRunContext.accountNumber,
                        "accountHolder" to "E2E",
                    ),
                )
                refundRequested.status shouldBe 200
                refundRequested.json()["data"]["bookingStatus"].asText() shouldBe "REFUND_REQUESTED"

                maker.put(
                    "/api/tickets/refund",
                    mapOf(
                        "performanceId" to fixture.performanceId,
                        "bookingList" to listOf(mapOf("bookingId" to bookingId)),
                    ),
                ).status shouldBe 200

                val ticket = maker.get("/api/tickets/${fixture.performanceId}")
                    .json()["data"]["bookingList"]
                    .first { it["bookingId"].asLong() == bookingId }
                ticket["bookingStatus"].asText() shouldBe "BOOKING_CANCELLED"

                pollUntil(
                    read = {
                        guest.get(
                            "/api/schedules/${fixture.scheduleId}/availability?purchaseTicketCount=1"
                        ).json()["data"]["availableTicketCount"].asInt()
                    },
                    done = { it == before },
                    safeSummary = Int::toString,
                ) shouldBe before
            } finally {
                E2eFixture.cleanup(maker, fixture)
            }
        }
})
```

fixture의 enum wire value와 guest identity 형식은 기존 Acceptance/OpenAPI 계약을 정본으로 유지한다. 현재 계약은 이름에 한글/영문만, 전화번호에 `000-0000-0000`, 생년월일에 6자리 숫자, password에 4자리 숫자를 허용한다.

`E2eFixture.cleanup`은 내부적으로 활성 booking을 먼저 취소/환불한 뒤 실제 delete API를 호출하고 결과를 별도 recorder에 남긴다. `404`는 이미 정리된 멱등 성공으로 취급한다.

```kotlin
fun deletePerformance(maker: E2eClient, performanceId: Long, recorder: CleanupRecorder) {
    val response = maker.delete("/api/performances/$performanceId")
    if (response.status !in setOf(200, 404)) {
        recorder.leak("performance", performanceId, response.safeSummary())
    }
}
```

## 9. CI/CD 연동

### 9.1 PR CI 완전 격리

PR의 `.github/workflows/ci-pr.yml`은 계속 `./gradlew check`만 실행한다.

- `e2eTest`를 `check` dependency에 추가하지 않는다.
- E2E source set을 `test` source set에 포함하지 않는다.
- PR workflow에 staging secret을 노출하지 않는다.
- fork PR이나 untrusted commit이 staging token을 사용하는 경로를 만들지 않는다.
- E2E task compile 여부를 PR에서 확인하고 싶다면 secret 없이 `e2eTestClasses`만 별도 실행할 수 있지만, Phase 1 필수 범위는 아니다.

### 9.2 권장 workflow 구조

옵션 비교:

| 옵션 | 장점 | 단점 | 판단 |
|---|---|---|---|
| A. `deploy-dev.yml` 마지막 job에서 직접 실행 | 동일 run/SHA, 단순한 gate | 수동 실행 재사용이 불편 | 가능 |
| B. `workflow_call` + `workflow_dispatch` E2E workflow | 자동/수동 재사용, 동일 SHA 전달 가능 | workflow 파일 하나 추가 | **추천** |
| C. `workflow_run`으로 deploy 완료 감지 | deploy workflow 수정 최소화 | SHA/결과 연결과 권한 모델이 복잡, chain 제약 | 차선 |
| D. `repository_dispatch` | 외부 CD와 연동 가능 | payload 검증과 token 운영 필요 | 외부 orchestrator 도입 시 |

BEAT는 별도 reusable workflow를 `workflow_call`로 deploy workflow 마지막에 호출하고, 같은 workflow에 `workflow_dispatch`를 열어 수동 재실행도 지원하는 B안을 권장한다.

### 9.3 GitHub Actions 예시

`.github/workflows/e2e-staging.yml`:

```yaml
name: e2e-staging

on:
  workflow_call:
    inputs:
      deployed_sha:
        required: true
        type: string
      suite:
        required: false
        default: critical
        type: string
  workflow_dispatch:
    inputs:
      deployed_sha:
        description: Exact 40-character SHA currently deployed to staging
        required: true
      suite:
        description: critical은 배포 gate, extended는 lifecycle 확장 검증
        required: true
        default: critical
        type: choice
        options:
          - critical
          - extended

permissions:
  contents: read

jobs:
  e2e:
    runs-on: ubuntu-24.04
    timeout-minutes: 10
    environment: staging
    concurrency:
      group: staging-e2e
      cancel-in-progress: false
    env:
      E2E_BASE_URL: ${{ vars.E2E_BASE_URL }}
      E2E_HEALTH_URL: ${{ vars.E2E_HEALTH_URL }}
      E2E_RELEASE_URL: ${{ vars.E2E_RELEASE_URL }}
      E2E_TARGET_ENV: staging
      E2E_SUITE: ${{ inputs.suite }}
      E2E_ORIGIN: ${{ vars.E2E_ORIGIN }}
      E2E_BOOKER_AUTH_TOKEN: ${{ secrets.E2E_BOOKER_AUTH_TOKEN }}
      E2E_MAKER_AUTH_TOKEN: ${{ secrets.E2E_MAKER_AUTH_TOKEN }}

    steps:
      - name: Checkout trusted develop history without persisted credentials
        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1
        with:
          ref: develop
          fetch-depth: 0
          persist-credentials: false

      - name: Validate and select deployed revision
        env:
          DEPLOYED_SHA: ${{ inputs.deployed_sha }}
        shell: bash
        run: |
          set -euo pipefail
          if [[ ! "$DEPLOYED_SHA" =~ ^[0-9a-f]{40}$ ]]; then
            echo "deployed_sha must be an exact commit SHA" >&2
            exit 1
          fi
          git cat-file -e "${DEPLOYED_SHA}^{commit}"
          git merge-base --is-ancestor "$DEPLOYED_SHA" HEAD || {
            echo "deployed_sha is not an ancestor of trusted develop" >&2
            exit 1
          }
          git checkout --detach "$DEPLOYED_SHA"

      - name: Set up JDK 25
        uses: actions/setup-java@03ad4de0992f5dab5e18fcb136590ce7c4a0ac95
        with:
          java-version: "25"
          distribution: temurin

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@3f131e8634966bd73d06cc69884922b02e6faf92

      - name: Verify required configuration without printing secrets
        shell: bash
        run: |
          missing=0
          for name in E2E_BASE_URL E2E_HEALTH_URL E2E_RELEASE_URL E2E_TARGET_ENV E2E_ORIGIN E2E_BOOKER_AUTH_TOKEN E2E_MAKER_AUTH_TOKEN; do
            if [ -z "${!name:-}" ]; then
              echo "Missing required configuration: $name" >&2
              missing=1
            fi
          done
          exit "$missing"

      - name: Verify the deployed API revision before mutations
        env:
          DEPLOYED_SHA: ${{ inputs.deployed_sha }}
        shell: bash
        run: |
          runtime_sha="$(curl --fail --silent --show-error "$E2E_RELEASE_URL" | jq -r '.beat.commitSha')"
          if [ "$runtime_sha" != "$DEPLOYED_SHA" ]; then
            echo "Runtime SHA does not match deployed_sha" >&2
            exit 1
          fi

      - name: Run staging API E2E
        run: ./gradlew e2eTest --no-build-cache --stacktrace

      - name: Reject empty or skipped E2E reports
        shell: bash
        run: |
          results=apps/api/build/test-results/e2eTest
          test -d "$results"
          rg -q '<testcase[ >]' "$results" || {
            echo "No E2E test case was executed" >&2
            exit 1
          }
          if rg -q '<skipped([[:space:]]|/|>)' "$results"; then
            echo "Automated staging E2E must not contain skipped tests" >&2
            exit 1
          fi

      - name: Upload E2E reports
        if: always()
        uses: actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a
        with:
          name: e2e-staging-${{ github.run_id }}-${{ github.run_attempt }}
          path: |
            apps/api/build/reports/tests/e2eTest/
            apps/api/build/test-results/e2eTest/
            apps/api/build/e2e-artifacts/
          if-no-files-found: warn
          retention-days: 14
```

실제 SHA 연결 규칙:

- 자동 실행은 `needs.resolve-ref.outputs.commit_sha`를 `deployed_sha`로 전달한다.
- 수동 실행은 현재 staging에 배포된 exact SHA만 받으며, 해당 SHA가 trusted `develop` history의 조상인지 secret을 쓰는 코드를 실행하기 전에 검증한다.
- deploy가 resolved SHA를 `BEAT_COMMIT_SHA`로 runtime에 주입하고 private management info가 `.beat.commitSha`에 full 40-character SHA를 반환하게 한다.
- E2E report에 `testedCommitSha`, deployed image tag, GitHub deployment URL을 기록한다.
- branch HEAD를 다시 checkout해 배포된 binary와 다른 E2E 코드를 실행하지 않는다.
- GitHub `staging` Environment에는 required reviewer와 허용 branch 정책을 설정한다.

`deploy-dev.yml`의 deploy matrix가 모두 성공한 뒤 reusable workflow를 호출하는 개념 예시:

```yaml
  staging-e2e:
    needs: [detect-changes, resolve-ref, deploy]
    if: ${{ needs.deploy.result == 'success' && needs.detect-changes.outputs.has_apis == 'true' }}
    uses: ./.github/workflows/e2e-staging.yml
    with:
      deployed_sha: ${{ needs.resolve-ref.outputs.commit_sha }}
```

`detect-changes`에는 `apps:api`가 실제 matrix에 포함됐을 때만 `true`인 `has_apis` output을 추가한다. admin/batch만 배포된 run에서 repository SHA를 API runtime SHA로 오인해 E2E를 호출하지 않는다. 수동 재실행도 `.beat.commitSha`와 입력 SHA가 일치해야 mutation 단계로 넘어간다.

called workflow의 job이 `environment: staging`을 선언하므로 token은 해당 GitHub Environment secret에서 읽는다. caller의 모든 secret을 넘기는 `secrets: inherit`는 사용하지 않는다.

실제 repository가 `dev=staging`으로 결정되면 job/environment 이름만 `dev`에 맞추고 의미를 문서화한다. 이름만 바꾸고 staging과 개발자 공유 환경의 운영 정책을 혼용해서는 안 된다.

### 9.4 실패 알림과 rollback 결정

실패 분류:

| 분류 | 예 | 조치 |
|---|---|---|
| Product regression | 5xx, 상태 전이 오류, 재고 불일치 | promotion 중단, 동일 SHA 신규 runId로 1회 확인 후 rollback 후보 |
| Deployment/config | 404 route, JWT secret 불일치, DB/Redis 연결 실패 | 즉시 promotion 중단, 배포 설정 수정 또는 rollback |
| External dependency | CoolSMS sandbox, S3/CloudFront provider 장애 | 영향 journey만 차단, app rollback은 telemetry와 함께 판단 |
| Test/data defect | seed token 만료, fixture validation 오류, cleanup leak | 배포 유지 가능, E2E owner가 즉시 복구 |
| Unknown | 증거 부족 | promotion 중단, 자동 rollback 금지, on-call 판단 |

초기 rollback 절차:

1. E2E job이 실패하면 이후 promotion을 중단한다.
2. Slack에 환경, SHA, 실패 journey, HTTP status, correlation ID, report link를 보낸다. secret과 response 전문은 보내지 않는다.
3. 같은 SHA를 새 runId로 한 번만 재실행한다. stateful request를 같은 payload로 재전송하지 않는다.
4. 두 번 모두 product/deployment failure이면 기존 `rollback-dev.yml` 또는 대응 staging rollback workflow를 운영자가 실행한다.
5. provider incident 또는 fixture 문제면 application rollback 대신 incident/test repair로 분기한다.

자동 rollback은 아래 조건을 모두 충족한 뒤 별도 결정으로 도입한다.

- 최근 50회 기준 flake rate 1% 미만
- cleanup leak rate 0.5% 미만
- failure classifier가 app regression과 provider/test defect를 구분
- rollback target image가 immutable SHA로 확정됨
- rollback 후 smoke 재검증 workflow 존재

## 10. 단계별 구축 milestone

### Phase 1 — Scaffolding과 Health/Smoke

작업:

- `dev=staging` 여부와 GitHub Environment 이름을 결정한다.
- `apps/api/src/e2eTest`와 `e2eTest` task를 추가한다.
- `E2eEnvironment`, 최소 `E2eClient`, masking logger를 만든다.
- management health와 `/api/main` smoke를 구현한다.
- `workflow_dispatch` 수동 workflow를 추가한다.
- PR `check`가 E2E를 실행하지 않는 Gradle 검증을 추가한다.

완료 조건:

- 설정 없는 로컬 `./gradlew e2eTest`는 명시적으로 SKIPPED다.
- 설정된 수동 run은 2분 안에 끝난다.
- 잘못된 base URL, expired token, 5xx가 report에서 구분된다.
- secret이 console과 artifact에 노출되지 않는다.

### Phase 2 — 비회원/회원 예매

작업:

- `E2eRunContext`와 paid/free performance fixture를 만든다.
- fixture가 `/api/files/presigned-url`로 최소 poster를 staging S3에 업로드한 뒤 반환 key로 공연을 생성하게 한다.
- 비회원 create → retrieve → refund/cancel journey를 구현한다.
- 회원 create → retrieve → refund/cancel journey를 구현한다.
- availability 전후 비교를 추가한다.
- 공개 API 보상 cleanup과 cleanup leak artifact를 추가한다.
- CoolSMS capture fake 도달 검증을 추가한다.
- `CoolSmsAdapter`의 주입 가능한 `SmsTransport` seam과 capture/live configuration contract test를 먼저 추가한다.

완료 조건:

- 각 journey가 독립 run namespace를 사용한다.
- 연속 20회 실행에서 데이터 충돌이 없다.
- 실패 후에도 다음 실행이 성공한다.
- suite p95가 5분 이하다.

### Phase 3 — 메이커 승인과 상태 전이

작업:

- 메이커 공연/회차 생성 fixture를 완성한다.
- `TL-C01`의 선택적 bulk 입금 확인, SMS 단일 발송, 환불 완료 재시도를 구현한다.
- `TL-C02`의 무료 예매 삭제, `deletable`, 목록 제외, 재고 단일 복구를 구현한다.
- `TL-E01`~`TL-E04`를 `e2e-extended` tag로 구현해 수동·야간 실행에만 포함한다.
- BEAT-CLIENT가 상태 배열을 중복 정의하지 않고 server `deletable`로 checkbox를 제어하는지 component test로 고정한다.
- server OpenAPI 생성물과 client schema drift 검사를 PR CI에 연결한다.
- Phase 2의 S3 upload fixture에 CloudFront read-through assertion을 추가한다.
- daily TTL janitor와 dry-run report를 만든다.

완료 조건:

- 유료/무료 분기, 선택된 bulk ID, 소유권, 상태, availability가 모두 외부 조회 결과로 확인된다.
- 입금 확인과 환불 완료를 재시도해도 SMS와 재고 반영이 정확히 한 번이다.
- `deletable=false`인 예매를 클라이언트에서 선택할 수 없고 API 직접 호출도 거부된다.
- 삭제된 예매는 일반·검색 목록에서 보이지 않고 `BOOKING_DELETED` 필터도 거부된다.
- external capture와 application failure를 report에서 구분한다.
- janitor가 allowlist 밖 데이터를 건드리지 않는 검증이 있다.

### Phase 4 — CD 연동과 안정화

작업:

- reusable `workflow_call`을 staging deploy 성공 뒤 연결한다.
- CD는 `E2E_SUITE=critical`만 hard gate로 실행하고, `extended`는 `workflow_dispatch`와 별도 야간 schedule에서 현재 배포 SHA를 확인한 뒤 실행한다.
- Slack 실패 알림과 report artifact를 연결한다.
- promotion gate와 수동 rollback runbook을 문서화한다.
- flake, duration, cleanup leak, failure category metric을 수집한다.
- 50회 결과를 검토해 제한적 자동 rollback 채택 여부를 결정한다.

완료 조건:

- 배포 SHA와 tested SHA가 항상 같다.
- P0 journey 실패 또는 skip 시 promotion이 중단된다.
- 성공/실패/rollback drill이 모두 재현된다.
- quarantine test가 hard gate에 남아 있지 않는다.

## 11. 운영 지표와 suite 확장 규칙

초기 목표:

| 지표 | 목표 |
|---|---:|
| 전체 실행 p95 | 5분 이하 |
| P0 journey skip | 0건 |
| 최근 50회 flake rate | 1% 미만 |
| cleanup leak rate | 0.5% 미만 |
| 실패 분류 시간 | 10분 이하 |

새 E2E를 추가하려면 PR 설명에 다음을 답한다.

1. 어떤 고객/매출/재고 위험을 검증하는가?
2. 왜 Acceptance/Testcontainers로는 잡을 수 없는가?
3. 어떤 staging 경계를 새로 통과하는가?
4. 생성 데이터와 외부 side effect는 어떻게 정리하는가?
5. 예상 실행 시간과 실패 시 rollback 영향은 무엇인가?

동일 위험을 기존 journey assertion 하나로 검증할 수 있으면 새 spec을 만들지 않는다.

## 12. 구현 전 결정 목록

- [ ] `dev`가 staging 역할인지, 별도 staging environment가 필요한지 확정
- [ ] management health URL의 runner 접근 방식 확정
- [ ] staging API의 exact host와 production 오접속 차단 guard 확정
- [ ] `e2e-booker`, `e2e-maker` token 발급·rotation 소유자 확정
- [ ] staging Origin allowlist 값 확정
- [ ] CoolSMS/Slack capture fake endpoint와 조회 계약 확정
- [ ] S3/CloudFront staging bucket/distribution과 exact-key cleanup manifest 확정; bucket이 E2E 전용일 때만 lifecycle TTL 채택
- [ ] 별도 staging 도입 시 image key의 `dev|prod` prefix 계약 처리 방식 확정
- [ ] janitor의 allowlist, TTL, dry-run, 승인자 확정
- [ ] 실패 알림 channel과 rollback 의사결정자 확정

## 13. 참고 문서

- [BEAT Architecture SSOT](../architecture/architecture.md)
- [Kotest 6.2 — Grouping Tests with Tags](https://kotest.io/docs/framework/tags.html)
- [Gradle — Testing in Java & JVM Projects](https://docs.gradle.org/current/userguide/java_testing.html)
- [Gradle — JVM Test Suite Plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html)
- [GitHub Actions — Events that trigger workflows](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows)
- [GitHub Actions — Reuse workflows](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows)
- [GitHub Actions — Deployment environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
