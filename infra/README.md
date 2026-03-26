# infra module

> 이 문서는 `infra` 모듈의 최종 To-Be 계약을 정의한다. `infra`는 구현 기술과 외부 adapter의 집합소이며, 상위 유스케이스를 알면 안 된다.

## 역할

- JPA/QueryDSL/async 등 기술 설정을 소유한다.
- `domain` Repository Interface의 구현체를 제공한다.
- 외부 API client, 파일 저장소, 메시징, 서드파티 adapter를 구현한다.
- 실행 모듈이 필요한 기술 설정만 명시적으로 import할 수 있는 부트스트랩 진입점을 제공한다.

## 허용 의존성

- `domain`
- `global-utils`

## 금지 규칙

- `apis`, `admin`, `batch`, `gateway` 의존 금지
- UseCase, Controller, 앱 전용 DTO 보유 금지
- `domain` 모델 대신 infra entity/adapter 타입을 외부에 직접 노출 금지

## As-Is 패키지 구조

```text
infra/
  src/main/java/com/beat/infra/
    EnableInfraBaseConfig.java
    InfraBaseConfig.java
    InfraBaseConfigGroup.java
    InfraBaseConfigImportSelector.java      # DeferredImportSelector — enum → class 매핑
    config/
      AsyncConfig.java                      # AsyncConfigurer, @Import(TaskExecutorConfig)
      TaskExecutorConfig.java               # beatApplicationTaskExecutor 빈 생성
      TaskSchedulerConfig.java              # taskScheduler 빈 생성
      JpaConfig.java
      MysqlCustomDialect.java
      QueryDslConfig.java
      RedisCacheConfig.java
      ThreadPoolProperties.java
  src/main/kotlin/com/beat/infra/
    InfraModuleConfig.kt

legacy root:
  src/main/java/com/beat/domain/**/dao/
  src/main/java/com/beat/global/common/config/**
```

설명:
- `InfraBaseConfigImportSelector`가 `@EnableInfraBaseConfig`의 enum 값을 읽어 해당 `@Configuration` 클래스를 선택적으로 import한다.
- `AsyncConfig`는 `@Import(TaskExecutorConfig.class)`로 executor 빈만 전이 로드하고, infra는 security-aware wrapper를 직접 소유하지 않는다.
- scheduler bean은 `TaskSchedulerConfig` + `InfraBaseConfigGroup.SCHEDULER`로 분리되어 batch에서만 명시적으로 가져간다.
- Redis runtime wiring은 Spring Boot auto-configuration과 gateway-owned config가 담당하고, infra는 더 이상 gateway-specific Redis bean을 소유하지 않는다.
- future shared caching은 dormant `RedisCacheConfig` + `InfraBaseConfigGroup.REDIS_CACHE`에서 시작하고, 현재 실행 모듈은 아직 이를 import하지 않는다.
- 일부 공통 config는 `infra`로 이동했지만, repository/JPA/query 구현 상당수는 아직 legacy root `dao` 패키지에 남아 있다.
- 즉 `infra`도 아직 최종형이 아니라 **이관 진행 중인 landing zone**이다.

## To-Be 패키지 구조

```text
com.beat.infra.config.*
com.beat.infra.external.<provider>
com.beat.infra.<context>.repository.jpa
com.beat.infra.<context>.repository.impl
com.beat.infra.<context>.repository.query   # 필요 시만
```

설명:
- `domain.<context>.repository.XxxRepository` 구현은 `infra.<context>.repository.impl`이 맡는다.
- Spring Data JPA 인터페이스는 `infra.<context>.repository.jpa`에 둔다.
- query 전용 구현은 지금 기본값이 아니고, 조회 복잡도 증가나 jOOQ 도입이 필요할 때만 `repository.query`를 추가한다.

## 최종 목표

- `infra.external.*` 타입을 상위 실행 모듈이 직접 import하지 않는다.
- `InfraModuleConfig`가 실제 기술 import를 모으는 진입점으로 성장한다.
- JPA/QueryDSL/async 부트스트랩이 명시적으로 조립되고, shared cache가 필요해질 때 `REDIS_CACHE` 그룹으로 확장한다.
