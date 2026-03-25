# admin module

> 이 문서는 admin root dependency removal 이후 `admin` 모듈의 현재 bootstrap 계약과 남아 있는 transitional debt를 설명한다. `admin`은 이제 root project 의존 없이 자체 classpath로 build/boot/test 되어야 한다.

## 역할

- 관리자/백오피스 HTTP API의 유일한 실행 진입점이다.
- 사용자 API와 다른 승인 정책, 응답 스펙, 운영 워크플로를 소유한다.
- 팀 컨벤션상 `Controller -> Facade -> Application Service -> Domain` 흐름을 따른다.
- `apis` 서비스를 재사용하지 않고, `domain`과 공유 모듈 계약만 사용한다.

## 허용 의존성

- `module-contracts`
- `domain`
- `infra`
- `global-utils`
- `observability`
- `gateway`의 공개 계약/enable 경계만

## 금지 규칙

- `project(":")` 직접 의존 금지
- `apis`, `batch` 직접 의존 금지
- `infra.external.*`, `infra.*.entity`, `infra.*.repository.impl` 직접 import 금지
- `gateway.security.*`, `gateway.filter.*`, `gateway.config.*` 직접 import 금지
- root legacy bootstrap lane import 금지
    - `com.beat.BeatApplication`
    - `com.beat.legacyroot.*`
    - `com.beat.global.common.scheduler.application.*`
    - root `SecurityConfig` / `WebConfig`
- 전역 스캔에 기대는 구조 금지
- JPA Entity, QueryDSL Q type, Redis document를 admin API DTO로 직접 노출 금지

## Current bootstrap shape

```text
admin/
  src/main/kotlin/com/beat/admin/
    AdminApplication.kt
    config/
      InfraConfig.kt                 # @EnableInfraBaseConfig(JPA, QUERY_DSL, REDIS, ASYNC, EXTERNAL_CLIENTS)

  src/main/java/com/beat/admin/
    config/
      AdminSecurityConfig.java       # admin-owned HTTP security policy
      AdminCorsConfig.java
      AdminWebConverterConfig.java
    adapter/in/api/
    facade/
    application/
    handler/
    port/in/
```

### Runtime contract

- `AdminApplication`은 정확히 아래 bootstrap surface만 import한다.
    - `GatewayModuleConfig`
    - `InfraConfig`
    - `ObservabilityModuleConfig`
- app-level broad `@ComponentScan`은 없다.
- `AdminSecurityConfig`가 관리자 route whitelist와 인증 정책을 소유한다.
- `GatewayModuleConfig`가 gateway 내부 구현 빈을 제공하지만, `admin`은 공개 진입점인 `GatewayModuleConfig`와 `gateway.annotation.CurrentMember`만 직접 참조한다.
- `InfraConfig.kt`가 필요한 infra base config group만 명시적으로 import한다.
- `admin` resources는 `beat.scheduler.owner=false`를 유지하고, scheduler owner bean이나 non-owner bridge를 따로 소유하지 않는다.

## What changed in this issue

- `admin/build.gradle.kts`에서 `implementation(project(":"))`를 제거했다.
- `admin`은 root project classpath 없이 build/boot/test 되는 방향으로 고정됐다.
- 테스트 계약과 architecture guard를 추가해 root dependency 재도입, root bootstrap import, gateway 내부 패키지 직접 참조를 막는다.
- `admin/README.md`를 detached bootstrap 상태 기준으로 갱신했다.

## Current ownership notes

### In `admin`

- admin-facing controller / facade / application service / DTO
- module-owned security route policy
- admin exception handling
- admin CORS / converter configuration

### Outside `admin`

- `gateway`: JWT, auth filter, current-member resolver, 인증/인가 shared primitives
- `infra`: JPA, QueryDSL, Redis, async, external-client bootstrap
- `domain`: admin이 사용하는 domain model / repository / port contracts
- `module-contracts`: storage, auth, schedule 같은 cross-module port contracts
- `global-utils`: shared response DTO와 공통 예외 계층
- `observability`: logging / metrics / tracing boundary

## Remaining transitional debt

- 소스 파일 상당수가 아직 Java 기반 legacy package style을 유지한다.
- `adapter`, `port` 패키지명은 그대로 남아 있고 최종 package normalization은 끝나지 않았다.
- `gateway`와 `observability`는 아직 marker config + legacy implementation 혼합 상태라 공개 표면을 더 명확히 줄일 여지가 있다.
- root executable lane은 retire되었고, `admin`은 detached module bootstrap만 유지한다.

## Guard rails

- `AdminApplicationTest`
    - `AdminApplication` import 집합 고정
    - broad app scan 금지
    - scheduler owner disabled 계약 고정
    - admin-owned security policy 존재 확인
- `AdminArchitectureGuardTest`
    - `admin/build.gradle.kts`의 root dependency 재추가 금지
    - root bootstrap lane import 금지
    - gateway 내부 패키지 직접 import 금지
- `AdminModuleContextBootTest`
    - module context boot smoke test
    - scheduler owner / schedule bridge가 admin context에 섞이지 않는지 확인

## To-Be 패키지 구조

```text
com.beat.admin.<context>/
  controller/
  facade/
  application/
    service/
      command/
      query/
    dto/
      request/
      response/
    exception/
  config/
```

## 서비스 / CQRS 규칙

- `Facade`는 하나로 유지하고 관리자 시나리오 조합을 맡는다.
- CQRS는 `application/service`에서 먼저 적용하고 `service/command`, `service/query`로 나눈다.
- DTO는 command/query로 나누지 않고 `dto/request`, `dto/response`만 유지한다.
- 관리자 애플리케이션 문맥의 에러 코드와 예외는 `application/exception`에 둔다.
- 관리자 전용 정책이라도 순수 비즈니스 규칙이면 `domain`으로 올린다.
- `admin -> gateway`는 최종적으로 허용하되, **공통 인증/보안 경계 진입점** 으로만 제한한다.
- 즉 `GatewayModuleConfig` 또는 `jwt.contract` 같은 공개 표면만 사용하고, `gateway.security.*`, `gateway.filter.*`, `gateway.config.*`를 직접 참조하지 않는다.
- `adapter`, `port` 패키지는 BEAT 기본 가이드로 강제하지 않는다.
- Repository는 지금 즉시 command/query로 나누지 않는다. 조회 복잡도 증가나 jOOQ 도입 필요가 생기면 그때 query 전용 구현을 분리한다.

## Follow-up after this issue

1. `admin` package normalization이 충분히 진행되면 `com.beat.admin.<context>` 구조로 점진 정리
2. `gateway`/`observability` 공개 표면을 더 좁힐 수 있는지 검토
3. shared documentation을 현재 detached bootstrap 기준으로 지속 정리
