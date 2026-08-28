# support:security module

`support:security`는 BEAT의 **웹 독립적인 보안 핵심 프리미티브 및 프론트오피스 인증 포트 어댑터 모듈**입니다.
JWT 토큰 생성/검증, 비밀번호 해싱, 리프레시 토큰 인증 메커니즘을 제공하며, 서블릿/웹 프레임워크에 종속되지 않습니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `jwt` | JJWT 기반 JWT 발급 및 서명 검증 (`JwtTokenProvider`, `JwtValidator`) |
| `password` | BCrypt 기반 비밀번호 해싱 구현체 (`BcryptPasswordHasher`) |
| `token` | `:application:frontoffice`의 `TokenIssuer`, `RefreshTokenAuthenticator` 포트 구현체 |
| `exception` | 토큰 만료, 서명 불일치 등 보안 전용 기술 예외 |

## 주요 책임

- **보안 프리미티브 제공**: JWT 인코딩/디코딩, Secret Key 기반 서명 검증, BCrypt 비밀번호 암호화
- **Frontoffice Security Port 구현**: `:application:frontoffice.security`에 선언된 SPI 인터페이스 구현
- **웹 독립적 설계**: Spring MVC / Servlet API 없이 순수 토큰 및 암호화 연산 수행

## 의존성

- `:application:frontoffice` — 프론트오피스 보안 SPI 포트 인터페이스 구현
- `:support:observability` — 보안 처리 로깅 및 메트릭 연동
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` — JWT 처리 라이브러리
- `spring-security-crypto` — BCrypt 암호화 엔진

## 이 모듈이 하지 않는 것

- **Spring Security 필터 체인 연동**: 서블릿 필터(`JwtAuthenticationFilter`) 및 `@CurrentMember`는 `:support:security-web`이 소유
- **비즈니스 로그인/회원가입 워크플로우**: 인증 비즈니스 흐름은 `:application:frontoffice`가 소유
- **권한 인가(Authorization) 정책**: 엔드포인트별 Role 인가는 `:apps:*`가 소유
