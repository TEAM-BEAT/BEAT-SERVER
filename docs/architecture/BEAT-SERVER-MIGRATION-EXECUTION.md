# BEAT-SERVER Migration Execution Record

Baseline: `develop` / `eb007147`  
Target: `BEAT-SERVER-CQRS-MULTIMODULE-ARCHITECTURE-FINAL.md`

## Initial audit verdict

### Gradle, CI, and runtime

- The current dirty tree aliases legacy directories to target project names: `apis -> :apps:api`, `admin -> :apps:admin`, `batch -> :apps:batch`, `core/domain -> :domain`, `core/infra -> :infrastructure`, `gateway -> :support:security`, and `observability -> :support:observability` (`settings.gradle.kts:24-47`).
- `application:frontoffice/admin/system` exist but have no sources and depend only on `:domain`.
- Current direct graph is `apps:* -> module-contracts/domain/infrastructure/global-support/support:*`; apps do not yet depend on application lanes. Infrastructure still depends on `module-contracts`.
- Gradle 9.4.1, Kotlin 2.3.20, Spring Boot 4.0.7, Java/JVM target 25, and the Temurin 25 runtime image are aligned.
- `:apps:api:bootJar` currently emits `api-*.jar`, while CI/deploy stages `apis-*.jar` (`.github/workflows/ci-pr.yml:47-54`, `deploy-dev.yml:211-218`, `deploy-prod.yml:90-97`). The alias skeleton therefore needs archive-name compatibility before merge.
- Deployment path filters still follow legacy physical directories and do not cover future `apps/**` or `application/**` source changes.

### Application ownership

- Frontoffice Booker: booking command/query/auth/session, public performance detail, schedule availability, home query, member authentication/social login.
- Frontoffice Maker: performance create/modify/delete/edit/list, ticket command/query, file upload.
- Admin: promotion and user workflows.
- System: ticket cleanup and promotion maintenance.
- HTTP facades map request/response DTOs and remain in apps. Twenty-four API application/helper files currently throw `ApiApplicationException`; Admin use cases similarly depend on `AdminApplicationException`.
- No cross-capability concrete Application Service call was found. Cross-capability repository/read-port orchestration remains and must be reviewed per command/query semantics.

### Contracts

The 48 `module-contracts` source files contain auth/session/security ports, query readers/read models, notification/storage ports, and supporting types. Final disposition:

| Disposition | Contracts |
|---|---|
| MOVE-TO-APPLICATION-PORT | guest throttle/session/password hash, refresh token, social login, image cache, booking/member notification, SMS |
| MOVE-TO-DOMAIN-REPOSITORY | guest credential lookup/result |
| MOVE-TO-QUERY-READER | maker ticket, maker performance list, performance edit form, home promotion, schedule availability, schedule summaries |
| REPLACE-WITH-CAPABILITY-CONTRACT | JWT mixed contract, performance content ownership, broad file storage |
| REQUIRES-CORRECTNESS-FIX | mixed `PerformanceSummaryReadPort`; storage DTO leakage |
| INLINE-OR-DELETE | `ReadModel` marker after consumer-owned query models move |

`PerformanceSummaryReadPort` mixes valid consumer projections with command inputs for price/payment terms and ownership. Its only adapter currently reads the primary performance tables through JDSL, with no replica or application cache, so no stale-read production defect is proven. The contract is still unsafe because later query optimization could silently weaken command correctness.

### Command correctness

- Booking creation locks Schedule, rechecks the DB close time, reserves inventory in the domain, and saves Booking/Schedule in one Application transaction.
- Cancellation uses schedule-then-booking lock ordering consistent with ticket commands; refund requests intentionally retain allocation.
- Proven defect: member booking persists `member.getUserId()` but returns `member.getId()` as `BookingCreationResult.userId` (`MemberBookingCommandService.kt:53,71`). Existing regression coverage does not distinguish those IDs.
- Risks requiring explicit decisions/tests: legacy nullable total fallback can use a changed current price; rehydration does not validate purchase-count range; duplicate guest credentials can be resolved nondeterministically.

