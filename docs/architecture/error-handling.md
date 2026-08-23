# BEAT error handling

BEAT의 오류 모델은 실패의 소유권과 HTTP 표현을 분리한다. 예외 타입이나 error code enum에 HTTP status를 중복 저장하지 않는다.

## Layer ownership

| Layer | Owns | Does not own |
| --- | --- | --- |
| Domain | invariant failure의 stable `code`, `DomainErrorType`, safe message | HTTP, Spring, response DTO, repository lookup/permission failure |
| Application | 실행 모듈별 use-case failure의 stable `code`, module-local `ApplicationErrorType`, safe message | `HttpStatus`, persistence/vendor exception detail |
| Infrastructure | known DB/provider failure translation, technical exception | API status/message, application enum |
| Web adapter | `type -> HttpStatus`, response envelope, unexpected error sanitization | domain/application rule 판단 |

Domain은 domain 소유의 `DomainException(DomainErrorCode)`을 사용한다. `apis` application은 `ApiApplicationException(ApplicationErrorCode)`, `admin` application은 `AdminApplicationException(ApplicationErrorCode)`을 사용하며 두 `ApplicationErrorCode`/`ApplicationErrorType`도 각 실행 모듈의 로컬 계약이다. `global-support`는 응답 envelope와 `SuccessCode`만 제공하고 application exception이나 HTTP 매핑을 소유하지 않는다. HTTP status별 exception subclass는 만들지 않는다.

Exception type은 복구 방식의 차이를 나타낼 때만 분리한다. 호출자가 특정 실패만 catch해서 재조회, retry, fallback 또는 멱등 복구를 수행해야 하거나 framework가 타입을 기준으로 처리할 때는 구체 exception/contract failure를 둔다. 최종 처리 방식은 같고 실패 사유만 다르면 공통 exception과 구체적인 stable ErrorCode를 사용한다. 따라서 ErrorCode 하나마다 내용 없는 exception subclass를 만들지 않는다.

예상 가능한 4xx domain/application 실패는 `code`와 변환된 HTTP status만 구조화해 info로 기록한다. 분류된 upstream 5xx는 infra contract failure와 `ApiApplicationException` 또는 `AdminApplicationException` cause chain에 원인을 보존해 내부 분류·재시도·진단에 사용한다. 다만 client response와 global log에는 raw cause/message/stack을 노출하지 않고 안전한 `code`/status만 남긴다. 예상하지 못한 `INTERNAL_ERROR`만 stack trace와 observation error를 남긴다. DB/provider message, token, 개인정보와 요청 원문은 응답이나 global 오류 로그 필드에 넣지 않는다.

## Application error type mapping

각 실행 모듈의 global exception handler가 자신의 module-local `ApplicationErrorType`에 대해 아래 매핑을 소유한다.

| ApplicationErrorType | HTTP | Meaning |
| --- | ---: | --- |
| `INVALID_INPUT` | 400 | 형식·필수값·허용 범위를 만족하지 않는 요청 |
| `UNAUTHENTICATED` | 401 | 인증 정보가 없거나 유효하지 않음 |
| `FORBIDDEN` | 403 | 인증됐지만 해당 행위를 수행할 권한이 없음 |
| `NOT_FOUND` | 404 | 요청한 리소스 또는 aggregate child가 없음 |
| `STATE_CONFLICT` | 409 | 현재 리소스 상태와 요청한 전이가 충돌함 |
| `UPSTREAM_FAILURE` | 502 | 외부 응답을 정상적으로 해석하거나 처리할 수 없음 |
| `UPSTREAM_UNAVAILABLE` | 503 | 외부 서비스 연결 실패 또는 5xx 응답 |
| `UPSTREAM_TIMEOUT` | 504 | 외부 서비스 응답 시간 초과 |
| `INTERNAL_ERROR` | 500 | 안전하게 공개할 수 있는 application 내부 실패 |

