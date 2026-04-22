# domain module

> 이 문서는 `domain` 모듈의 현재 상태, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `domain`은 도메인 모델과 repository interface의 중심을 목표로 하지만, 현재는 아직 JPA/persistence concern을 포함한다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| 현재 `domain/src/main/java/com/beat/domain/**` 안에 JPA entity와 Spring Data repository concern이 남아 있다. 예: `Users`는 `@Entity`이고 `UserRepository`는 `JpaRepository`를 확장한다. `Role.java`는 `ROLE_*` 문자열만 소유하고 `GrantedAuthority` / `SimpleGrantedAuthority` bridge는 없다. | `domain`에는 도메인 모델, 값 객체, 도메인 서비스, repository interface만 남긴다. JPA entity / persistence model, Spring Data adapter, QueryDSL/Kotlin JDSL 구현체, repository 구현체는 `infra`가 소유한다. | domain model / persistence model separation -> #380. infra query/implementation 경계 -> #381. Security authority conversion은 domain 밖에 둔다. |

## 역할

- 핵심 도메인 모델, 값 객체, 도메인 서비스, repository interface를 소유한다.
- 유스케이스와 무관하게 재사용되는 순수 비즈니스 규칙을 제공한다.
- JPA entity, Spring Data repository adapter, query 구현체, 웹, 보안, 외부 API의 구현 세부사항을 모른다.

## 허용 의존성

- `global-utils`
- Kotlin/JDK 표준 라이브러리

## 금지 규칙

- `infra`, `apis`, `admin`, `batch`, `gateway` 의존 금지
- `HttpStatus`, `ResponseEntity`, `Page`, `Pageable` 같은 Spring Web/Data 타입 의존 금지
- `GrantedAuthority` 같은 Spring Security adapter 타입 의존 금지
- JPA Entity, QueryDSL Q type, Redis document, 외부 API DTO 보유 금지
- Controller 요청/응답 DTO 보유 금지

## Current 패키지 구조

```text
domain/
  src/main/java/com/beat/domain/<context>/
    dao/          # 현재는 대부분 Spring Data repository concern이 남아 있음
    domain/       # 현재는 JPA entity와 domain enum이 함께 남아 있음
    repository/   # slice별로 이동 중인 technology-neutral repository interface
    exception/
    port/
```

설명:
- 현재 도메인 모듈은 최종 순수 도메인 형태가 아니라 migration landing zone이다.
- `Users` 같은 타입은 아직 `@Entity`이고, `UserRepository` 같은 repository는 `JpaRepository`를 확장한다. `Promotion`은 #380 concrete slice에서 pure domain model로 분리되었고, `PromotionRepository`는 `repository/` 아래 technology-neutral contract로 이동했다.
- 최종 목표에서는 JPA entity / persistence model / Spring Data adapter / query 구현체가 `infra`로 이동하고, `domain`에는 repository interface만 남는다.
- `Role`은 현재 `ROLE_USER`, `ROLE_MEMBER`, `ROLE_ADMIN` 문자열만 소유하며, Spring Security authority adapter 책임은 domain 밖에 둔다.

## #380 domain / persistence transition note

`domain`의 JPA entity, Spring Data repository, QueryDSL projection, `BaseTimeEntity` auditing 같은 migration 상세 inventory와 단계별 분리 전략은 root [`MIGRATION.md`](../MIGRATION.md)의 `#380 domain / persistence separation baseline` 섹션을 기준으로 한다. Promotion slice는 immutable Kotlin data class `Promotion`, `domain.promotion.repository.PromotionRepository` contract, infra `PromotionJpaEntity`, Spring Data adapter, mapper, implementation으로 분리되어 있다.

이 README는 `domain` 모듈의 역할과 목표 경계만 설명하고, migration 진행 중인 transitional allowlist/guard 상세는 `MIGRATION.md`와 `SharedBoundaryContractTest`에서 관리한다.

## To-Be 패키지 구조

```text
com.beat.domain.<context>/
  domain/
  service/
  repository/
  vo/
  exception/
```

설명:
- `domain/`에는 aggregate, domain entity, enum 같은 핵심 도메인 모델을 둔다. 여기서 entity는 JPA entity가 아니라 도메인 모델을 의미한다.
- `service/`에는 cross-aggregate 규칙 중심의 domain service를 둔다.
- `repository/`에는 **interface only** 저장소 계약만 둔다.
- `vo/`에는 진짜 값 객체만 둔다.
- 애플리케이션 문맥의 전달 모델이나 응답 모델은 각 실행 모듈에서 소유한다.
- persistence mapper, JPA projection, read-model mapping은 domain 책임이 아니다. domain repository는 도메인 객체가 필요한 저장/수정/단순 조회 언어만 유지한다. 화면/목록/통계용 조회 최적화 projection은 실행 모듈 DTO 또는 infra query/read-model 경계에서 다룬다.

## 최종 목표

- Domain 규칙은 우선 Entity/Value Object에 둔다.
- `Domain Service`는 CRUD wrapper가 아니라 cross-aggregate 규칙 중심으로 정리한다.
- Repository 시그니처는 도메인 중립적인 interface로 유지한다.
- Spring Web/Data/JPA/QueryDSL 같은 구현 기술 의존성이 제거된다.
- JPA entity, Spring Data repository adapter, query 구현체, repository implementation은 `infra`가 소유한다.
- `Role` 같은 도메인 enum은 권한 문자열만 소유하고, `GrantedAuthority` 변환 같은 security adapter 책임은 `gateway`나 실행 모듈 경계에 둔다.

## #378 deferred persistence/query boundary

Issue #378에서는 `domain` 안에 남아 있는 JPA entity / Spring Data repository concern을 이동하지 않는다. 실제 domain model / persistence model separation은 #380, infra query implementation boundary와 QueryDSL/Kotlin JDSL scan 결정은 #381에서 함께 처리한다.

현재 `domain`은 최종 순수 domain 모듈이 아니라 migration landing zone이며, 이 문서는 To-Be를 현재 완료 상태처럼 표현하지 않기 위해 current/deferred 상태를 명시한다.