### Tests and Kotlin/Java debt

- Production: 444 Kotlin and 4 Java files. The Java production files are the two Batch services and two Batch jobs.
- Tests: 61 Kotlin and 46 Java files. Thirty-five Java tests consume Kotlin APIs; 33 depend on Java-style getters.
- Root source-scanning suites contain 73 methods / 3,199 LOC across `SharedBoundaryContractTest`, `RootRetirementContractTest`, and `PromotionBoundaryTest`.
- Production debt baseline: 79 `@JvmStatic` occurrences in 34 files, 12 `@JvmOverloads` occurrences in 5 files, no `@JvmField`, 16 `java.util.Optional` imports, and 46 domain Java-style getter declarations.
- These bridges stay until Java callers and protected invariants have replacement Kotlin/ArchUnit/compiler coverage.

### Reference capability

Booking remains the reference slice after the correctness prerequisite. It has the strongest existing coverage and exercises transaction ownership, Schedule locking/inventory, Performance terms, guest/member flows, post-commit events, and consumer query projections. Schedule is treated as Booking's enabling authoritative seam, not as an independent first migration.

## PR dependency graph

```text
PR-1 ─┐
      ├→ PR-3 → PR-4 ─┐
PR-2 ─┘          │    ├→ PR-7 → PR-8 → PR-9 → PR-10
                 ├→ PR-5 ┘
                 └→ PR-6 ┘
```

PR-4 and PR-5 may proceed in parallel after PR-3. PR-6 waits for the relevant frontoffice Booking/Promotion contracts from PR-4.

## Planned PRs

### PR-1 — Characterize and correct Booking identity/amount semantics

- Objective: add regression coverage for distinct member/user IDs, fix the member-create response, and record snapshot/legacy amount behavior without changing routes or JSON shape.
- Boundary: correctness precedes structural movement; the defect and characterization are independently reviewable.
- Expected files: focused Booking tests and the smallest Booking command correction; no module moves.
- Invariant gained: create response identity matches persisted/retrieved Booking identity.
- Risk: clients may have accidentally consumed the wrong numeric ID; field name and schema remain unchanged.
- Tests: Booking command/status/query tests and focused API contract tests.
- Dependencies: none.
- Temporary compatibility: current package/module layout remains.
- Rollback: revert the focused service/test diff.
- DoD: differing member/user IDs are asserted; focused tests pass; behavior decision is documented.

### PR-2 — Target Gradle skeleton with runtime compatibility guards

- Objective: introduce target logical projects and isolation guards while preserving artifact names, CI staging, deploy triggers, and three bootable runtimes.
- Boundary: graph mechanics are separable from use-case movement and easy to roll back.
- Expected files: settings/root/module build files, application skeletons, CI/deploy path filters, root graph tests.
- Invariant gained: target lanes exist and are compile-time isolated; runtime artifacts remain deploy-compatible.
- Risk: Gradle task/archive renames and hidden stale artifacts.
- Tests: `projects`, `verifyTargetModuleGraph`, `verifyModuleBootJars`, clean artifact-name assertions, root tests.
- Dependencies: none.
- Temporary compatibility: target project names map to legacy physical directories; `module-contracts` and `global-support` remain.
- Rollback: restore legacy includes/dependency paths and CI filters.
- DoD: clean build emits expected deploy artifacts and all three boot jars pass.

### PR-3 — Booking authoritative seam and reference slice

