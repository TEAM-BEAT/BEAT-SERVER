# application:frontoffice module

`application:frontoffice`는 BEAT 프론트오피스(사용자/주최자)의 **Application Service 및 유스케이스 오케스트레이션 레이어**입니다.
CQRS 패턴을 기반으로 관람자(Booker)와 공연 주최자(Maker)의 요구사항을 명확히 분리하여 처리합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `auth.command` | 로그인 세션 발급, 토큰 갱신, 세션 저장소 계약 |
| `booking.booker.command` | 회원/비회원 예매, 취소 오케스트레이션, 비회원 스로틀링 및 이벤트 발행 |
| `booking.booker.query` | 회원/비회원 예매 내역 조회 및 ReadModel 조립 |
| `home.booker.query` | 홈 화면 전용 프로젝션 조회 |
| `member.command` | 소셜 로그인 회원 등록 및 가입 이벤트 발행 |
| `performance.booker.query` | 관람자용 공연 상세 정보 및 예매 가능 회차 조회 |
| `performance.maker.command` | 공연 생성/수정/삭제, 이미지 업로드용 Presigned URL 발급 |
| `performance.maker.query` | 주최자용 공연 목록 및 수정 폼 조회 |
| `schedule.booker.query` | 공연 회차 및 마감일(D-day) 계산 조회 |
| `security` | 인증/보안 관련 SPI 인터페이스 (`PasswordHasher`, `TokenIssuer` 등) |
| `ticket.maker.command` | 주최자의 입금 확인 및 티켓 확정 처리 |
| `ticket.maker.query` | 주최자용 티켓 목록/상태 조회 |
| `exception` | 도메인 예외 번역(`DomainFailureTranslator`) 및 프론트오피스 실패 코드 |

## 주요 책임

- **유스케이스 및 트랜잭션 경계 관리**: 비즈니스 흐름을 제어하고 `@Transactional` 경계를 정의
- **CQRS 기반 Command / Query 분리**: 상태 변경 로직과 조회 전용 ReadModel/QueryService를 분리
- **액터별 유스케이스 격리**: 관람자(Booker)와 주최자(Maker)의 요구사항을 독립적인 패키지로 격리
- **이벤트 발행**: `BookingCreatedEvent`, `MemberRegisteredEvent` 등 트랜잭션 후속 처리용 애플리케이션 이벤트 발행
- **도메인 실패 번역**: 도메인 불변식 위반(`DomainException`)을 프론트오피스 애플리케이션 예외로 매핑

## 의존성

- `:domain` — 도메인 모델(Entity, VO), Aggregate Root, RepositoryPort, DomainService
- `spring-context`, `spring-tx` — 스프링 DI 및 선언적 트랜잭션 관리
- `kotlin-logging` — 유스케이스 로깅

## 이 모듈이 하지 않는 것

- **HTTP / Web 계층 처리**: Controller, Request/Response DTO, Swagger는 `:apps:api`가 소유
- **영속성 구현 세부사항**: JPA Entity, Spring Data Repository, QueryDSL 구현은 `:infrastructure`가 소유
- **외부 연동 구현**: OAuth 클라이언트, AWS S3, Redis 어댑터의 실제 구현체를 소유하지 않음
- **도메인 모델 외부 노출**: Raw Domain Model을 반환하지 않고 Application Result나 ReadModel로 변환하여 전달