`IllegalArgumentException`을 일괄 400으로 변환하지 않는다. 프로그래머 계약 위반과 저장 데이터 오류까지 클라이언트 책임으로 오분류되기 때문이다. API 입력 오류는 validation/binding 또는 명시적인 application error로 표현한다.

## Stable error code

- `code`는 enum 이름에서 자동 생성하지 않고 명시한다.
- 이미 소비자에게 공개된 code는 enum/class rename과 무관하게 유지한다.
- `message`는 사람에게 보여주는 안전한 문구이며 클라이언트 분기 기준이 아니다.
- raw DB/provider exception message, SQL, token, 개인정보는 response에 포함하지 않는다.

현재 client 계약은 `{status, message}`를 유지한다. `code` 응답 필드와 RFC 9457 Problem Details는 소비자 역직렬화/OpenAPI/contract test를 확인한 별도 API 계약 변경으로 진행한다. 내부 stable code 도입이 response shape 변경을 자동 승인하지 않는다.

의미상 `STATE_CONFLICT`/`NOT_FOUND`/`UNAUTHENTICATED`로 바로잡은 기존 API 오류 중 과거에 400/403/404로 응답하던 항목은 `apis` web adapter의 명시적 v1 compatibility mapping으로 이전 status를 유지한다. 여기에는 저장된 refresh token 부재의 기존 404도 포함한다. 새 계약에서 409/404/401로 바꾸려면 API version 또는 공지된 deprecation/소비자 전환 절차를 먼저 거친다. Admin은 별도 공개 v1 override 없이 의미 기반 matrix를 따른다.

## Catch and translation

예외는 다음 중 하나를 수행할 때만 잡는다.

1. 알려진 기술 실패를 port/application 언어로 번역한다.
2. 실제 복구 또는 fallback을 수행한다.
3. transaction commit 이후 best-effort 부수 효과 실패를 격리한다.

알 수 없는 예외를 400/404 같은 client error로 바꾸지 않는다. catch-log-rethrow로 같은 실패를 중복 기록하지 않는다. 예상하지 못한 예외는 global handler가 500으로 가리고 observation에 기록한다.

Spring MVC가 직접 만드는 binding/media-type/method/JSON parsing 예외는 `ResponseEntityExceptionHandler` 확장 지점에서 처리해 동일한 `{status, message}` envelope를 유지한다. 기존 정적 리소스 404의 빈 body 계약도 별도 override로 보존한다.

저장소·외부 연동 port는 HTTP 또는 실행 모듈의 `ApiApplicationException`/`AdminApplicationException`을 던지지 않는다. 예를 들어 refresh-token 조회 부재는 `OptionalLong.empty()`로 반환하고 application use case가 토큰 오류로 번역한다. 로그아웃 삭제는 멱등 처리한다. 알려진 외부 인증 실패는 contract failure로 분류한 뒤 application layer에서 401/502/503/504 의미로 번역한다.

Spring transaction command는 `RuntimeException` 전파를 기본으로 한다. Kotlin `runCatching`은 외부 호출의 국소 번역·복구처럼 결과를 즉시 소비하는 경계에만 사용하며, application service 전체를 감싸 예외를 삼키지 않는다.

## Verification

- 모든 application code에 고유하고 명시적인 `code`가 있는지 검사한다.
- API/Admin handler가 같은 type/status matrix를 따르는지 contract test로 검증한다.
- 상태 충돌, 권한, nested resource 부재가 각각 409/403/404인지 검증한다.
- 예상하지 못한 예외의 message가 response에 노출되지 않는지 검증한다.
- domain이 Spring/global-support를 import하지 않는지 architecture test로 검증한다.
- transactional command의 예외가 rollback되는지 핵심 유스케이스에서 검증한다.

## References

- [Spring Framework — Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring Framework — Rolling Back a Declarative Transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Google AIP-193 — Errors](https://google.aip.dev/193)
- [Toss Tech — Kotlin으로 안전하게 실패 다루기: Result 패턴](https://toss.tech/article/kotlin-result)
