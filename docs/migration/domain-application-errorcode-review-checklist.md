# Domain/Application ErrorCode Review Checklist

Scope: Issue #421 final review aid. Use this checklist to prevent domain/application ErrorCode and SuccessCode ownership regressions after the split.


## Target split rule

Use this rule when reviewing any future ErrorCode / SuccessCode change:

- Keep an error code in `domain` only when it is thrown by a pure domain model/service to enforce an invariant, state transition, or aggregate lifecycle rule that does not require request/auth/actor/external context.
- Move or introduce an application-owned error code when the failure is created by use-case orchestration, request/query validation, authentication/session handling, actor/owner/permission checks, persistence lookup wording, external adapter translation, or response-flow policy.
- Keep contract/gateway authentication codes out of `domain` unless they become true domain invariants; today `TokenErrorCode` is in `module-contracts` and is consumed by gateway/application/infra auth flows.
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

These are candidates that need special attention in the inventory because their current package may not match their throw-site semantics.

| Current enum | Constants / pattern | Current observed use | Review concern | Suggested follow-up classification |
| --- | --- | --- | --- | --- |
| `BookingErrorCode` | `INVALID_DATA_FORMAT` | thrown by `domain.booking.domain.Booking` | true booking invariant; should not be merged with request/query format codes of the same enum without review | `domain-owned` |
| `BookingErrorCode` | `REQUIRED_DATA_MISSING`, `INVALID_REQUEST_FORMAT`, lookup `NO_*` codes | executable booking services | request/query and lookup-flow language is application-facing | `application-owned` unless a domain invariant throw site is found |
| `BookingErrorCode` / `TicketApplicationErrorCode` | `NO_PERFORMANCE_FOUND` / `NO_TICKETS_FOUND` | `NO_PERFORMANCE_FOUND` has no main-code throw site; `NO_TICKETS_FOUND` is ticket response language | stale lookup messages can hide in the wrong context | keep ticket lookup language under `apis.ticket`; defer truly unused booking constants |
| `TicketApplicationErrorCode` | all constants | controller/service validation and ticket update flow | moved to `apis.ticket.application.exception`; not a pure ticket domain model throw | `application-owned` unless follow-up introduces domain ticket model invariant use |
| `MemberApplicationErrorCode` / `SocialLoginFailure` | `SOCIAL_TYPE_BAD_REQUEST`, auth failure translation | infra adapter now throws port-level `SocialLoginFailure`; apis translates to member application code | external-provider/auth adapter failure must stay out of domain | `application-owned translation; infra uses port-level failure` |
| `MemberApplicationErrorCode`, `UserApplicationErrorCode` | `*_NOT_FOUND` | executable services/facades | lookup failures are use-case/persistence orchestration, not domain invariants | `application-owned` |
| `PerformanceErrorCode` | `NEGATIVE_TICKET_PRICE`, `INVALID_DATA_FORMAT` | `domain.performance.domain.Performance` | pure domain validation currently depends on these constants | `domain-owned` |
| `PerformanceErrorCode` | `NOT_PERFORMANCE_OWNER` | both domain `Performance` and executable services | selected #421 direction treats actor/owner validation as application responsibility; domain call-site must be removed before moving | `application-owned after call-site refactor` |
| `PerformanceErrorCode` | `SCHEDULE_LIST_NOT_FOUND`, `PRICE_UPDATE_NOT_ALLOWED`, schedule creation/modification limits, deletion failed, not-found, request-format codes | domain/application/executable services depending on constant | selected #421 direction treats API-visible empty schedule list and use-case flow failures as application language; move/check guards before package movement | `application-owned after call-site refactor where needed` |
| `PerformanceErrorCode` | `INTERNAL_SERVER_ERROR` | no main-code throw site found in this worktree | generic 500 is not a domain invariant and may be dead code | `unused/defer` |
| `PerformanceImageApplicationErrorCode`, `CastApplicationErrorCode`, `StaffApplicationErrorCode` | belong-to-performance / not-found constants | executable performance modification service | relationship checks happen in application orchestration; not-found is lookup flow | `application-owned` unless future pure domain relationship model throws them |
| `PromotionErrorCode` | `PROMOTION_NOT_FOUND` | admin application service | lookup flow | `application-owned` |
| `ScheduleErrorCode` | `INSUFFICIENT_TICKETS`, `EXCESS_TICKET_DELETE`, `INVALID_DATA_FORMAT` | pure `domain.schedule.domain.Schedule`; some also used by application services | core schedule inventory/count validation is domain-owned, but application reuse may need separate app-level validation code | `domain-owned` for domain throws; split app reuse if message/status semantics differ |
| `ScheduleErrorCode` | `SCHEDULE_NOT_BELONG_TO_PERFORMANCE`, `NO_SCHEDULE_FOUND` | executable services | relationship/lookup checks are currently application orchestration | `application-owned` unless future pure domain relation check owns it |
| `TokenErrorCode` | all constants | `module-contracts`, gateway, executable auth, infra Kakao adapter | auth/session/provider contract, not domain model | keep outside `domain`; consider contract/application-auth ownership separately |

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
