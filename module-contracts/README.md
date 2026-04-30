# module-contracts module

> 이 문서는 `module-contracts` 모듈의 현재 계약 surface, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `module-contracts`는 실행 모듈과 인프라가 공유하는 경량 계약 경계이며, 구현 대신 인터페이스와 전달 모델만 소유한다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Contract surface intentionally remains Java source and currently exposes auth, notification, schedule, SMS, and storage ports/transfer models. `domain` / `global-utils` are compileOnly references for transitional contract types. | Stable implementation-free API; language conversion only after API shape and Java/Kotlin consumer compatibility are reviewed. | Domain-coupled contract type reduction -> follow-up after #378. |

## 역할

- 실행 모듈(`apis`, `admin`, `batch`)과 `infra`가 공통으로 참조하는 계약을 소유한다.
- 포트, command, response, value object 성격의 공유 타입만 제공한다.
- 구현 세부사항이나 기술 스택 세부사항을 모르고, 모듈 간 결합을 낮추는 경계 역할을 한다.

## 허용 의존성

- `domain` — 현재 `SocialType`, `Schedule` 기반 transitional contract 때문에 compileOnly로만 허용한다. 신규 계약은 domain type을 추가로 노출하지 않는다.
- `global-utils`
- Kotlin/JDK 표준 라이브러리

## 금지 규칙

- `apis`, `admin`, `batch`, `infra`, `gateway` 직접 의존 금지
- Spring Web/Data 타입 의존 금지
- JPA Entity, QueryDSL Q type, Redis document 보유 금지
- 외부 API 구현체, Controller, Service, Repository 구현체 보유 금지
- 실행 모듈 전용 DTO나 응답 모델을 여기에 두는 것 금지

## As-Is 패키지 구조

```text
module-contracts/
  src/main/java/com/beat/contracts/
    common/
      ReadModel.java                       # read/query contract marker; Spring/JPA behavior 없음
    auth/
      JwtSubject.java
      JwtTokenPort.java
      RefreshTokenPort.java
      TokenErrorCode.java
      TokenValidationResult.java
      social/
        SocialLoginCommand.java
        SocialLoginPort.java
        SocialMemberInfo.java
    notification/
      BookingNotification.java
      BookingNotificationPort.java
      MemberNotification.java
      MemberNotificationPort.java
    schedule/
      ScheduleJobPort.java
      ScheduleReadPort.java
      readmodel/
        MinPerformanceDate.java            # @ReadModel; Schedule 최소 공연 일자 query result
    sms/
      SmsMessage.java
      SmsPort.java
    storage/
      BannerPresignedUrl.java
      CarouselPresignedUrls.java
      FileStoragePort.java
      PerformancePresignedUrls.java
```

설명:

- 현재 `module-contracts`는 `auth`, `notification`, `schedule`, `sms`, `storage` 계약을 모아두는 얇은 공유 모듈이다.
- 구현체는 다른 모듈에 있고, 이 모듈은 계약 타입만 제공한다.
- #378 결정: Java source를 유지한다. cross Java/Kotlin consumer API 안정성이 언어 전환보다 우선이므로 Kotlin 변환은 하지 않는다.
- `build.gradle.kts`에서 `domain`, `global-utils`를 `compileOnly`로만 참조한다. `SocialType`, `Schedule`, `BaseErrorCode` 같은 domain/global-utils-coupled contract type은 후속에서 contract-local value/ID로 줄일지 검토한다.
- `SharedBoundaryContractTest`는 Spring/JPA/Redis stereotype이나 infra/executable/gateway 구현 참조가 들어오지 않도록 guard한다.
- `ReadModel`은 module-contracts query result임을 드러내는 marker annotation이다. Spring/JPA가 인식하는 annotation이 아니며, save 대상/도메인 모델/API 응답 DTO가 아니라는 architectural label로만 사용한다.

## To-Be 패키지 구조

```text
com.beat.contracts/
  common/
    ReadModel
  auth/
    JwtSubject
    JwtTokenPort
    RefreshTokenPort
    TokenErrorCode
    TokenValidationResult
    social/
      SocialLoginCommand
      SocialLoginPort
      SocialMemberInfo
  notification/
    BookingNotification
    BookingNotificationPort
    MemberNotification
    MemberNotificationPort
  schedule/
    ScheduleJobPort
    ScheduleReadPort
    readmodel/
      MinPerformanceDate
  sms/
    SmsMessage
    SmsPort
  storage/
    BannerPresignedUrl
    CarouselPresignedUrls
    FileStoragePort
    PerformancePresignedUrls
```

