# global-utils module

> 이 문서는 `global-utils` 모듈의 최종 To-Be 계약을 정의한다. 이 모듈은 편한 공용 창고가 아니라, 가장 보수적으로 관리되는 shared-kernel 성격의 공용 모듈이어야 한다.

## 역할

- 여러 모듈에서 공유해야 하는 최소 계약과 경량 공통 타입을 제공한다.
- 공통 annotation, validation, enum, 식별자 helper, 문자열/시간 유틸리티를 제공한다.
- 비즈니스 규칙이나 웹 어댑터를 담지 않는다.

## 허용 의존성

- 저수준 유틸리티 라이브러리
- 검증/로깅 보조 라이브러리

## 금지 규칙

- `apis`, `admin`, `batch`, `gateway`, `domain`, `infra`, `observability` 의존 금지
- `HttpStatus`, `ResponseEntity`, `@RestControllerAdvice` 같은 웹 전용 추상화 보유 금지
- 특정 도메인 문맥에 종속된 정책/상수 추가 금지

## As-Is 패키지 구조

```text
global-utils module:
  (현재는 거의 placeholder)

legacy root:
  src/main/java/com/beat/global/common/**
```

설명:
- 현재 shared code 대부분은 아직 root legacy `com.beat.global.common` 아래에 남아 있다.
- `global-utils` 모듈은 최종 목적지이지만 아직 거의 비어 있는 상태다.

## To-Be 패키지 구조

```text
com.beat.global.common.annotation
com.beat.global.common.util
com.beat.global.common.exception.base
```

## 최종 목표

- 저수준 공유 타입만 남고 프레젠테이션 기술 의존성이 제거된다.
- `global-utils`가 shared-kernel처럼 다뤄질 정도로 admission rule이 엄격해진다.
- `transitionBoundaryTest`와 `SharedBoundaryContractTest`의 shared guard는 `global-utils`가 Spring/runtime lane import 없이 standalone shared-kernel로 남는지 계속 확인한다.
