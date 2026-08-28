# infrastructure module

`infrastructure`는 BEAT의 **기술 구현 및 영속성/외부 어댑터 모듈**입니다.
JPA 엔티티, Spring Data JPA 리포지토리, jOOQ CQRS 쿼리 어댑터, Redis 세션 저장소, AWS S3, 외부 API 클라이언트의 구체적인 기술 구현을 소유합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `persistence.<domain>.entity` | JPA 엔티티, `@Embeddable` 값 객체, BaseTimeEntity |
| `persistence.<domain>.repository` | 도메인 `RepositoryPort` 구현체 및 Spring Data JPA 리포지토리 |
| `persistence.<domain>.mapper` | 도메인 모델 ↔ JPA 엔티티 상호 변환 매퍼 |
| `persistence.query` | jOOQ 기반 CQRS 뷰 조회 어댑터 (Frontoffice / Admin ReadModel 구현체) |
| `redis` | Redis 템플릿, 리프레시 토큰/비회원 세션 저장소, 스로틀링 Lua 스크립트 |
| `external` | AWS S3 파일 업로더, CoolSMS 문자 발송, Slack 웹훅, Kakao OAuth 클라이언트 |
| `config` | JPA/HikariCP 설정, jOOQ 설정, Redis 설정, RestClient 설정 |

## 주요 책임

- **도메인 저장소 포트 구현**: `:domain`의 RepositoryPort를 Spring Data JPA/JDSL로 구현
- **CQRS 뷰 쿼리 어댑터 구현**: `:application:*`이 정의한 ReadModel/QueryPort를 jOOQ로 고성능 구현
- **외부 인프라 연동**: S3 Presigned URL 생성, SMS 발송, 알림 전송 어댑터 제공
- **기술 설정 캡슐화**: `@EnableInfraBaseConfig` 등 상위 실행 모듈에 필요한 빈 선택적 노출

## 의존성

- `:domain` — 도메인 엔티티 및 저장소 포트 구현
- `:application:frontoffice` — 프론트오피스 Query Reader / Output Port 구현
- `:application:admin` — 백오피스 Output Port 구현
- `:application:system` — (필요 시) 배치 전용 영속성 포트 구현

## 이 모듈이 하지 않는 것

- **HTTP Controller / Web DTO 소유**: 요청/응답 처리는 `:apps:*`가 소유
- **비즈니스 유스케이스 조율**: 트랜잭션 오케스트레이션은 `:application:*`이 소유
- **배포 인프라 및 IaC 관리**: Ansible, CloudFormation, Nginx 등의 운영 인프라는 `ops/` 디렉토리가 소유
