# Domain/Application ErrorCode Inventory

Issue #421 commit 1 baseline. This file is an inventory only: no Java/Kotlin source movement, import changes, or ownership changes are performed in this commit.

## Scope and source of truth

- Inventory source: `*ErrorCode` enums implementing `BaseErrorCode` and `*SuccessCode` enums implementing `BaseSuccessCode` under `domain/src/main/java/**/exception`, plus the existing auth contract `module-contracts/src/main/java/com/beat/contracts/auth/TokenErrorCode.java`.
- `BaseErrorCode` contract: `global-utils/src/main/java/com/beat/global/common/exception/base/BaseErrorCode.java` exposes `getStatus()` and `getMessage()`.
- Usage scan command shape: `rg "<Enum>\.<CODE>" apis admin batch domain gateway infra module-contracts src`, excluding the enum declaration line.
- Success-code enums are included because #421 moves API response success messages out of `domain`.

## Classification key

| Classification | Meaning for the domain/application split |
| --- | --- |
| `domain invariant` | Production usage is inside `domain/src/main`; moving it requires a domain exception/error abstraction decision. |
| `hybrid domain + application` | Both domain model and executable/application code use the same code; split needs an alias/compatibility plan. |
| `application use-case` | Declared in `domain`, but production usage is in `apis`/`admin`/`batch`; candidate for application-owned ErrorCode. |
| `infra adapter use-case` | Declared in `domain`, but production usage is in `infra`; candidate for adapter/contract-owned code, not domain. |
| `shared contract / adapter auth` | Auth contract ErrorCode already lives in `module-contracts` and is consumed by adapter/support modules. |
| `shared contract / application auth` | Auth contract ErrorCode already lives in `module-contracts` and is consumed by executable application flow. |
| `unused / reserved` | No non-declaration references found; do not move blindly without confirming API compatibility or planned use. |
| `success response` | API response success message. Target owner is executable response boundary, not `domain`. |

## Summary

- Total ErrorCode constants inventoried: 48
- Total SuccessCode constants inventoried: 25
- ErrorCode HTTP status distribution: 400: 22, 401: 2, 403: 6, 404: 14, 409: 2, 500: 2
- SuccessCode HTTP status distribution: 200: 22, 201: 3
- ErrorCode classification distribution: application use-case: 24, domain invariant: 5, hybrid domain + application: 3, infra adapter use-case: 1, shared contract / adapter auth: 2, shared contract / application auth: 7, unused / reserved: 6
- SuccessCode classification distribution: success response: 25
- Primary migration hazard: `BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, and `ConflictException` handlers return the exception-type HTTP status, while the fallback `BeatException` handler returns `baseErrorCode.getStatus()`. Keep status/message behavior fixed when splitting ownership.

## Full inventory

### `BookingErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/booking/exception/BookingErrorCode.java`
- Current package: `com.beat.domain.booking.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `REQUIRED_DATA_MISSING` | 400 | 필수 데이터가 누락되었습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `INVALID_DATA_FORMAT` | 400 | 잘못된 데이터 형식입니다. | domain invariant | prod 1 (domain:1); test 1 | Thrown from domain model; should stay domain-owned or move with domain exception abstraction only. |
| `INVALID_REQUEST_FORMAT` | 400 | 잘못된 요청 형식입니다. | application use-case | prod 4 (apis:4); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `NO_BOOKING_FOUND` | 404 | 입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요. | application use-case | prod 7 (apis:7); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `NO_TICKETS_FOUND` | 404 | 입력하신 정보와 일치하는 예매자 목록이 없습니다. | unused / reserved | none | No non-declaration references found; confirm before moving or deleting. |
| `NO_PERFORMANCE_FOUND` | 404 | 공연을 찾을 수 없습니다. | unused / reserved | none | No non-declaration references found; confirm before moving or deleting. |
| `NO_SCHEDULE_FOUND` | 404 | 회차를 찾을 수 없습니다. | unused / reserved | none | No non-declaration references found; confirm before moving or deleting. |

