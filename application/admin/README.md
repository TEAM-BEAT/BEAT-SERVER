# application:admin module

`application:admin`은 BEAT 백오피스의 **관리자 Application Service 및 유스케이스 오케스트레이션 레이어**입니다.
프로모션/배너 관리 및 관리자 사용자 조회 유스케이스와 트랜잭션 경계를 담당합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `promotion.command` | 프로모션/캐러셀 생성·수정·삭제, 이미지 Presigned URL 발급 및 캐시 관리 |
| `promotion.query` | 백오피스 프로모션/캐러셀 목록 및 상세 조회 |
| `user.query` | 관리자 사용자 목록 및 상세 조회 |
| `exception` | 도메인 예외 번역(`DomainFailureTranslator`) 및 어드민 실패 코드 |

## 주요 책임

- **관리자 유스케이스 오케스트레이션**: 백오피스 변경 및 조회 흐름 제어와 트랜잭션 관리
- **프로모션 및 캐러셀 관리**: 캐러셀 순서 조정, 유효성 검증, S3 이미지 업로드용 Presigned URL 발급
- **관리자 유저 조회**: 관리자 권한을 가진 사용자 목록 및 정보 조회
- **도메인 실패 번역**: `DomainException`을 `AdminApplicationException`으로 번역하여 안전한 에러 언어 제공
- **결과 모델 조립**: HTTP 계층에 독립적인 `AdminPromotionResults`, `AdminUserResults` 제공

## 의존성

- `:domain` — Promotion, Users Aggregate Root, RepositoryPort
- `spring-context`, `spring-tx` — 스프링 빈 관리 및 트랜잭션 처리

## 이 모듈이 하지 않는 것

- **HTTP / Web 엔드포인트 소유**: Controller, Facade, 관리자 Request/Response DTO, Swagger는 `:apps:admin`이 소유
- **프론트오피스 유스케이스 처리**: 일반 사용자 및 주최자 기능은 `:application:frontoffice`가 소유
- **영속성 및 외부 인프라 구현**: JPA 매핑, Spring Data, S3 SDK 호출 구현은 `:infrastructure`가 소유
- **Raw Domain Model 노출**: Facade/Controller로 도메인 엔티티를 직접 노출하지 않고 Result 객체로 변환
