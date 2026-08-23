# Spring Boot 4.1 업그레이드 노트 (F8)

_2026-08-23 조사. 현재 런타임은 Boot 4.0.8 / Cloud 2025.1.3 (ADR-MIG-008)._

## 이미 적용됨

- `management.tracing.enabled` → **`management.tracing.export.enabled`** (Boot 4.0 Release Notes 명시 개명)
  - `application-observability.yml` 4개 블록(base/dev/prod/test) 전부 전환 완료.

## IDE "구성 프로퍼티 해결 불가" 경고 = 오탐

프로젝트가 4.0.8인데 IDE(IntelliJ)가 4.1.0 메타데이터로 검사해 발생. 아래 키들은
Boot 공식 Common Application Properties에 모두 존재하는 유효 키다:

- `management.server.port`
- `management.endpoints.web.base-path` / `.exposure.include` / `.exposure.exclude`
- `management.endpoint.health.show-details` / `management.endpoint.*.access`
- `management.endpoints.access.default`
- `management.metrics.distribution.slo.http.server.requests`
  (ServiceLevelObjectiveBoundary 바인딩 경고도 동일 사례)

확인 방법: `spring-boot-properties-migrator`를 test 의존으로 잠깐 붙여 부팅 진단을 출력하면
런타임 관점의 실제 미그레이션 항목만 나온다.

## 4.1 점프 시 추가 확인 목록

1. Spring Cloud 호환 train 존재 여부(없으면 OpenFeign retirement 선행 — ADR-MIG-008).
2. Actuator 스타터 모듈 분할: `spring-boot-starter-actuator` 관련 Production-Ready Starters
   매핑을 Migration Guide 표에서 재확인.
3. Endpoint 파라미터의 `org.springframework.lang.Nullable` 금지 → `org.jspecify.annotations.Nullable`.
4. Health probe 비활성이 필요하면 `management.endpoint.health.probes.enabled`.
5. `spring-boot-properties-migrator` 실행 진단 후 제거.

출처: Spring Boot 4.0 Migration Guide / Release Notes / Configuration Changelog /
docs.spring.io Common Application Properties (2026-08-23 기준).