- Objective: move Booker Booking command/query use cases to `application:frontoffice`, separate consumer query readers, and replace command use of `PerformanceSummaryReadModel` with authoritative Performance-owned state/capability.
- Boundary: one coherent capability proves the architecture before broad migration.
- Expected files: Booking/Schedule application code and tests, app adapters, Performance authoritative collaboration, infrastructure adapters, relevant contracts.
- Invariant gained: apps contain Booking HTTP adaptation only; Booking commands use authoritative inputs and own transactions.
- Risk: inventory, close-time, payment amount, cancellation/refund, guest auth, event timing.
- Tests: all Booking unit/integration/concurrency tests, Schedule availability/locking tests, API JSON/status tests, app context.
- Dependencies: PR-1 and PR-2.
- Temporary compatibility: adapter delegation may bridge legacy package names for one PR; no duplicate business workflow.
- Rollback: point app adapters back to the legacy Booking entry points while retaining compatible contracts.
- DoD: complete member/guest create/cancel/query flow crosses apps -> application -> domain <- infrastructure; no Booking command consumes a read model.

### PR-4 — Remaining frontoffice capabilities

- Objective: migrate Performance/Schedule, then Ticket/Member/Home/File use cases and consumer-owned readers/ports.
- Boundary: these capabilities share frontoffice runtime and contract fan-in; capability-sized commits remain reversible inside the PR.
- Expected files: `application:frontoffice`, `apps:api`, domain/infrastructure adapters, relevant `module-contracts` types/tests.
- Invariant gained: API runtime is adapter/bootstrap only; no cross-capability concrete Application Service graph.
- Risk: ownership authorization, ticket lifecycle, social auth, storage DTO mapping, read projection compatibility.
- Tests: capability unit/application/infrastructure tests, API contract/security/context tests.
- Dependencies: PR-3.
- Temporary compatibility: unmigrated Admin/System contract consumers remain in `module-contracts`.
- Rollback: capability adapter-by-adapter routing to the last green implementation.
- DoD: frontoffice use cases live only in `application:frontoffice`; application has no Web/JPA/Redis/Feign/S3/JDSL implementation types.

### PR-5 — Admin application lane

- Objective: migrate Admin promotion/user workflows and replace mixed Performance/storage contracts with Admin-owned inputs/outputs.
- Boundary: Admin delivery and policy have an independent actor/change reason and can be reviewed separately from frontoffice.
- Expected files: `application:admin`, `apps:admin`, infrastructure implementations, admin tests/contracts.
- Invariant gained: Admin app owns HTTP only; Admin workflows do not reuse frontoffice services.
- Risk: promotion eligibility/order, performance ownership lookup, upload/cache behavior, error mapping.
- Tests: Admin application/architecture/API/context tests and persistence adapter tests.
- Dependencies: PR-3; may parallel PR-4 where files do not overlap.
- Temporary compatibility: shared storage implementation may implement old and new ports until PR-7.
- Rollback: restore current Admin facade-to-service wiring.
- DoD: Admin use cases and failure language reside in `application:admin`; HTTP exceptions are mapped in the app.

### PR-6 — System/Batch application lane and Java production retirement

- Objective: move cleanup/maintenance workflows into `application:system`, leave jobs/schedulers in `apps:batch`, and convert the four Java production files after caller coverage exists.
- Boundary: scheduler ownership and Kotlin interoperability debt converge in one runtime lane.
- Expected files: `application:system`, batch jobs/config, domain repositories, Batch tests.
- Invariant gained: Batch triggers use cases only; no Java production remains without justification.
- Risk: cron/transaction/job behavior and promotion/schedule coordination.
- Tests: Batch context/scheduler/job/application tests; contention probe remains opt-in and isolated.
- Dependencies: PR-4 and PR-2.
- Temporary compatibility: existing cron/property keys and physical batch path remain.
- Rollback: restore legacy job delegation without schema change.
- DoD: batch is independently executable and all business maintenance workflow is in `application:system`.

### PR-7 — Retire `module-contracts` and hide infrastructure

