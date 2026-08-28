# application:system module

`application:system`은 BEAT의 **배경 작업 및 배치 처리를 위한 시스템 Application Service 레이어**입니다.
데이터 정리, 보존 주기 정책 집행, 만료 데이터 유지보수 등의 주기적 자동화 유스케이스를 소유합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `booking.command` | 1년 이상 경과한 취소 예매 건 청크 단위 정리 (`TicketCleanupService`) |
| `promotion.command` | 종료된 공연 프로모션 만료 처리 및 캐러셀 순서 재정렬 (`PromotionMaintenanceService`) |

## 주요 책임

- **예매 데이터 보존 정책 집행**: 취소된 지 1년 지난 예매(`Booking`) 데이터를 500건 단위 청크로 삭제하여 DB 부하 분산
- **프로모션 데이터 정합성 유지**: 공연 종료 여부를 확인해 유효하지 않은 프로모션을 삭제하고 캐러셀 번호 연속성 재배치
- **도메인 정책 기반 오케스트레이션**: `PromotionEligibilityDomainService`, `PromotionCarouselDomainService` 등 순수 도메인 정책을 활용한 유지보수
- **시스템 트랜잭션 관리**: 배치 작업 단위의 `@Transactional` 경계 설정 및 락(`lockById`, `lockAll`) 제어

## 의존성

- `:domain` — Booking, Performance, Promotion, Schedule Aggregate 및 RepositoryPort, DomainService
- `spring-context`, `spring-tx` — 스프링 컴포넌트 및 트랜잭션 관리
- `kotlin-logging` — 배치 유지보수 작업 로깅

## 이 모듈이 하지 않는 것

- **스케줄링 트리거 및 잡 러너 소유**: Spring Batch Job, `@Scheduled`, CommandLineRunner 등 실행 진입점은 `:apps:batch`가 소유
- **HTTP / 사용자 요청 처리**: Web/API 진입점을 일체 포함하지 않음
- **영속성 구현체 소유**: JPA Entity 및 데이터베이스 직접 쿼리 구현은 `:infrastructure`가 소유
