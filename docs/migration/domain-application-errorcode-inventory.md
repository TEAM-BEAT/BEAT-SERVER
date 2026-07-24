# Domain/Application ErrorCode Inventory

Issue #421 당시의 inventory와 status/message migration snapshot입니다. 이후 ownership과 상수는 변경됐으므로 이 문서는 현재 source of truth가 아닙니다. 현재 기준은 [error handling guide](../architecture/error-handling.md)이며 아래의 `Current` 표기는 snapshot 작성 시점을 뜻합니다.

## Historical snapshot scope

- 현재 소유권은 [`../../domain/README.md`](../../domain/README.md)와 실제 enum/handler 코드를 기준으로 판단합니다. 이 표를 신규 변경의 allowlist로 사용하지 않습니다.
- Snapshot source: 당시 domain/application/success ErrorCode와 auth contract를 대상으로 수집했습니다.
- Snapshot 당시 `BaseErrorCode` contract는 `global-support/src/main/kotlin/com/beat/global/support/exception/base/BaseErrorCode.kt`에 있었고 `getStatus()`와 `getMessage()`를 노출했습니다. 현재 global-support는 이 오류 계약을 소유하지 않습니다.
- Usage scan command shape: `rg "<Enum>\.<CODE>" apis admin batch domain gateway infra module-contracts src`, excluding the enum declaration line.
- Success-code enums are included because #421 moved API response success messages out of `domain` and into executable response boundaries.

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

- Original ErrorCode constants inventoried: 48
- Original SuccessCode constants inventoried: 25
- Original ErrorCode HTTP status distribution: 400: 22, 401: 2, 403: 6, 404: 14, 409: 2, 500: 2
- Original SuccessCode HTTP status distribution: 200: 22, 201: 3
- Original ErrorCode classification distribution: application use-case: 24, domain invariant: 5, hybrid domain + application: 3, infra adapter use-case: 1, shared contract / adapter auth: 2, shared contract / application auth: 7, unused / reserved: 6
- Original SuccessCode classification distribution: success response: 25
- Primary migration hazard: `BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, and `ConflictException` handlers return the exception-type HTTP status, while the fallback `BeatException` handler returns `baseErrorCode.getStatus()`. Keep status/message behavior fixed when splitting ownership.

## Full inventory

### `BookingErrorCode`

- Current file: `domain/src/main/kotlin/com/beat/domain/booking/exception/BookingErrorCode.kt`
- Current package: `com.beat.domain.booking.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `INVALID_PURCHASE_TICKET_COUNT` | 400 | 구매 티켓 수량은 0보다 커야 합니다. | domain invariant | booking creation | Domain-owned aggregate invariant. The v1 handler preserves the former public message. |
| `INVALID_REFUND_ACCOUNT` | 400 | 환불 계좌 정보는 모두 입력하거나 모두 비워야 합니다. | domain invariant | refund request | Domain-owned value-object invariant. The v1 handler preserves the former public message. |
| `PAYMENT_CONFIRMATION_NOT_ALLOWED` | 409 (public v1: 400) | 현재 예매 상태에서는 결제를 확정할 수 없습니다. | domain state transition | payment confirmation | Domain owns the transition; the HTTP adapter preserves the v1 response contract. |
| `REFUND_REQUEST_NOT_ALLOWED` | 409 (public v1: 400) | 현재 예매 상태에서는 환불을 요청할 수 없습니다. | domain state transition | refund request | Domain owns the transition; the HTTP adapter preserves the v1 response contract. |

### `TicketApplicationErrorCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/ticket/exception/TicketApplicationErrorCode.kt`
- Current package: `com.beat.apis.ticket.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED` | 400 | 이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다. | application use-case | prod 1 (apis:1); test 0 | Moved to ticket application boundary with value unchanged. |
| `SEARCH_WORD_TOO_SHORT` | 400 | 검색어는 최소 2글자 이상이어야 합니다. | application use-case | prod 1 (apis:1); test 2 | Moved to ticket application boundary with value unchanged. |
| `DELETED_TICKET_RETRIEVE_NOT_ALLOWED` | 400 | 삭제된 예매자를 조회할 수 없습니다. | application use-case | prod 2 (apis:2); test 0 | Moved to ticket application boundary with value unchanged. |
| `NO_TICKETS_FOUND` | 404 | 입력하신 정보와 일치하는 예매자 목록이 없습니다. | application flow | prod 0; test 1 | Ticket lookup/search response language; owned by ticket application boundary. |

