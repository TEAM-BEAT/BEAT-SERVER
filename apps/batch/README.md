# apps:batch module

`apps:batch`는 BEAT의 **주기적 배경 작업 실행 진입점 및 스케줄러 구성 루트(Composition Root)**입니다.
Spring `@Scheduled`를 기반으로 `:application:system`의 데이터 정리 및 유지보수 작업을 주기적으로 트리거합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `booking.job` | 1년 경과 취소 예매 데이터 정리 스케줄러 (`TicketCleanupJobRunner`) |
| `promotion.job` | 만료 프로모션 자동 정리 및 순서 재배치 스케줄러 (`PromotionMaintenanceJobRunner`) |
| `config` / `scheduling` | 스케줄러 쓰레드 풀 설정 및 예외 핸들러 (`ScheduledTaskErrorHandler`) |

## 주요 책임

- **스케줄링 트리거**: 정기 배치 작업의 Cron 주기 정의 및 실행 트리거
- **Composition Root**: `@SpringBootApplication` 배치 전용 경량 부트스트랩
- **에러 핸들링 및 관측**: 배치 작업 실패 시 로깅, Sentry 알림 연동 및 스레드 격리

## 의존성

- `:application:system` — 배경 유지보수 트랜잭션 및 유스케이스
- `:infrastructure` — 배치 전용 DB 접근 어댑터
- `:support:observability` — 작업 추적, MDC 로깅, 헬스체크 Actuator

## 이 모듈이 하지 않는 것

- **HTTP / Web 계층 서빙**: 웹 엔드포인트 및 컨트롤러를 포함하지 않음
- **비즈니스 유지보수 로직**: 실제 청크 삭제 및 도메인 정책은 `:application:system`이 소유
- **Redis 세션/인증 연동**: 배치는 Redis 의존성을 갖지 않음