### `TicketErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/booking/exception/TicketErrorCode.java`
- Current package: `com.beat.domain.booking.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED` | 400 | 이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `SEARCH_WORD_TOO_SHORT` | 400 | 검색어는 최소 2글자 이상이어야 합니다. | application use-case | prod 1 (apis:1); test 2 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `DELETED_TICKET_RETRIEVE_NOT_ALLOWED` | 400 | 삭제된 예매자를 조회할 수 없습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `CastErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/cast/exception/CastErrorCode.java`
- Current package: `com.beat.domain.cast.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `CAST_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 등장인물은 해당 공연에 속해 있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `CAST_NOT_FOUND` | 404 | 등장인물이 존재하지 않습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `MemberErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/member/exception/MemberErrorCode.java`
- Current package: `com.beat.domain.member.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `SOCIAL_TYPE_BAD_REQUEST` | 400 | 로그인 요청이 유효하지 않습니다. | infra adapter use-case | prod 1 (infra:1); test 0 | Declared in domain but adapter layer uses it; candidate for contract/application/adapter-owned code, not domain. |
| `MEMBER_NOT_FOUND` | 404 | 회원이 없습니다 | application use-case | prod 12 (admin:1, apis:11); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `PerformanceErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/performance/exception/PerformanceErrorCode.java`
- Current package: `com.beat.domain.performance.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `REQUIRED_DATA_MISSING` | 400 | 필수 데이터가 누락되었습니다. | unused / reserved | none | No non-declaration references found; confirm before moving or deleting. |
| `INVALID_DATA_FORMAT` | 400 | 잘못된 데이터 형식입니다. | domain invariant | prod 2 (domain:2); test 4 | Thrown from domain model; should stay domain-owned or move with domain exception abstraction only. |
| `INVALID_REQUEST_FORMAT` | 400 | 잘못된 요청 형식입니다. | unused / reserved | none | No non-declaration references found; confirm before moving or deleting. |
| `PRICE_UPDATE_NOT_ALLOWED` | 400 | 예매자가 존재하여 가격을 수정할 수 없습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `NEGATIVE_TICKET_PRICE` | 400 | 티켓 가격은 음수일 수 없습니다. | domain invariant | prod 1 (domain:1); test 2 | Thrown from domain model; should stay domain-owned or move with domain exception abstraction only. |
| `MAX_SCHEDULE_LIMIT_EXCEEDED` | 400 | 공연 회차는 최대 10개까지 추가할 수 있습니다. | application use-case | prod 3 (apis:3); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `PAST_SCHEDULE_NOT_ALLOWED` | 400 | 과거 날짜 회차를 포함한 공연을 생성할 수 없습니다. | application use-case | prod 3 (apis:3); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `SCHEDULE_MODIFICATION_NOT_ALLOWED_FOR_ENDED_SCHEDULE` | 400 | 종료된 회차를 수정할 수 없습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `INVALID_TICKET_COUNT` | 400 | 판매된 티켓 수보다 적은 수로 판매할 티켓 매수를 수정할 수 없습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `PERFORMANCE_DELETE_FAILED` | 403 | 예매자가 1명 이상 있을 경우, 공연을 삭제할 수 없습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `NOT_PERFORMANCE_OWNER` | 403 | 해당 공연의 메이커가 아닙니다. | hybrid domain + application | prod 4 (apis:3, domain:1); test 0 | Selected #421 decision treats actor/owner validation as application responsibility. Later commit must remove the domain call-site before moving this code. |
| `PERFORMANCE_NOT_FOUND` | 404 | 해당 공연 정보를 찾을 수 없습니다. | application use-case | prod 12 (admin:1, apis:11); test 1 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `SCHEDULE_LIST_NOT_FOUND` | 404 | 스케쥴 리스트에 스케쥴이 없습니다. | hybrid domain + application | prod 1 (domain:1); test 0 | Current throw site is domain, but selected #421 decision treats API-visible empty schedule list as application lookup/flow language. Later commit must move/check the empty-list guard at the application boundary or replace the domain throw with a domain-neutral invariant. |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류입니다. | unused / reserved | none | No non-declaration references found; confirm before moving or deleting. |

