# module-contracts module

> 이 문서는 `module-contracts` 모듈의 현재 계약 surface, 목표 계약, 그리고 #384에서 수행하지 않는 후속 작업을 구분한다. `module-contracts`는 실행 모듈과 인프라가 공유하는 경량 계약 경계이며, 구현 대신 인터페이스와 전달 모델만 소유한다.

## Migration status

| Current | Target | Deferred-to-issue |
| --- | --- | --- |
| Contract surface intentionally remains mostly Java with an explicitly allowed Kotlin port-failure type and exposes auth, booking read-model, notification, schedule, SMS, and storage ports/transfer models. Issue #426 이후 `domain` 직접 의존은 제거되었고, 공유 계약은 contract-local enum/value/read model만 노출한다. | Stable implementation-free API; domain model / domain enum / JPA entity / API ResponseDTO를 직접 노출하지 않는다. | 추가 contract 세분화와 언어 전환은 별도 이슈에서 검토한다. |

## 역할

- 실행 모듈(`apis`, `admin`, `batch`)과 `infra`가 공통으로 참조하는 계약을 소유한다.
- 포트, command, response, value object 성격의 공유 타입만 제공한다.
- 구현 세부사항이나 기술 스택 세부사항을 모르고, 모듈 간 결합을 낮추는 경계 역할을 한다.

## 허용 의존성

- `global-support`
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
      TokenErrorCode.java                 # contract-local token error enum; global-support BaseErrorCode contract만 구현
      TokenValidationResult.java           # contract-local token validation result enum
      social/
        SocialLoginRequest.java
        SocialLoginPort.java
        SocialLoginType.java                    # contract-local social provider enum
        SocialMemberInfo.java
    booking/
      MakerTicketReadPort.java
      readmodel/
        MakerTicketBookingStatus.java       # contract-local booking status enum; domain BookingStatus 노출 없음
        MakerTicketListItemReadModel.java        # @ReadModel; maker ticket list/search query result
        MakerTicketScheduleNumber.java      # contract-local schedule number enum; domain ScheduleNumber 노출 없음
    notification/
      BookingNotification.java
      BookingNotificationPort.java
      MemberNotification.java
      MemberNotificationPort.java
    schedule/
      ScheduleBookingCloseJobPort.java
      ScheduleBookingCloseJobTarget.java                    # scheduler job boundary target; domain Schedule 노출 없음
      ScheduleReadPort.java
      readmodel/
        MinPerformanceDateReadModel.java            # @ReadModel; Schedule 최소 공연 일자 query result
    sms/
      SmsMessage.java
      SmsPort.java
    storage/
      BannerPresignedUrl.java
      CarouselPresignedUrls.java
      FileStoragePort.java
      PerformancePresignedUrls.java
  src/main/kotlin/com/beat/contracts/
    auth/social/
      SocialLoginFailure.kt                 # port-level social login failure; nested Reason enum을 application boundary가 API ErrorCode로 번역
```

설명:

- 현재 `module-contracts`는 `auth`, `booking`, `notification`, `schedule`, `sms`, `storage` 계약을 모아두는 얇은 공유 모듈이다.
- 구현체는 다른 모듈에 있고, 이 모듈은 계약 타입만 제공한다.
- Issue #378 결정: contract surface는 cross Java/Kotlin consumer API 안정성이 우선이다. 대부분 Java source를 유지하되, `SocialLoginFailure.kt`처럼 port-level failure 표현에 한해 명시적으로 허용된 Kotlin contract가 있을 수 있다.
- `build.gradle.kts`에서 `global-support`만 `compileOnly`로 참조한다. `SocialType`, `Schedule` 같은 domain-coupled contract type은 Issue #426에서 `SocialLoginType`, `ScheduleBookingCloseJobTarget`으로 치환했다.
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
      SocialLoginRequest
      SocialLoginPort
      SocialLoginType
      SocialMemberInfo
      SocialLoginFailure
  booking/
    MakerTicketReadPort
    readmodel/
      MakerTicketBookingStatus
      MakerTicketListItemReadModel
      MakerTicketScheduleNumber
  notification/
    BookingNotification
    BookingNotificationPort
    MemberNotification
    MemberNotificationPort
  schedule/
    ScheduleBookingCloseJobPort
    ScheduleBookingCloseJobTarget
    ScheduleReadPort
    readmodel/
      MinPerformanceDateReadModel
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
- Issue #426 이후 `SocialLoginRequest`는 `SocialLoginType` contract enum만 받고, `SocialMemberInfo`는 외부 provider profile 결과(`socialId`, `nickname`, `email`)만 반환한다. `SocialLoginFailure`는 infra adapter-local failure를 application boundary에서 API-facing ErrorCode로 번역하기 위한 port-level failure다. `ScheduleBookingCloseJobPort`는 `ScheduleBookingCloseJobTarget` contract value만 노출한다. contract와 domain enum/model 사이 변환은 실행 모듈 application boundary가 담당한다.


### Read-model contract rule

ReadModel은 Domain model을 조회 화면에 억지로 맞추지 않기 위한 조회 전용 shape다. `module-contracts`는 모든 read model의 기본 위치가 아니라, 실행 모듈과 infra 사이에 구현 없는 query 계약이 필요할 때만 read model contract를 소유한다.

- `*ReadPort`: 실행 모듈 query service가 의존하는 조회 port.
- `*ReadModel`: query adapter가 반환하는 조회 결과. save 대상이 아니며 domain invariant를 책임지지 않는다.
- `*SearchCondition`: 검색/필터/정렬 조건이 많아져 파라미터 직접 전달보다 의미가 분명해질 때만 도입한다. 단순 조회 조건은 port 메서드 파라미터로 직접 전달해 불필요한 wrapper를 만들지 않는다. 도입 시에도 Spring Data `Pageable`, `Sort`, QueryDSL type을 노출하지 않는다.
- 신규 read contract는 primitive/JDK type 또는 contract-local enum/value만 사용한다.
- API ResponseDTO와 1:1로 보이더라도 재사용하지 않는다. ResponseDTO는 `apis`, `admin`, `batch` 각 실행 모듈이 각자 소유한다.
- 특정 실행 모듈 내부에서만 쓰고 infra 구현 계약이 필요 없는 조립 결과는 해당 실행 모듈 `application/dto/result` 또는 query service 내부 type에 둔다.
- infra query implementation 내부에서만 쓰는 row/projection은 `infra.persistence.<context>.repository.query` 내부 type으로 둔다.
- 실행 모듈과 infra 사이로 노출되는 read-model query result는 `@ReadModel`을 붙여 domain model/API ResponseDTO/JPA projection과 구분한다.
  - 예: `@ReadModel MinPerformanceDateReadModel`, `@ReadModel MakerTicketListItemReadModel`
  - `@ReadModel`은 `module-contracts`의 `com.beat.contracts.common`에 둔다. 이 marker가 domain/global-support/infra에 있으면 read/query contract가 특정 계층에 불필요하게 묶이므로 금지한다.
  - 이 marker는 Spring component나 JPA annotation이 아니므로 bean 등록, persistence mapping, serialization behavior를 바꾸지 않는다.

## 최종 목표

- 실행 모듈이 서로의 구현 패키지를 참조하지 않고도 계약만으로 연결된다.
- 포트와 전달 모델이 한 곳에 모여 모듈 간 경계를 명확히 유지한다.
- `domain` compileOnly 의존 없이 contract-local value/id/read model만으로 실행 모듈과 infra를 연결한다.
- `module-contracts`는 가능한 한 안정적인 공유 API만 제공하고, 변경은 최소화한다.
