# domain module

`domain`은 BEAT의 **순수 프레임워크 프리(Framework-free) 도메인 모델 레이어**입니다.
Spring, JPA, HTTP 등 어떤 인프라 프레임워크에도 의존하지 않으며, 순수 Kotlin으로 핵심 비즈니스 상태와 불변식을 보호합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `booking` | 예매 Aggregate Root (`Booking`), 불변식, 상태 전이, 저장소 계약 (`BookingRepositoryPort`) |
| `performance` | 공연 Aggregate Root (`Performance`), 공연 기간 VO, 결제 계좌 VO, 저장소 계약 |
| `schedule` | 공연 회차 Entity (`Schedule`), 예매 마감 판정, 좌석 관리, 저장소 계약 |
| `promotion` | 프로모션 Entity (`Promotion`), 캐러셀 순서 도메인 서비스, 저장소 계약 |
| `member` / `user` | 회원/사용자 모델, 역할(`Role`), 소셜 타입 VO, 저장소 계약 |
| `cast` / `staff` | 출연진/스태프 도메인 엔티티 및 저장소 계약 |
| `event` | 도메인 이벤트 (`BookingCreatedEvent`, `MemberRegisteredEvent` 등) |
| `common` | 공통 도메인 예외(`DomainException`), 상태 코드, 기본 인터페이스 |

## 주요 책임

- **비즈니스 상태 및 불변식 보호**: 예매 인원 초과, 결제 금액 검증, 회차 마감 상태 등 핵심 규칙 집행
- **도메인 서비스**: 단일 엔티티에 속하지 않는 순수 비즈니스 정책(프로모션 캐러셀 번호 재정렬 등) 처리
- **저장소 계약(Port) 정의**: 인프라 구현체로부터 독립된 Output Port 인터페이스 정의
- **도메인 이벤트 선언**: 상태 변경 사실을 나타내는 순수 불변 이벤트 정의

## 의존성

- **의존성 없음 (Zero Dependencies)** — 순수 Kotlin 표준 라이브러리만 사용

## 이 모듈이 하지 않는 것

- **Spring / JPA 어노테이션 사용 금지**: `@Entity`, `@Transactional`, `@Component` 등 프레임워크 의존 일체 배제
- **SQL / jOOQ 쿼리 소유 금지**: DB 접근은 `:infrastructure`가 소유
- **트랜잭션 및 유스케이스 조율**: 트랜잭션 경계는 `:application:*`이 소유
- **HTTP / 직렬화 처리**: JSON 직렬화 및 Web DTO는 `:apps:*`가 소유
