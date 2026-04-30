# domain module

> 이 문서는 `domain` 모듈의 현재 상태, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `domain`은 도메인 모델과 repository interface의 중심이며, #419 이후 남은 persistence concern은 `BaseTimeEntity` auditing baseline 하나로 축소되어 있다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| `domain/src/main`에는 JPA entity와 Spring Data repository adapter가 남아 있지 않다. `BookingRepository`, `TicketRepository`를 포함한 저장소 계약은 `repository/` 아래 technology-neutral interface로 정리되었다. 단, `BaseTimeEntity` auditing mapped superclass는 아직 #380 transitional baseline으로 남아 있다. `Role.java`는 `ROLE_*` 문자열만 소유하고 `GrantedAuthority` / `SimpleGrantedAuthority` bridge는 없다. | `domain`에는 도메인 모델, 값 객체, 도메인 서비스, repository interface만 남긴다. JPA entity / persistence model, Spring Data adapter, QueryDSL/Kotlin JDSL 구현체, repository 구현체는 `infra`가 소유한다. | `BaseTimeEntity` infra 이동 -> #380 follow-up. infra query/read-model 경계 -> #381. Security authority conversion은 domain 밖에 둔다. |

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
    domain/       # Java enum / value type / legacy non-Kotlin domain surface
    repository/   # technology-neutral repository interface
    exception/
    port/
  src/main/kotlin/com/beat/domain/<context>/
    domain/       # Kotlin pure domain model
```

설명:
- 현재 도메인 모듈은 최종 순수 도메인 형태가 아니라 migration landing zone이다.
- `dao/` package는 더 이상 존재하지 않아야 하며, 새 repository port는 `repository/` 아래에 둔다.
- `Promotion`, `Cast`/`Staff`, `Users`, `Member`, `Performance`/`PerformanceImage`, `Schedule`, `Booking`은 pure domain model과 infra persistence model로 분리되었다.
- JPA entity / persistence model / Spring Data adapter / query 구현체는 `infra`로 이동했고, `domain`에는 repository interface만 남는다. 다만 `BaseTimeEntity` auditing baseline은 아직 #380 follow-up으로 infra 이동 대상이다.
- `Role`은 현재 `ROLE_USER`, `ROLE_MEMBER`, `ROLE_ADMIN` 문자열만 소유하며, Spring Security authority adapter 책임은 domain 밖에 둔다.

## #380 domain / persistence transition note

`domain`의 transitional persistence concern은 #419 이후 `BaseTimeEntity` auditing baseline만 남아 있다. 상세 inventory와 단계별 분리 전략은 root [`MIGRATION.md`](../MIGRATION.md)의 `#380 domain / persistence separation baseline` 섹션을 기준으로 한다. Booking slice까지 immutable Kotlin domain model, `domain.<context>.repository` contract, infra JPA entity, Spring Data adapter, mapper, implementation 패턴으로 분리되어 있다.

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
- `service/`에는 Entity/VO 하나에 자연스럽게 넣기 어려운 순수 domain service를 둔다.
- `repository/`에는 **interface only** 저장소 계약만 둔다.
- `vo/`에는 진짜 값 객체만 둔다.
- 애플리케이션 문맥의 전달 모델이나 응답 모델은 각 실행 모듈에서 소유한다.
- persistence mapper, JPA projection, read-model mapping은 domain 책임이 아니다. domain repository는 도메인 객체가 필요한 저장/수정/단순 조회 언어만 유지한다. 화면/목록/검색/정렬/통계용 조회 최적화 projection은 실행 모듈 query service, `module-contracts` read port, infra query/read-model 경계에서 다룬다.


### Domain decision/service standard

실행 모듈의 ApplicationService는 도메인 판단을 직접 구현하지 않고 domain decision layer에 위임한다. domain decision layer는 Entity/VO와 DomainService를 함께 의미한다.

- Entity/VO는 자기 자신의 invariant와 상태 변경 primitive를 소유한다.
    - 예: 상태 전이, 수량 증감, 값 검증, 소유권 검증처럼 한 aggregate 안에서 끝나는 규칙