### `CastApplicationErrorCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/performance/exception/CastApplicationErrorCode.kt`
- Current package: `com.beat.apis.performance.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `CAST_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 등장인물은 해당 공연에 속해 있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `CAST_NOT_FOUND` | 404 | 등장인물이 존재하지 않습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `MemberApplicationErrorCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/member/exception/MemberApplicationErrorCode.kt`
- Current package: `com.beat.apis.member.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `SOCIAL_TYPE_BAD_REQUEST` | 400 | 로그인 요청이 유효하지 않습니다. | infra adapter use-case | prod 1 (infra:1); test 0 | Declared in domain but adapter layer uses it; candidate for contract/application/adapter-owned code, not domain. |
| `MEMBER_NOT_FOUND` | 404 | 회원이 없습니다 | application use-case | prod 12 (admin:1, apis:11); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `PerformanceErrorCode`

- Current file: `domain/src/main/kotlin/com/beat/domain/performance/exception/PerformanceErrorCode.kt`
- Current package: `com.beat.domain.performance.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `NON_POSITIVE_RUNNING_TIME` | 400 | 러닝타임은 0보다 커야 합니다. | domain invariant | `RunningTime` creation | Domain-owned value-object invariant. The v1 handler preserves the existing public message. |
| `NEGATIVE_SCHEDULE_COUNT` | 400 | 공연 회차 수는 음수일 수 없습니다. | domain invariant | `Performance` creation and rehydration | Domain-owned aggregate invariant. The v1 handler preserves the existing public message. |
| `INVALID_PERFORMANCE_PERIOD` | 400 | 공연 종료일은 시작일보다 빠를 수 없습니다. | domain invariant | `PerformancePeriod` creation | Domain-owned value-object invariant. |
| `NEGATIVE_TICKET_PRICE` | 400 | 티켓 가격은 음수일 수 없습니다. | domain invariant | prod 1 (domain:1); test 2 | Thrown from domain model; should stay domain-owned or move with domain exception abstraction only. |
| `NEGATIVE_TICKET_QUANTITY` | 400 | 티켓 수량은 음수일 수 없습니다. | domain invariant | `TicketPrice` total-price calculation | Domain-owned value-object invariant. |
| `INCOMPLETE_PAYMENT_ACCOUNT` | 400 | 정산 계좌 정보는 은행, 계좌번호, 예금주를 모두 입력해야 합니다. | domain invariant | `PaymentAccount` creation | Domain-owned value-object invariant. |
| `PRICE_UPDATE_NOT_ALLOWED` | 400 | 예매자가 존재하여 가격을 수정할 수 없습니다. | application use-case | application service | Application-owned orchestration rule. |
| `PAST_SCHEDULE_NOT_ALLOWED` | 400 | 과거 날짜 회차를 포함한 공연을 생성할 수 없습니다. | application use-case | prod 3 (apis:3); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `SCHEDULE_MODIFICATION_NOT_ALLOWED_FOR_ENDED_SCHEDULE` | 400 | 종료된 회차를 수정할 수 없습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `PERFORMANCE_DELETE_FAILED` | 403 | 예매자가 1명 이상 있을 경우, 공연을 삭제할 수 없습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `NOT_PERFORMANCE_OWNER` | 403 | 해당 공연의 메이커가 아닙니다. | application actor validation | prod application only | Moved to `PerformanceApplicationErrorCode`; domain exposes `isOwnedBy` boolean only. |
| `PERFORMANCE_NOT_FOUND` | 404 | 해당 공연 정보를 찾을 수 없습니다. | application use-case | prod 12 (admin:1, apis:11); test 1 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `SCHEDULE_LIST_NOT_FOUND` | 400 | 스케쥴 리스트에 스케쥴이 없습니다. | application flow | prod application only | Empty schedule input is rejected as a bad request before calling domain period formatting. |

### `PerformanceImageApplicationErrorCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/performance/exception/PerformanceImageApplicationErrorCode.kt`
- Current package: `com.beat.apis.performance.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 싱세이미지는 해당 공연에 속해 있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `PERFORMANCE_IMAGE_NOT_FOUND` | 404 | 해당 공연 상세이미지를 찾을 수 없습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `PromotionApplicationErrorCode` promotion lookup slice

- Current file: `admin/src/main/kotlin/com/beat/admin/promotion/exception/PromotionApplicationErrorCode.kt`
- Current package: `com.beat.admin.promotion.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PROMOTION_NOT_FOUND` | 404 | 해당 홍보 정보를 찾을 수 없습니다. | application use-case | prod 1 (admin:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `ScheduleErrorCode`

- Current file: `domain/src/main/kotlin/com/beat/domain/schedule/exception/ScheduleErrorCode.kt`
- Current package: `com.beat.domain.schedule.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `INVALID_BOOKING_WINDOW` | 400 | 예매 마감 시각은 공연 시작 시각보다 빠를 수 없습니다. | domain invariant | `Schedule` creation and rehydration | Domain-owned aggregate invariant. The v1 handler preserves the existing public message. |
| `NEGATIVE_TICKET_COUNT` | 400 | 티켓 수량은 음수일 수 없습니다. | domain invariant | `Schedule` creation and rehydration | Domain-owned aggregate invariant. The v1 handler preserves the existing public message. |
| `NON_POSITIVE_TICKET_COUNT` | 400 | 예매 티켓 수량은 0보다 커야 합니다. | domain invariant | ticket allocation and cancellation | Domain-owned aggregate invariant. The v1 handler preserves the existing public message. |
| `MIXED_PERFORMANCE_SCHEDULES` | 400 | 서로 다른 공연의 회차를 함께 배정할 수 없습니다. | domain service invariant | schedule sequence assignment | Domain-owned cross-entity invariant. |
| `TOO_MANY_SCHEDULES` | 400 | 지원 가능한 회차 수를 초과했습니다. | domain service invariant | schedule sequence assignment | Domain-owned cross-entity invariant. |
| `ALLOCATED_TICKETS_EXCEED_TOTAL` | 400 | 예매된 티켓 수는 전체 티켓 수를 초과할 수 없습니다. | domain invariant | `Schedule` creation, rehydration, and resize | Owned by the aggregate because allocated tickets cannot exceed its total capacity. |
| `SCHEDULE_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 스케줄은 해당 공연에 속해 있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `NO_SCHEDULE_FOUND` | 404 | 해당 회차를 찾을 수 없습니다. | application use-case | prod 11 (apis:11); test 2 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `INSUFFICIENT_TICKETS` | 409 | 요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요. | domain invariant + application availability alias | domain keeps allocation invariant; apis uses `ScheduleApplicationErrorCode.INSUFFICIENT_TICKETS` for availability response | Split completed with value unchanged at both boundaries. |
| `EXCESS_TICKET_DELETE` | 409 | 예매된 티켓 수 이상을 삭제할 수 없습니다. | domain invariant | prod 1 (domain:1); test 1 | Thrown from domain model; should stay domain-owned or move with domain exception abstraction only. |

Request-shape validation uses `ScheduleApplicationErrorCode.INVALID_TICKET_AVAILABILITY_REQUEST`; its HTTP status and public message remain unchanged from the former generic application code.

### `StaffApplicationErrorCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/performance/exception/StaffApplicationErrorCode.kt`
- Current package: `com.beat.apis.performance.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `STAFF_NOT_BELONG_TO_PERFORMANCE` | 403 | 해당 스태프는 해당 공연에 속해있지 않습니다. | application use-case | prod 1 (apis:1); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |
| `STAFF_NOT_FOUND` | 404 | 스태프가 존재하지 않습니다. | application use-case | prod 2 (apis:2); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `UserApplicationErrorCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/user/exception/UserApplicationErrorCode.kt`
- Current package: `com.beat.apis.user.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `USER_NOT_FOUND` | 404 | 유저가 없습니다 | application use-case | prod 6 (apis:6); test 0 | Declared in domain but only executable/application layer uses it; candidate for application-owned ErrorCode. |

### `TokenApplicationErrorCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/member/exception/TokenApplicationErrorCode.kt`
- Current package: `com.beat.apis.member.exception`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `REFRESH_TOKEN_NOT_FOUND` | 404(v1) | 리프레쉬 토큰이 존재하지 않습니다 | application auth | internal semantic type is `UNAUTHENTICATED`; web adapter preserves legacy 404. |
| `INVALID_REFRESH_TOKEN_ERROR` | 400(v1) | 잘못된 리프레쉬 토큰입니다 | application auth | internal semantic type is `UNAUTHENTICATED`; web adapter preserves legacy 400. |
| `REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR` | 400(v1) | 리프레쉬 토큰의 사용자 정보가 일치하지 않습니다 | application auth | internal semantic type is `UNAUTHENTICATED`; web adapter preserves legacy 400. |
| `REFRESH_TOKEN_EXPIRED_ERROR` | 401 | 리프레쉬 토큰이 만료되었습니다 | application auth | semantic and HTTP contracts already agree. |
| `REFRESH_TOKEN_SIGNATURE_ERROR` | 400(v1) | 리프레쉬 토큰의 서명의 잘못 되었습니다 | application auth | internal semantic type is `UNAUTHENTICATED`; web adapter preserves legacy 400. |
| `UNSUPPORTED_REFRESH_TOKEN_ERROR` | 400(v1) | 지원하지 않는 리프레쉬 토큰입니다 | application auth | internal semantic type is `UNAUTHENTICATED`; web adapter preserves legacy 400. |
| `REFRESH_TOKEN_EMPTY_ERROR` | 400(v1) | 리프레쉬 토큰이 비어있습니다 | application auth | internal semantic type is `UNAUTHENTICATED`; web adapter preserves legacy 400. |
| `UNKNOWN_REFRESH_TOKEN_ERROR` | 500 | 알 수 없는 리프레쉬 토큰 오류가 발생했습니다 | application auth | unexpected token processing failure. |

## SuccessCode inventory

All current domain `*SuccessCode` constants are response-boundary messages. They are target candidates for executable response ownership, not domain ownership. Commit 1 records the current values only; later commits move packages/imports.

### `BookingSuccessCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/booking/api/response/BookingSuccessCode.kt`
- Current package: `com.beat.apis.booking.api.response`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `MEMBER_BOOKING_RETRIEVE_SUCCESS` | 200 | 회원 예매 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/booking` response boundary with value unchanged. |
| `GUEST_BOOKING_RETRIEVE_SUCCESS` | 200 | 비회원 예매 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/booking` response boundary with value unchanged. |
| `BOOKING_REFUND_SUCCESS` | 200 | 예매자의 환불요청이 성공했습니다. | success response | apis controller response | Moved to `apis/booking` response boundary with value unchanged. |
| `BOOKING_CANCEL_SUCCESS` | 200 | 예매자의 취소요청이 성공했습니다. | success response | apis controller response | Moved to `apis/booking` response boundary with value unchanged. |
| `MEMBER_BOOKING_SUCCESS` | 201 | 회원 예매가 성공적으로 완료되었습니다 | success response | apis controller response | Moved to `apis/booking` response boundary with value unchanged. |
| `GUEST_BOOKING_SUCCESS` | 201 | 비회원 예매가 성공적으로 완료되었습니다 | success response | apis controller response | Moved to `apis/booking` response boundary with value unchanged. |

### `TicketSuccessCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/ticket/api/response/TicketSuccessCode.kt`
- Current package: `com.beat.apis.ticket.api.response`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `TICKET_RETRIEVE_SUCCESS` | 200 | 예매자 목록 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/ticket` response boundary with value unchanged. |
| `TICKET_UPDATE_SUCCESS` | 200 | 예매자 입금여부 수정이 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/ticket` response boundary with value unchanged. |
| `TICKET_REFUND_SUCCESS` | 200 | 예매 환불처리 요청이 성공했습니다. | success response | apis controller response | Moved to `apis/ticket` response boundary with value unchanged. |
| `TICKET_DELETE_SUCCESS` | 200 | 예매자 삭제 요청이 성공했습니다. | success response | apis controller response | Moved to `apis/ticket` response boundary with value unchanged. |
| `TICKET_SEARCH_SUCCESS` | 200 | 예매자 검색 결과 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/ticket` response boundary with value unchanged. |

