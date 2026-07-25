# Domain/Application ErrorCode Review Checklist

Scope: Issue #421 migration 당시 review aid입니다. 현재 규칙은 [error handling guide](../architecture/error-handling.md)와 [`../../domain/README.md`](../../domain/README.md)를 우선하며, 아래 candidate 표의 `current`는 migration snapshot 시점을 뜻합니다.


## Target split rule

Use this rule when reviewing any future ErrorCode / SuccessCode change:

- Keep an error code in `domain` only when it is thrown by a pure domain model/service to enforce an invariant, state transition, or aggregate lifecycle rule that does not require request/auth/actor/external context.
- Move or introduce an application-owned error code when the failure is created by use-case orchestration, request/query validation, authentication/session handling, actor/owner/permission checks, persistence lookup wording, external adapter translation, or response-flow policy.
- Keep authentication/session failures out of `domain`; `TokenApplicationErrorCode` is owned by the `apis` authentication use case while gateway/infra expose implementation-neutral contract results.
- Do not use HTTP, Spring MVC, persistence, QueryDSL/JPA, or API DTO concerns as a reason for a code to remain in `domain`.

## Inventory review gates

When reviewing future ErrorCode changes, verify each affected row has:

- [ ] Current enum path and package.
- [ ] Constant name, status, and message copied from source.
- [ ] Current main-code throw sites grouped by layer (`domain`, executable application, `infra`, `gateway`, `module-contracts`).
- [ ] Classification: `domain invariant`, `hybrid domain + application`, `application use-case`, `infra adapter use-case`, `success response`, `contract/gateway-owned`, or `unused/defer`.
- [ ] Rationale tied to the target split rule above.
- [ ] Proposed follow-up owner/path, if classification differs from the current path.
- [ ] Explicit note for ambiguous constants that are used from both domain and application layers.

## High-risk review findings to check

아래 표는 #421 결정 당시 검토 후보 기록입니다. 현재 enum/package/throw site inventory로 사용하지 않습니다.

| Current enum | Constants / pattern | Current observed use | Review concern | Suggested follow-up classification |
| --- | --- | --- | --- | --- |
| `BookingErrorCode` | purchase count, refund account, payment/refund transition codes | thrown by `domain.booking.model.Booking` and `RefundAccount` | invariant-specific names replace the former generic code; v1 response mapping remains at the HTTP boundary | `domain-owned` |
| `BookingErrorCode` | `REQUIRED_DATA_MISSING`, `INVALID_REQUEST_FORMAT`, lookup `NO_*` codes | executable booking services | request/query and lookup-flow language is application-facing | `application-owned` unless a domain invariant throw site is found |
| `BookingErrorCode` / `TicketApplicationErrorCode` | `NO_PERFORMANCE_FOUND` / `NO_TICKETS_FOUND` | `NO_PERFORMANCE_FOUND` has no main-code throw site; `NO_TICKETS_FOUND` is ticket response language | stale lookup messages can hide in the wrong context | keep ticket lookup language under `apis.ticket`; defer truly unused booking constants |
| `TicketApplicationErrorCode` | all constants | controller/service validation and ticket update flow | moved to `apis.ticket.exception`; not a pure ticket domain model throw | `application-owned` unless follow-up introduces domain ticket model invariant use |
| `MemberApplicationErrorCode` / `SocialLoginFailure` | `SOCIAL_TYPE_BAD_REQUEST`, auth failure translation | infra adapter now throws port-level `SocialLoginFailure`; apis translates to member application code | external-provider/auth adapter failure must stay out of domain | `application-owned translation; infra uses port-level failure` |
| `MemberApplicationErrorCode`, `UserApplicationErrorCode` | `*_NOT_FOUND` | executable services/facades | lookup failures are use-case/persistence orchestration, not domain invariants | `application-owned` |
| `PerformanceErrorCode` | running time, period, ticket price/quantity, schedule count, payment account codes | `Performance` and performance value objects | each code names one pure domain invariant | `domain-owned` |
| `PerformanceErrorCode` | `NOT_PERFORMANCE_OWNER` | both domain `Performance` and executable services | selected #421 direction treats actor/owner validation as application responsibility; domain call-site must be removed before moving | `application-owned after call-site refactor` |
| `PerformanceErrorCode` | `SCHEDULE_LIST_NOT_FOUND`, `PRICE_UPDATE_NOT_ALLOWED`, schedule creation/modification limits, deletion failed, not-found, request-format codes | domain/application/executable services depending on constant | selected #421 direction treats API-visible empty schedule list and use-case flow failures as application language; move/check guards before package movement | `application-owned after call-site refactor where needed` |
| `PerformanceErrorCode` | `INTERNAL_SERVER_ERROR` | no main-code throw site found in this worktree | generic 500 is not a domain invariant and may be dead code | `unused/defer` |
| `PerformanceImageApplicationErrorCode`, `CastApplicationErrorCode`, `StaffApplicationErrorCode` | belong-to-performance / not-found constants | executable performance modification service | relationship checks happen in application orchestration; not-found is lookup flow | `application-owned` unless future pure domain relationship model throws them |
| `PromotionErrorCode` | `PROMOTION_NOT_FOUND` | admin application service | lookup flow | `application-owned` |
| `ScheduleErrorCode` | booking window, ticket counts, allocation, sequence and mixed-performance codes | `Schedule` and `ScheduleSequenceDomainService` | aggregate and cross-entity invariants use explicit codes; request-shape validation has a separate application code | `domain-owned` |
| `ScheduleErrorCode` | `SCHEDULE_NOT_BELONG_TO_PERFORMANCE`, `NO_SCHEDULE_FOUND` | executable services | relationship/lookup checks are currently application orchestration | `application-owned` unless future pure domain relation check owns it |
| `TokenApplicationErrorCode` | all constants | executable auth use case | auth/session failure, not domain model or shared port contract | `apis.member.exception` ownership |

## Proposed migration safety checks

For commit 1 docs-only baseline:

- [ ] Confirm there are no Java/Kotlin source movement changes.
- [ ] Confirm no imports changed in source files.
- [ ] Confirm docs call out mixed-use constants and the selected #421 call-site decision instead of silently reclassifying them.
- [ ] Confirm no other worker-owned docs are overwritten in this worktree.

For a later implementation commit:

- [ ] Add or update boundary tests before moving error-code packages.
- [ ] Move one ownership slice at a time; avoid a repository-wide import rewrite until the target package names are stable.
- [ ] Preserve `BaseErrorCode` status/message behavior exactly unless a product/API change is explicitly approved.
- [ ] For mixed-use constants, first move the application/actor/lookup check to the application boundary; create separate domain/application constants only when both remaining throw sites are still semantically valid.
- [ ] Run compile/test gates after each slice: `./gradlew --no-daemon :domain:test :apis:test :admin:test :batch:test` or narrower impacted modules plus root boundary tests.

## Local evidence captured for this checklist

Commands used to prepare this baseline:

```text
python3 - list *ErrorCode.kt files
rg -n "enum .*ErrorCode|class .*ErrorCode|interface .*ErrorCode|ErrorCode|ErrorType|ErrorStatus" ...
python3 - parse enum constants/status/message
python3 - classify main-code throw-site layer counts
rg -n exact mixed domain/application usages for Booking/Performance/Schedule constants
./gradlew tasks --all --console=plain
```
