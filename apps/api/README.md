# apps:api module

`apps:api`는 BEAT 사용자 프론트오피스의 **REST API 진입점 및 실행 구성 루트(Composition Root)**입니다.
예매자 및 공연 메이커의 HTTP 요청을 받아 `:application:frontoffice` 유스케이스로 라우팅하고 응답 DTO를 반환합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `booking.web` | 예매 생성, 취소, 내역 조회 Controller & Request/Response DTO |
| `auth.web` | 소셜 로그인, 토큰 갱신, 세션 발급 Controller |
| `member.web` | 회원 정보 조회 및 수정 Controller |
| `performance.web` | 공연 목록, 상세 정보, 주최자 공연 관리 Controller |
| `schedule.web` | 공연 회차 및 마감 정보 Controller |
| `ticket.web` | 주최자 티켓 입금 확인 및 상태 관리 Controller |
| `common` / `config` | WebMvc, CORS, 전역 예외 처리(`GlobalExceptionHandler`), Swagger 설정 |

## 주요 책임

- **HTTP 어댑터 진입점**: REST 엔드포인트 노출, 요청 검증(`@Valid`), 응답 직렬화
- **Composition Root**: `@SpringBootApplication` 부트스트랩 및 모듈 빈 통합
- **클라이언트 계약 유지**: 일관된 API 응답 포맷(`ApiResponseCustom<T>`) 및 OpenAPI 3.1 Swagger 스펙 제공
- **웹 계층 보안 통합**: `@EnableGatewayServletSecurity`를 통한 서블릿 필터 체인 활성화

## 의존성

- `:application:frontoffice` — 유스케이스 및 ReadModel 조회
- `:infrastructure` — 런타임 저장소 및 기술 어댑터 빈 주입
- `:support:security-web` — 서블릿 JWT 필터 체인 및 `@CurrentMember` 바인딩
- `:support:observability` — MDC 로깅 및 Actuator 엔드포인트

## 이 모듈이 하지 않는 것

- **도메인 규칙 및 비즈니스 트랜잭션**: 유스케이스 오케스트레이션은 `:application:frontoffice`가 소유
- **영속성 직접 접근**: JPA/jOOQ 쿼리 및 DB 연동은 `:infrastructure`가 소유
- **백오피스 관리자 기능**: 어드민 전용 라우팅은 `:apps:admin`이 소유
- **주기적 배경 작업**: 스케줄링 및 배치는 `:apps:batch`가 소유
