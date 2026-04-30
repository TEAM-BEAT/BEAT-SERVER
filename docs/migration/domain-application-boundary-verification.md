# Domain/Application Boundary Verification Snapshot

Scope: Issue #421 commit 2/3 verification aid. This document records the status/message values and review checks that must stay stable while response success codes and lookup not-found error codes move out of domain packages.

## Success response snapshot

These `BaseSuccessCode` values are API response messages. Package names may change during commit 2, but status/message values must not.

| Code | Status | Message | Target owner |
| --- | ---: | --- | --- |
| `BookingSuccessCode.MEMBER_BOOKING_RETRIEVE_SUCCESS` | 200 | 회원 예매 조회가 성공적으로 완료되었습니다. | `apis/booking/api/response` |
| `BookingSuccessCode.GUEST_BOOKING_RETRIEVE_SUCCESS` | 200 | 비회원 예매 조회가 성공적으로 완료되었습니다. | `apis/booking/api/response` |
| `BookingSuccessCode.BOOKING_REFUND_SUCCESS` | 200 | 예매자의 환불요청이 성공했습니다. | `apis/booking/api/response` |
| `BookingSuccessCode.BOOKING_CANCEL_SUCCESS` | 200 | 예매자의 취소요청이 성공했습니다. | `apis/booking/api/response` |
| `BookingSuccessCode.MEMBER_BOOKING_SUCCESS` | 201 | 회원 예매가 성공적으로 완료되었습니다 | `apis/booking/api/response` |
| `BookingSuccessCode.GUEST_BOOKING_SUCCESS` | 201 | 비회원 예매가 성공적으로 완료되었습니다 | `apis/booking/api/response` |
| `TicketSuccessCode.TICKET_RETRIEVE_SUCCESS` | 200 | 예매자 목록 조회가 성공적으로 완료되었습니다. | `apis/booking/api/response` |
| `TicketSuccessCode.TICKET_UPDATE_SUCCESS` | 200 | 예매자 입금여부 수정이 성공적으로 완료되었습니다. | `apis/booking/api/response` |
| `TicketSuccessCode.TICKET_REFUND_SUCCESS` | 200 | 예매 환불처리 요청이 성공했습니다. | `apis/booking/api/response` |
| `TicketSuccessCode.TICKET_DELETE_SUCCESS` | 200 | 예매자 삭제 요청이 성공했습니다. | `apis/booking/api/response` |
| `TicketSuccessCode.TICKET_SEARCH_SUCCESS` | 200 | 예매자 검색 결과 조회가 성공적으로 완료되었습니다. | `apis/booking/api/response` |
| `MemberSuccessCode.SIGN_UP_SUCCESS` | 200 | 로그인 성공 | `apis/member/api/response` |
| `MemberSuccessCode.ISSUE_ACCESS_TOKEN_SUCCESS` | 200 | 엑세스토큰 발급 성공 | `apis/member/api/response` |
| `MemberSuccessCode.ISSUE_ACCESS_TOKEN_USING_REFRESH_TOKEN` | 200 | 리프레쉬 토큰으로 액세스 토큰 재발급 성공 | `apis/member/api/response` |
| `MemberSuccessCode.SIGN_OUT_SUCCESS` | 200 | 로그아웃 성공 | `apis/member/api/response` |
| `MemberSuccessCode.USER_DELETE_SUCCESS` | 200 | 회원 탈퇴 성공 | `apis/member/api/response` |
| `PerformanceSuccessCode.PERFORMANCE_UPDATE_SUCCESS` | 200 | 공연이 성공적으로 수정되었습니다. | `apis/performance/api/response` |
| `PerformanceSuccessCode.PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 공연 상세 정보 조회가 성공적으로 완료되었습니다. | `apis/performance/api/response` |
| `PerformanceSuccessCode.PERFORMANCE_MODIFY_PAGE_SUCCESS` | 200 | 공연 수정 페이지 조회가 성공적으로 완료되었습니다. | `apis/performance/api/response` |
| `PerformanceSuccessCode.PERFORMANCE_DELETE_SUCCESS` | 200 | 공연이 성공적으로 삭제되었습니다. | `apis/performance/api/response` |
| `PerformanceSuccessCode.BOOKING_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 예매 관련 공연 정보 조회가 성공적으로 완료되었습니다. | `apis/performance/api/response` |
| `PerformanceSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 홈 화면 공연 목록 조회가 성공적으로 완료되었습니다. | `apis/performance/api/response` |
| `PerformanceSuccessCode.MAKER_PERFORMANCE_RETRIEVE_SUCCESS` | 200 | 회원이 등록한 공연 목록의 조회가 성공적으로 완료되었습니다. | `apis/performance/api/response` |
| `PerformanceSuccessCode.PERFORMANCE_CREATE_SUCCESS` | 201 | 공연이 성공적으로 생성되었습니다. | `apis/performance/api/response` |
| `ScheduleSuccessCode.TICKET_AVAILABILITY_RETRIEVAL_SUCCESS` | 200 | 티켓 수량 조회가 성공적으로 완료되었습니다. | `apis/schedule/api/response` |

## Lookup not-found snapshot

Commit 3 may move lookup `*_NOT_FOUND` and `NO_*_FOUND` constants to application exception packages. The constants below are client-visible message contracts and must keep their values.

| Code | Status | Message | Review note |
| --- | ---: | --- | --- |
| `BookingErrorCode.NO_BOOKING_FOUND` | 404 | 입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요. | Booking lookup flow |
| `BookingErrorCode.NO_TICKETS_FOUND` | 404 | 입력하신 정보와 일치하는 예매자 목록이 없습니다. | Booking/ticket lookup flow |
| `BookingErrorCode.NO_PERFORMANCE_FOUND` | 404 | 공연을 찾을 수 없습니다. | Currently unused; preserve if moved or delete only with explicit cleanup approval |
| `BookingErrorCode.NO_SCHEDULE_FOUND` | 404 | 회차를 찾을 수 없습니다. | Currently unused; do not confuse with `ScheduleErrorCode.NO_SCHEDULE_FOUND` |
| `CastErrorCode.CAST_NOT_FOUND` | 404 | 등장인물이 존재하지 않습니다. | Performance modification lookup flow |
| `MemberErrorCode.MEMBER_NOT_FOUND` | 404 | 회원이 없습니다 | API/admin member lookup flow |
| `PerformanceErrorCode.PERFORMANCE_NOT_FOUND` | 404 | 해당 공연 정보를 찾을 수 없습니다. | API/admin performance lookup flow |
| `PerformanceErrorCode.SCHEDULE_LIST_NOT_FOUND` | 404 | 스케쥴 리스트에 스케쥴이 없습니다. | Hybrid hazard: current domain throw is wrapped in `BadRequestException` |
| `PerformanceImageErrorCode.PERFORMANCE_IMAGE_NOT_FOUND` | 404 | 해당 공연 상세이미지를 찾을 수 없습니다. | Performance image lookup flow |
| `PromotionErrorCode.PROMOTION_NOT_FOUND` | 404 | 해당 홍보 정보를 찾을 수 없습니다. | Admin promotion lookup flow |
| `ScheduleErrorCode.NO_SCHEDULE_FOUND` | 404 | 해당 회차를 찾을 수 없습니다. | API schedule lookup flow |
| `StaffErrorCode.STAFF_NOT_FOUND` | 404 | 스태프가 존재하지 않습니다. | Performance modification lookup flow |
| `UserErrorCode.USER_NOT_FOUND` | 404 | 유저가 없습니다 | API user lookup flow |

## Boundary review checks

- Controller response wiring should remain `SuccessResponse.of/from(code, ...)`; moving packages only is safe when the same enum constants still implement `BaseSuccessCode`.
- `GlobalExceptionHandler` and `AdminGlobalExceptionHandler` should keep exception-type handlers unchanged. Package moves must not change the `BadRequestException`, `NotFoundException`, `ForbiddenException`, or `ConflictException` hierarchy.
- `PerformanceErrorCode.SCHEDULE_LIST_NOT_FOUND` is special: the current domain path throws `BadRequestException` with a code whose own status is `404`. If the code moves to an application package, add an application guard before calling `Performance.updatePerformancePeriod(...)` or replace the domain throw with a domain-neutral invariant while preserving the API response behavior intentionally.
- After commit 2, no `*SuccessCode.java` or `*SuccessCode.kt` file should remain under `domain/src/main`; new response code enums should be Kotlin files under `apis/src/main/kotlin/.../api/response`, and controller imports should point at those packages.
- After commit 3, lookup not-found constants should no longer be imported from domain by executable application lookup flows; domain-owned invariant codes must remain available to domain models.

## Verification commands

Run the snapshot test after each migration slice:

```bash
./gradlew --no-daemon :apis:test --tests com.beat.apis.boundary.DomainApplicationCodeBoundarySnapshotTest
```

Then run impacted module compile/tests:

```bash
./gradlew --no-daemon :domain:compileJava :domain:compileKotlin :apis:compileJava :apis:compileKotlin :admin:compileJava :admin:compileKotlin
./gradlew --no-daemon :apis:test :admin:test
```