### `MemberSuccessCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/member/api/response/MemberSuccessCode.kt`
- Current package: `com.beat.apis.member.api.response`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `SIGN_UP_SUCCESS` | 200 | 로그인 성공 | success response | apis controller response | Moved to `apis/member` response boundary with value unchanged. |
| `ISSUE_ACCESS_TOKEN_SUCCESS` | 200 | 엑세스토큰 발급 성공 | success response | currently reserved/legacy response | Moved to `apis/member` response boundary with value unchanged. |
| `ISSUE_ACCESS_TOKEN_USING_REFRESH_TOKEN` | 200 | 리프레쉬 토큰으로 액세스 토큰 재발급 성공 | success response | apis controller response | Moved to `apis/member` response boundary with value unchanged. |
| `SIGN_OUT_SUCCESS` | 200 | 로그아웃 성공 | success response | apis controller response | Moved to `apis/member` response boundary with value unchanged. |
| `USER_DELETE_SUCCESS` | 200 | 회원 탈퇴 성공 | success response | currently reserved/legacy response | Moved to `apis/member` response boundary with value unchanged. |

### `PerformanceSuccessCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/performance/api/response/PerformanceSuccessCode.kt`
- Current package: `com.beat.apis.performance.api.response`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `PERFORMANCE_UPDATE_SUCCESS` | 200 | 공연이 성공적으로 수정되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 공연 상세 정보 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_MODIFY_PAGE_SUCCESS` | 200 | 공연 수정 페이지 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_DELETE_SUCCESS` | 200 | 공연이 성공적으로 삭제되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |
| `BOOKING_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 예매 관련 공연 정보 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |
| `HOME_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 홈 화면 공연 목록 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |
| `MAKER_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 회원이 등록한 공연 목록의 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |
| `PERFORMANCE_CREATE_SUCCESS` | 201 | 공연이 성공적으로 생성되었습니다. | success response | apis controller response | Moved to `apis/performance` response boundary with value unchanged. |

### `ScheduleSuccessCode`

- Current file: `apis/src/main/kotlin/com/beat/apis/schedule/api/response/ScheduleSuccessCode.kt`
- Current package: `com.beat.apis.schedule.api.response`

| Code | Status | Message | Classification | Current usage | Migration note |
| --- | ---: | --- | --- | --- | --- |
| `TICKET_AVAILABILITY_RETRIEVAL_SUCCESS` | 200 | 티켓 수량 조회가 성공적으로 완료되었습니다. | success response | apis controller response | Moved to `apis/schedule` response boundary with value unchanged. |

## Cross-cutting hazards to preserve

- Do not move source or rewrite imports in the inventory commit; later commits should use this file as the source list for split candidates.
- Generic domain codes were replaced with invariant-specific codes. The API/admin v1 handlers keep the previous status and message where the public contract differed.
- `BookingApplicationErrorCode.NO_PERFORMANCE_FOUND` and `BookingApplicationErrorCode.NO_SCHEDULE_FOUND` are preserved for booking-context compatibility, while active performance/schedule lookups use their context-local application codes.
- `TokenApplicationErrorCode` is owned by the `apis` authentication use case; `module-contracts` no longer depends on `global-support` for an executable-only error enum.
- Domain model Kotlin files now throw HTTP/framework-neutral `DomainException` with domain ErrorCodes. API/admin handlers map `INVALID_INPUT` to 400 and `STATE_CONFLICT` to 409; the domain module no longer depends on `global-support`.