설명:

- To-Be 구조는 현재 구조와 거의 동일하게 유지한다.
- 이 모듈은 기능별 계약만 담고, 구현 계층을 끼워 넣지 않는다.
- 계약이 늘어나더라도 역할별 패키지 분리를 기본으로 유지한다.
- read-model query result는 예외적으로 `<context>/readmodel` 하위 패키지에 모아 port/command/external contract와 구분한다.


### Application/query contract boundary

Facade/ApplicationService/DomainService 표준에서 `module-contracts`는 실행 모듈과 infra 사이의 구현 없는 계약만 맡는다.

- command/query application service 자체는 실행 모듈(`apis`, `admin`, `batch`)이 소유한다.
- DomainService는 `domain`이 소유한다. `Policy` suffix는 기본 표준으로 쓰지 않는다.
- `module-contracts`에는 실행 모듈이 필요하고 infra가 구현하는 port와 전달 모델만 둔다.
    - external port: auth, sms, storage, notification 등
    - read-model/query port: 복잡한 화면 조회, 검색, 정렬, 통계처럼 domain repository에 넣기 어려운 조회 계약
- 계약 타입은 Spring/JPA/QueryDSL/Redis/document 구현 세부사항을 포함하지 않는다.
- 신규 계약 타입은 Domain model, JPA Entity, 실행 모듈 ResponseDTO를 필드나 반환 타입으로 담지 않는다.
- 실행 모듈 전용 response DTO는 여기에 두지 않는다. read-model contract는 실행 모듈 query service와 infra query adapter 사이의 구현 없는 계약이 필요하거나 여러 실행 모듈에서 공유될 때만 둔다.
- 현재 `SocialLoginCommand` / `SocialMemberInfo`의 `SocialType`, `ScheduleJobPort`의 `Schedule` 의존은 transitional exception이다. 새 계약에서 반복하지 않고, 후속에서 contract-local enum/value 또는 id/snapshot으로 축소한다.


### Read-model contract rule

ReadModel은 Domain model을 조회 화면에 억지로 맞추지 않기 위한 조회 전용 shape다. `module-contracts`는 모든 read model의 기본 위치가 아니라, 실행 모듈과 infra 사이에 구현 없는 query 계약이 필요할 때만 read model contract를 소유한다.

- `*ReadPort`: 실행 모듈 query service가 의존하는 조회 port.
- `*ReadResult` / `*SearchResult`: query adapter가 반환하는 조회 결과. save 대상이 아니며 domain invariant를 책임지지 않는다.
- `*SearchCondition`: 검색/필터/정렬 조건. Spring Data `Pageable`, `Sort`, QueryDSL type을 노출하지 않는다.
- 신규 read contract는 primitive/JDK type 또는 contract-local enum/value만 사용한다.
- API ResponseDTO와 1:1로 보이더라도 재사용하지 않는다. ResponseDTO는 `apis`, `admin`, `batch` 각 실행 모듈이 각자 소유한다.
- 특정 실행 모듈 내부에서만 쓰고 infra 구현 계약이 필요 없는 조립 결과는 해당 실행 모듈 `application/dto/result` 또는 query service 내부 type에 둔다.
- infra query implementation 내부에서만 쓰는 row/projection은 `infra.persistence.<context>.repository.query` 내부 type으로 둔다.
- 실행 모듈과 infra 사이로 노출되는 read-model query result는 `@ReadModel`을 붙여 domain model/API ResponseDTO/JPA projection과 구분한다.
  - 예: `@ReadModel MinPerformanceDate`
  - `@ReadModel`은 `module-contracts`의 `com.beat.contracts.common`에 둔다. 이 marker가 domain/global-utils/infra에 있으면 read/query contract가 특정 계층에 불필요하게 묶이므로 금지한다.
  - 이 marker는 Spring component나 JPA annotation이 아니므로 bean 등록, persistence mapping, serialization behavior를 바꾸지 않는다.

## 최종 목표

- 실행 모듈이 서로의 구현 패키지를 참조하지 않고도 계약만으로 연결된다.
- 포트와 전달 모델이 한 곳에 모여 모듈 간 경계를 명확히 유지한다.
- 현재 domain-coupled transitional contracts는 contract-local value/id/snapshot으로 줄여 `domain` compileOnly 의존을 제거할 수 있는 상태로 수렴한다.
- `module-contracts`는 가능한 한 안정적인 공유 API만 제공하고, 변경은 최소화한다.
