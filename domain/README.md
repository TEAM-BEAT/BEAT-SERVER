# domain module

> 이 문서는 `domain` 모듈의 최종 To-Be 계약을 정의한다. `domain`은 비즈니스 규칙의 중심이며, 실행 모듈/인프라/웹을 모르는 상태를 목표로 한다.

## 역할

- 핵심 도메인 모델, 값 객체, 도메인 서비스, Repository Interface를 소유한다.
- 유스케이스와 무관하게 재사용되는 순수 비즈니스 규칙을 제공한다.
- 영속성, 웹, 보안, 외부 API의 구현 세부사항을 모른다.

## 허용 의존성

- `global-utils`
- Kotlin/JDK 표준 라이브러리

## 금지 규칙

- `infra`, `apis`, `admin`, `batch`, `gateway` 의존 금지
- `HttpStatus`, `ResponseEntity`, `Page`, `Pageable` 같은 Spring Web/Data 타입 의존 금지
- JPA Entity, QueryDSL Q type, Redis document, 외부 API DTO 보유 금지
- Controller 요청/응답 DTO 보유 금지

## As-Is 패키지 구조

```text
domain module:
  (현재는 거의 placeholder에 가까움)

legacy root:
  src/main/java/com/beat/domain/<context>/
    api/
    application/
    dao/
    domain/
    exception/
    port/
```

설명:
- 현재 진짜 도메인 코드는 아직 root legacy `src/main/java/com/beat/domain/**` 아래에 많이 남아 있다.
- `domain` 모듈은 최종 목적지이지만, 아직 패키지 이관이 본격화되기 전 단계다.

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
- `domain/`에는 aggregate, entity, enum 같은 핵심 도메인 모델을 둔다.
- `service/`에는 cross-aggregate 규칙 중심의 domain service를 둔다.
- `repository/`에는 **interface only** 저장소 계약만 둔다.
- `vo/`에는 진짜 값 객체만 둔다.
- 애플리케이션 문맥의 전달 모델이나 응답 모델은 각 실행 모듈에서 소유한다.

## 최종 목표

- Domain 규칙은 우선 Entity/Value Object에 둔다.
- `Domain Service`는 CRUD wrapper가 아니라 cross-aggregate 규칙 중심으로 정리한다.
- Repository 시그니처는 도메인 중립적으로 유지한다.
- Spring Web/Data 의존성이 제거된다.
