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

- `domain`
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

## To-Be 패키지 구조

```text
com.beat.contracts/
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
- 이 모듈은 기능별 계약만 담고, 더 깊은 계층화는 만들지 않는다.
- 계약이 늘어나더라도 역할별 패키지 분리만 유지하고, 구현 계층을 끼워 넣지 않는다.

## 최종 목표

- 실행 모듈이 서로의 구현 패키지를 참조하지 않고도 계약만으로 연결된다.
- 포트와 전달 모델이 한 곳에 모여 모듈 간 경계를 명확히 유지한다.
- `module-contracts`는 가능한 한 안정적인 공유 API만 제공하고, 변경은 최소화한다.