- Objective: finish all contract dispositions, remove the central module, and make adapter implementations `internal` behind minimal bootstrap configuration.
- Boundary: deletion waits until every consumer is migrated, minimizing broken intermediate states.
- Expected files: all application ports/readers, infrastructure implementations/config, settings/build files, architecture guards.
- Invariant gained: no central contract module; consumer-owned vocabulary and hidden driven adapters.
- Risk: Spring component discovery/visibility and accidental public API dependencies.
- Tests: full application/infra/app suites, dependency reports, ArchUnit/visibility/context tests.
- Dependencies: PR-4, PR-5, and PR-6.
- Temporary compatibility: none after merge; dual implementations are removed in this PR.
- Rollback: revert as a unit to restore the last compatible contract module.
- DoD: zero `module-contracts` references; remaining ports each have recorded volatility/ownership rationale.

### PR-8 — Normalize support boundaries and retire `global-support`

- Objective: keep authentication plumbing in `support:security`, observability in `support:observability`, and move/delete each `global-support` concern by ownership.
- Boundary: cross-cutting cleanup follows stable application/infra APIs to avoid moving legacy coupling into support.
- Expected files: support projects, app security config, observability config/tests, global-support consumers/build graph.
- Invariant gained: narrow support modules with no business rules; no unearned shared module.
- Risk: authentication filters/principals, exception/logging behavior, runtime configuration.
- Tests: security, JWT/session, observability, app context and boot tests.
- Dependencies: PR-7.
- Temporary compatibility: runtime property names/log formats remain unchanged.
- Rollback: restore prior support project dependencies/configuration.
- DoD: `global-support` is retired or every remaining exception is explicitly justified against the target.

### PR-9 — Kotlin-first APIs and semantic architecture guards

- Objective: remove caller-proven Java bridges/Optional usage, replace root source scanning with compiler/Gradle/ArchUnit/semantic tests, and minimize application public APIs.
- Boundary: modernization occurs only after Java production and legacy contract consumers are gone.
- Expected files: domain/application Kotlin APIs, migrated tests, architecture tests, visibility modifiers.
- Invariant gained: idiomatic Kotlin-owned APIs and stronger executable architecture enforcement.
- Risk: Java test callers and reflective/JPA/Spring construction.
- Tests: migrated domain/application/infra tests, ArchUnit, reflection/JPA context tests, full `check`.
- Dependencies: PR-8 and PR-6.
- Temporary compatibility: only bridges with a named external Java ABI consumer may remain.
- Rollback: restore individual bridge while retaining replacement tests.
- DoD: remaining Java, `@Jvm*`, Optional, getters, nullable IDs, and source-string assertions each have concrete justification.

### PR-10 — Physical tree alignment and final verification

- Objective: relocate remaining legacy physical directories to the target tree, remove aliases/temporary adapters, update CI/deploy paths, and produce final evidence.
- Boundary: mechanical relocation follows semantic migration, avoiding a big-bang rewrite while making the final tree truthful.
- Expected files: physical module paths, settings, CI/deploy/Docker references, final architecture/test reports.
- Invariant gained: target logical and physical architecture match; three apps remain independently executable.
- Risk: path-sensitive CI/deploy scripts and missed legacy references.
- Tests: clean `./gradlew check`, all boot jars, dependency/build-health reports, API/security/batch/integration tests, exhaustive final searches.
- Dependencies: PR-9.
- Temporary compatibility: none.
- Rollback: revert the mechanical relocation commit(s) to the last green aliased graph.
- DoD: every completion criterion and `task_artifact.md` item is checked with command evidence.

## Existing dirty-work mapping

- PR-1 candidates: new focused API characterization tests only after individual verification.
- PR-2 candidates: current settings/build/root-test/application-skeleton changes; archive-name/CI compatibility must be fixed before acceptance.
- Later PR candidates: architecture/observability documents and SQL proposals only when their owning implementation is approved and tested.
- Excluded from migration changes: `dump.rdb`, vendored wheel, and unrelated operational artifacts. They are preserved as pre-existing user-owned files and must not be committed by this migration.