### `PerformanceImageErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/performanceimage/exception/PerformanceImageErrorCode.java`
- Current package: `com.beat.domain.performanceimage.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 싱세이미지는 해당 공연에 속해 있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `PERFORMANCE_IMAGE_NOT_FOUND` | 404 | 해당 공연 상세이미지를 찾을 수 없습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `PromotionErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/promotion/exception/PromotionErrorCode.java`
- Current package: `com.beat.domain.promotion.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PROMOTION_NOT_FOUND` | 404 | 해당 홍보 정보를 찾을 수 없습니다. | application use-case | prod 1 (admin:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `ScheduleErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/schedule/exception/ScheduleErrorCode.java`
- Current package: `com.beat.domain.schedule.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `INVALID_DATA_FORMAT` | 400 | 잘못된 데이터 형식입니다. | hybrid domain + application | prod 3 (apis:1, domain:2); test 7 | Used by domain invariant and executable use case; split needs a compatibility/alias plan before moving. |
| `SCHEDULE_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 스케줄은 해당 공연에 속해 있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `NO_SCHEDULE_FOUND` | 404 | 해당 회차를 찾을 수 없습니다. | application use-case | prod 11 (apis:11); test 2 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `INSUFFICIENT_TICKETS` | 409 | 요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요. | hybrid domain + application | prod 4 (apis:3, domain:1); test 2 | Used by domain invariant and executable use case; split needs a compatibility/alias plan before moving. |
| `EXCESS_TICKET_DELETE` | 409 | 예매된 티켓 수 이상을 삭제할 수 없습니다. | domain invariant | prod 1 (domain:1); test 1 | Thrown from domain model; should stay domain-owned or move with domain exception abstraction only. |

### `StaffErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/staff/exception/StaffErrorCode.java`
- Current package: `com.beat.domain.staff.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `STAFF_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 스태프는 해당 공연에 속해있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `STAFF_NOT_FOUND` | 404 | 스태프가 존재하지 않습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `UserErrorCode`

- Current file: `domain/src/main/java/com/beat/domain/user/exception/UserErrorCode.java`
- Current package: `com.beat.domain.user.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `USER_NOT_FOUND` | 404 | 유저가 없습니다 | application use-case | prod 6 (apis:6); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `TokenErrorCode`

- Current file: `module-contracts/src/main/java/com/beat/contracts/auth/TokenErrorCode.java`
- Current package: `com.beat.contracts.auth`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `AUTHENTICATION_CODE_EXPIRED` | 401 | 인가코드가 만료되었습니다 | shared contract / adapter auth | prod 10 (infra:10); test 0 | Auth-related code already lives outside domain; keep out of domain/application split unless contract is redesigned. |
| `REFRESH_TOKEN_NOT_FOUND` | 404 | 리프레쉬 토큰이 존재하지 않습니다 | shared contract / adapter auth | prod 2 (gateway:2); test 2 | Auth-related code already lives outside domain; keep out of domain/application split unless contract is redesigned. |
| `INVALID_REFRESH_TOKEN_ERROR` | 400 | 잘못된 리프레쉬 토큰입니다 | shared contract / application auth | prod 3 (apis:3); test 2 | Auth application flow consumes contract-level code; not a domain-owned candidate. |
| `REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR` | 400 | 리프레쉬 토큰의 사용자 정보가 일치하지 않습니다 | shared contract / application auth | prod 1 (apis:1); test 0 | Auth application flow consumes contract-level code; not a domain-owned candidate. |
| `REFRESH_TOKEN_EXPIRED_ERROR` | 401 | 리프레쉬 토큰이 만료되었습니다 | shared contract / application auth | prod 1 (apis:1); test 0 | Auth application flow consumes contract-level code; not a domain-owned candidate. |
| `REFRESH_TOKEN_SIGNATURE_ERROR` | 400 | 리프레쉬 토큰의 서명의 잘못 되었습니다 | shared contract / application auth | prod 1 (apis:1); test 0 | Auth application flow consumes contract-level code; not a domain-owned candidate. |
| `UNSUPPORTED_REFRESH_TOKEN_ERROR` | 400 | 지원하지 않는 리프레쉬 토큰입니다 | shared contract / application auth | prod 1 (apis:1); test 0 | Auth application flow consumes contract-level code; not a domain-owned candidate. |
| `REFRESH_TOKEN_EMPTY_ERROR` | 400 | 리프레쉬 토큰이 비어있습니다 | shared contract / application auth | prod 1 (apis:1); test 0 | Auth application flow consumes contract-level code; not a domain-owned candidate. |
| `UNKNOWN_REFRESH_TOKEN_ERROR` | 500 | 알 수 없는 리프레쉬 토큰 오류가 발생했습니다 | shared contract / application auth | prod 1 (apis:1); test 0 | Auth application flow consumes contract-level code; not a domain-owned candidate. |

## SuccessCode inventory

All current domain `*SuccessCode` constants are response-boundary messages. They are target candidates for executable response ownership, not domain ownership. Commit 1 records the current values only; later commits move packages/imports.

### `BookingSuccessCode`

