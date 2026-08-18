# BEAT-SERVER Architecture Migration Artifact

## Safety and baseline

- [x] Target architecture document read completely
- [x] Existing dirty worktree detected and preserved
- [x] Base branch and baseline commit recorded
- [x] Existing uncommitted migration changes classified
- [x] Baseline build/test result recorded

## Required initial audit

- [x] Full Gradle project/dependency graph audited
- [x] CI/CD and runtime assumptions audited
- [x] Application Services inventoried and ownership-classified
- [x] `module-contracts` interfaces/read models inventoried and classified
- [x] Major Command correctness paths traced to consistency sources
- [x] Architecture tests and root legacy tests inventoried
- [x] Java/Kotlin interoperability debt inventoried
- [x] Java production and test sources inventoried
- [x] Reference Capability selected with evidence

## PR strategy

- [x] PR dependency graph recorded
- [x] Every PR has boundary rationale, risks, tests, dependencies, rollback, and DoD
- [x] Existing uncommitted changes mapped to planned PR boundaries

## Migration implementation

- [x] Correctness/characterization prerequisites complete
- [x] Target Gradle boundaries and architecture guards complete
- [ ] Reference Capability migrated and verified
- [ ] Remaining frontoffice capabilities migrated and verified
- [ ] Admin workflows migrated and verified
- [ ] System/batch workflows migrated and verified
- [ ] Infrastructure consolidated with internal implementations
- [ ] Security and observability boundaries normalized
- [ ] `module-contracts` consumers migrated and module retired
- [ ] Kotlin-owned APIs modernized after caller audit
- [ ] Legacy architecture tests replaced and retired

## Final verification

- [ ] `./gradlew check` passes
- [ ] Applicable unit/application/infrastructure/security/batch/API tests pass
- [ ] All three apps build and remain independently executable
- [ ] Final Gradle dependency graph inspected
- [ ] Application public APIs inspected and minimized
- [ ] No `module-contracts` or legacy module references remain
- [ ] Remaining Java sources and `@Jvm*`/`Optional` usages justified
- [ ] Source-string architecture assertions removed or justified
- [ ] Command-to-read-model correctness dependencies absent or justified
- [ ] Cross-Application-Service calls absent or justified
- [ ] Public infrastructure implementations absent or justified
- [ ] Temporary migration adapters absent or justified
- [ ] Final migration report complete

## Evidence log

- Baseline: `develop` at `eb007147`; worktree already contained target-project aliases, empty Application lane projects, architecture docs, and regression tests.
- Baseline `./gradlew projects check --stacktrace`: project model resolved; `:apps:api:test` executed 165 tests, then Gradle failed closing a missing `apis/build/test-results/test/binary/in-progress-results-*.bin`. No assertion failure was reported; deterministic single-worker rerun remains required.
- Application ownership: Booker booking/member/schedule/home, Maker performance/ticket/file, Admin promotion/user, and System ticket-cleanup/promotion-maintenance use cases identified. HTTP DTO conversion facades remain app adapters. No cross-capability concrete Application Service call was found; repository/read-port orchestration remains to migrate.
- Initial audit and ten-PR dependency graph: `docs/architecture/BEAT-SERVER-MIGRATION-EXECUTION.md`.
- Correctness: the current Performance summary adapter reads primary DB state, but its mixed `@ReadModel` contract is unsafe for commands. Member Booking create also returns member ID where persisted/retrieved Booking uses user ID; PR-1 fixes this under regression coverage.
- PR-1: member Booking create now returns the persisted Booking user ID. `BookingCreationStatusServiceTest` asserts distinct member/user IDs; focused test passed with a single Gradle worker. Sober review: PASS.

## Per-PR completion reports

### PR-1 — Characterize and correct Booking identity/amount semantics

1. Architecture invariant improved: Booking's external result uses the same identity persisted by the command and returned by queries.
2. Behavior preserved: route, JSON field name, status, amount, inventory, and event behavior are unchanged; the incorrect numeric identity is corrected.
3. Tests/evidence: focused `BookingCreationStatusServiceTest` passed; existing Booking retrieval tests preserve stored total and characterize legacy fallback.
4. Legacy path removed: member-ID-as-user-ID response path.
5. Temporary compatibility remaining: Booking still resides in the API module and commands still consume the mixed Performance summary read model until PR-3.
6. Next PRs unblocked: target Gradle skeleton and Booking reference-slice extraction.

### PR-2 — Target Gradle skeleton with runtime compatibility guards

1. Architecture invariant improved: target projects exist; Domain/Application lanes and executable lanes have compile-time isolation guards.
2. Behavior preserved: legacy physical paths, runtime ports/profiles, and deploy artifact names `apis/admin/batch-*.jar` remain compatible.
3. Tests/evidence: `verifyTargetModuleGraph`, `verifyModuleBootJars`, and fresh root `:test --rerun-tasks` passed; all three boot jars were built. Sober review: PASS after two findings were fixed.
4. Legacy path removed: legacy logical Gradle task names; physical aliases intentionally remain.
5. Temporary compatibility remaining: empty Application lanes, physical aliases, `module-contracts`, and `global-support`.
6. Next PRs unblocked: Booking reference slice can now compile in `application:frontoffice` while apps remain executable.
