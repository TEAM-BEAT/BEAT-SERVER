# support:security-web

`support:security-web`은 **서블릿 웹 계층 보안 통합** 모듈입니다.
`:support:security`의 토큰 검증 API와 `:support:observability`의 MDC 로깅을 결합하여
Spring Security 필터 체인, 인증 진입점, 접근 거부 핸들러, `@CurrentMember` argument resolver를 제공합니다.

## 패키지 구조

| 패키지 | 책임 |
|--------|------|
| `authentication.internal` | `JwtAuthenticationFilter`, `SecurityMdcLoggingFilter`, `CustomAccessDeniedHandler`, `CustomJwtAuthenticationEntryPoint`, `MemberAuthentication`, `AdminAuthentication`, `CurrentMemberArgumentResolver` |
| `authentication.internal.config` | `SecurityFilterConfig`, `ServletSecurityConfig`, `WebMvcConfig` |
| `guest.internal.config` | `GuestAccessConfig` — 비회원 접근 정책 설정 |
| `shared.internal` | `GatewayConfigImportSelector` — 조건부 설정 임포트 |
| _(root)_ | `@EnableGatewayServletSecurity`, `@EnableGatewayConfig`, `GatewayConfigGroup`, `@CurrentMember` |

## 주요 책임

- **JWT 인증 필터**: `JwtAuthenticationFilter`가 Authorization 헤더에서 토큰을 추출하고 `:support:security`의 `TokenValidator`로 검증
- **MDC 로깅 필터**: `SecurityMdcLoggingFilter`가 인증 결과를 기반으로 `userId`를 SLF4J MDC에 주입 (`:support:observability` 연동)
- **@CurrentMember 바인딩**: `CurrentMemberArgumentResolver`가 컨트롤러 파라미터에 현재 인증된 사용자 정보 주입
- **모듈형 활성화**: `@EnableGatewayServletSecurity`, `@EnableGatewayConfig` 어노테이션으로 실행 모듈이 필요한 보안 슬라이스만 선택적 활성화

## 의존성

```
:support:security     ← 토큰 검증·발급 API (TokenValidator, JwtProvider)
:support:observability ← MDC 로깅 유틸리티
spring-security       ← compileOnly (실행 모듈이 제공)
spring-web            ← compileOnly (실행 모듈이 제공)
springdoc-openapi     ← compileOnly (Swagger 보안 스키마)
```

## 이 모듈이 하지 않는 것

- JWT 토큰 생성/검증 로직 자체 (`→ :support:security`)
- 로그 포맷 결정, traceId 주입 (`→ :support:observability`)
- HTTP 라우팅, 컨트롤러 (`→ :apps:api`, `:apps:admin`)
- 비즈니스 인가 정책 (`→ :application:*`)
