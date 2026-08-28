# apps:admin module

`apps:admin`은 BEAT 백오피스의 **관리자 REST API 진입점 및 실행 구성 루트(Composition Root)**입니다.
관리자 웹의 요청을 받아 `:application:admin` 유스케이스로 라우팅하고 어드민 전용 응답을 반환합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `promotion.web` | 프로모션/배너 등록, 수정, 삭제, 순서 변경 Controller & DTO |
| `user.web` | 관리자 계정 목록 및 회원 조회 Controller |
| `common` / `config` | 어드민 전용 보안 설정, WebMvc, Swagger 설정, 예외 처리 |

## 주요 책임

- **관리자 HTTP 진입점**: 관리자 전용 REST 엔드포인트 노출 및 파라미터 검증
- **Composition Root**: `@SpringBootApplication` 관리자 부트스트랩 및 모듈 통합
- **관리자 Swagger 스펙**: 관리자 전용 OpenAPI 3.1 문서 제공 (`/swagger-ui/index.html`)
- **관리자 인가 격리**: 관리자 권한(`ADMIN`) 검증 및 접근 제어

## 의존성

- `:application:admin` — 백오피스 유스케이스 및 Result 모델
- `:infrastructure` — 런타임 저장소 및 기술 어댑터 빈 주입
- `:support:security-web` — 서블릿 JWT 필터 체인 및 어드민 인증
- `:support:observability` — MDC 로깅 및 Actuator 엔드포인트

## 이 모듈이 하지 않는 것

- **프론트오피스 사용자 라우팅**: 일반 사용자 API는 `:apps:api`가 소유
- **도메인 규칙 및 비즈니스 트랜잭션**: 프로모션 비즈니스 로직은 `:application:admin`과 `:domain`이 소유
- **영속성 직접 구현**: DB 접근 및 S3 어댑터는 `:infrastructure`가 소유