- `service/`는 Entity/VO 하나에 자연스럽게 넣기 어려운 순수 도메인 정책/전략을 소유한다.
- `Policy` suffix는 기본 규칙으로 쓰지 않는다. 정책 의미는 `*DomainService` 이름과 메서드명으로 표현한다.
- 첫 도입은 context 단위의 cohesive service 하나로 시작한다. 예: `ScheduleDomainService`, `BookingDomainService`, `PerformanceDomainService`.
- 정책이 커지고 변경 이유가 갈라질 때만 역할별 DomainService로 분리한다. 예: `BookingRefundDomainService`, `BookingCancellationDomainService`, `PerformanceModificationDomainService`.
- `*DomainService`는 주요 도메인 정책의 표준 이름이지만, 단순 CRUD wrapper나 repository delegation이 아니다.
- DomainService는 repository, transaction, Spring annotation, DTO, external/module-contract port, JPA/QueryDSL type을 알지 않는다.
- DomainService는 이미 조회된 Entity/VO/primitive를 받아 판단하거나 domain primitive/value/result를 반환한다. 조회/저장 순서와 transaction은 실행 모듈 command/query service 책임이다.
- 구현체 분리는 실제 정책 variation이 있을 때만 둔다. variation이 없으면 concrete `*DomainService` class 하나로 시작하고, 역할이 갈라질 때 interface/strategy로 확장한다.


### Domain model exposure rule

- RepositoryPort는 pure Domain model을 반환하고 저장한다.
- command/query service는 유스케이스 내부에서 Domain model을 조회, 변경, 정책 판단에 사용할 수 있다.
- Domain model은 command/query service 밖으로 반환하지 않는다.
- Facade, Controller, Job/Runner, RequestDTO, ResponseDTO, CommandResult, QueryResult는 Domain model을 필드나 반환 타입으로 담지 않는다.
- 신규 `module-contracts` 타입도 Domain model을 필드나 반환 타입으로 담지 않는다. 현재 `ScheduleJobPort`와 social auth contract의 domain-coupled 타입은 transitional exception이며 새 계약의 선례로 쓰지 않는다.
- 실행 모듈 간 Domain model을 직접 전달하는 방식으로 application service를 공유하지 않는다. 공유가 필요하면 `module-contracts`의 최소 read/command contract를 정의한다.

### Domain repository vs read-model rule

- domain repository는 aggregate lifecycle과 command에 필요한 저장/수정/단순 조회 언어만 소유한다.
- `Page`, `Pageable`, `Sort`, QueryDSL/JDSL projection, API ResponseDTO, 화면/검색/목록/정렬/통계 요구가 필요해지면 domain repository가 아니라 read-model/query adapter 후보로 본다.
- ReadModel은 Domain model이 아니며 save 대상도 아니다. 조회 결과를 빠르게 만들기 위한 query 전용 shape다.
- infra query adapter가 구현하고 실행 모듈 query service가 주입받는 read contract는 `module-contracts`가 소유한다.

## 최종 목표

- Domain 규칙은 우선 Entity/Value Object에 둔다.
- 주요 도메인 정책은 `*DomainService`로 명명해 ApplicationService의 절차 코드와 분리한다.
- `Domain Service`는 CRUD wrapper가 아니라 순수 정책 중심으로 정리하며, class 이름은 기본적으로 `*DomainService` suffix를 사용한다. 처음부터 과하게 쪼개지 않고 context 단위 cohesive service로 시작한 뒤 변경 이유가 갈라질 때 분리한다.
- Schedule due-date처럼 domain language가 들어간 계산/판단은 `ScheduleDomainService` 또는 Schedule Entity/VO method에 둔다. `global-utils`에는 business-neutral date/time helper만 허용하고, BEAT/Schedule 정책 이름을 가진 계산은 두지 않는다.
- Repository 시그니처는 도메인 중립적인 interface로 유지한다.
- Spring Web/Data/JPA/QueryDSL 같은 구현 기술 의존성이 제거된다.
- JPA entity, Spring Data repository adapter, query 구현체, repository implementation은 `infra`가 소유한다.
- `Role` 같은 도메인 enum은 권한 문자열만 소유하고, `GrantedAuthority` 변환 같은 security adapter 책임은 `gateway`나 실행 모듈 경계에 둔다.

## #378 deferred persistence/query boundary

Issue #378에서는 `domain` 안에 남아 있던 JPA entity / Spring Data repository concern을 이동하지 않았다. #419 이후 이 concern은 slice별로 infra로 이동되었고, 남은 follow-up은 `BaseTimeEntity` infra 이동과 infra query implementation boundary / QueryDSL-Kotlin JDSL scan 결정이다.

현재 `domain`은 최종 순수 domain 모듈이 아니라 migration landing zone이며, 이 문서는 To-Be를 현재 완료 상태처럼 표현하지 않기 위해 current/deferred 상태를 명시한다.
