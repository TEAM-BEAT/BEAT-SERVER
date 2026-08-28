# support:observability module

`support:observability`는 BEAT의 **횡단 관심사용 관측 가능성(Observability) 기술 라이브러리**입니다.
SLF4J MDC 기반의 구조화된 로깅, OpenTelemetry 분산 트레이싱 상관관계(Correlation), Spring Boot Actuator 설정 및 Sentry 에러 전송 설정을 제공합니다.

## 패키지 구조

| 패키지 | 설명 |
| --- | --- |
| `logging` | SLF4J MDC 헬퍼(`MdcLogging`), 4-Layer 로깅 전략, JSON 로그 포맷터 |
| `tracing` | Grafana Tempo 분산 추적 연동, `traceId`/`spanId` MDC 주입 및 전파 |
| `config` | Spring Boot Actuator, Micrometer Prometheus 지표 설정, Sentry DSN 연동 |

## 주요 책임

- **구조화된 MDC 로깅**: 요청별 `traceId`, `spanId`, `userId`를 MDC 컨텍스트에 관리하여 로그-트레이스 연동 지원
- **4-Layer 로깅 표준 제공**: Security, Web, Service, External Layer별 일관된 로깅 포맷 정의
- **지표 및 헬스체크 표준화**: Actuator 엔드포인트 노출 정책 및 Prometheus 스크랩 규격 정의
- **에러 모니터링**: 예외 발생 시 Sentry 컨텍스트 태깅 및 이벤트 전송

## 의존성

- **단독 라이브러리 (No project dependencies)**: 타 비즈니스 모듈에 의존하지 않음
- `spring-boot-starter-actuator`, `micrometer-registry-prometheus` — 메트릭 수집
- `spring-boot-starter-opentelemetry` — 분산 트레이싱
- `sentry-spring-boot-4-starter` — 에러 트래킹

## 이 모듈이 하지 않는 것

- **비즈니스 에러 정의**: 업무 도메인 예외 및 HTTP 응답 코드는 `:domain`과 `:application:*`이 소유
- **HTTP 요청 가로채기/인증**: `SecurityMdcLoggingFilter` 등 실제 서블릿 필터는 `:support:security-web`이 소유