- Current file: `domain/src/main/java/com/beat/domain/booking/exception/BookingSuccessCode.java`
- Current package: `com.beat.domain.booking.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `MEMBER_BOOKING_RETRIEVE_SUCCESS` | 200 | 회원 예매 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `GUEST_BOOKING_RETRIEVE_SUCCESS` | 200 | 비회원 예매 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `BOOKING_REFUND_SUCCESS` | 200 | 예매자의 환불요청이 성공했습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `BOOKING_CANCEL_SUCCESS` | 200 | 예매자의 취소요청이 성공했습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `MEMBER_BOOKING_SUCCESS` | 201 | 회원 예매가 성공적으로 완료되었습니다 | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `GUEST_BOOKING_SUCCESS` | 201 | 비회원 예매가 성공적으로 완료되었습니다 | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |

### `TicketSuccessCode`

- Current file: `domain/src/main/java/com/beat/domain/booking/exception/TicketSuccessCode.java`
- Current package: `com.beat.domain.booking.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `TICKET_RETRIEVE_SUCCESS` | 200 | 예매자 목록 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `TICKET_UPDATE_SUCCESS` | 200 | 예매자 입금여부 수정이 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `TICKET_REFUND_SUCCESS` | 200 | 예매 환불처리 요청이 성공했습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `TICKET_DELETE_SUCCESS` | 200 | 예매자 삭제 요청이 성공했습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |
| `TICKET_SEARCH_SUCCESS` | 200 | 예매자 검색 결과 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/booking` response boundary with value unchanged. |

### `MemberSuccessCode`

- Current file: `domain/src/main/java/com/beat/domain/member/exception/MemberSuccessCode.java`
- Current package: `com.beat.domain.member.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `SIGN_UP_SUCCESS` | 200 | 로그인 성공 | success response | apis controller response | Move to `apis/member` response boundary with value unchanged. |
| `ISSUE_ACCESS_TOKEN_SUCCESS` | 200 | 엑세스토큰 발급 성공 | success response | currently reserved/legacy response | Move or remove only after confirming consumer compatibility. |
| `ISSUE_ACCESS_TOKEN_USING_REFRESH_TOKEN` | 200 | 리프레쉬 토큰으로 액세스 토큰 재발급 성공 | success response | apis controller response | Move to `apis/member` response boundary with value unchanged. |
| `SIGN_OUT_SUCCESS` | 200 | 로그아웃 성공 | success response | apis controller response | Move to `apis/member` response boundary with value unchanged. |
| `USER_DELETE_SUCCESS` | 200 | 회원 탈퇴 성공 | success response | currently reserved/legacy response | Move or remove only after confirming consumer compatibility. |

### `PerformanceSuccessCode`

- Current file: `domain/src/main/java/com/beat/domain/performance/exception/PerformanceSuccessCode.java`
- Current package: `com.beat.domain.performance.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PERFORMANCE_UPDATE_SUCCESS` | 200 | 공연이 성공적으로 수정되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 공연 상세 정보 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_MODIFY_PAGE_SUCCESS` | 200 | 공연 수정 페이지 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_DELETE_SUCCESS` | 200 | 공연이 성공적으로 삭제되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |
| `BOOKING_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 예매 관련 공연 정보 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |
| `HOME_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 홈 화면 공연 목록 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |
| `MAKER_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 회원이 등록한 공연 목록의 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_CREATE_SUCCESS` | 201 | 공연이 성공적으로 생성되었습니다. | success response | apis controller response | Move to `apis/performance` response boundary with value unchanged. |

### `ScheduleSuccessCode`

- Current file: `domain/src/main/java/com/beat/domain/schedule/exception/ScheduleSuccessCode.java`
- Current package: `com.beat.domain.schedule.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `TICKET_AVAILABILITY_RETRIEVAL_SUCCESS` | 200 | 티켓 수량 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Move to `apis/schedule` response boundary with value unchanged. |

## Cross-cutting hazards to preserve

- Do not move source or rewrite imports in the inventory commit; later commits should use this file as the source list for split candidates.
- Duplicate generic codes/messages exist across `BookingErrorCode`, `PerformanceErrorCode`, and `ScheduleErrorCode` (`INVALID_DATA_FORMAT`, request/data missing/format variants). Split work should avoid changing client-visible messages unless explicitly planned.
- `BookingErrorCode.NO_PERFORMANCE_FOUND` and `BookingErrorCode.NO_SCHEDULE_FOUND` are currently unused while `PerformanceErrorCode.PERFORMANCE_NOT_FOUND` and `ScheduleErrorCode.NO_SCHEDULE_FOUND` are actively used.
- `TokenErrorCode` is already outside `domain`; it still depends on `BaseErrorCode` from `global-utils`, so a future split should decide whether auth codes stay contract-level or become application/support-level.
- Domain model Kotlin files currently throw `BadRequestException`, `ForbiddenException`, or `ConflictException` with domain ErrorCodes. If future work makes domain independent from `global-utils`, replace exception/error boundaries atomically with tests.
